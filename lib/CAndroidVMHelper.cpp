/*
 * CAndroidVMHelper.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "CAndroidVMHelper.h"
#include "texts/TextOperations.h"

#include <bit>

#ifdef VCMI_ANDROID
static JavaVM * vmCache = nullptr;

/// cached java classloader so that we can find our classes from other threads
static jobject vcmiClassLoader;
static jmethodID vcmiFindClassMethod;

bool CAndroidVMHelper::alwaysUseLoadedClass = false;

void CAndroidVMHelper::cacheVM(JNIEnv * env)
{
	env->GetJavaVM(&vmCache);
}

CAndroidVMHelper::CAndroidVMHelper()
{
	auto res = vmCache->GetEnv((void **) &envPtr, JNI_VERSION_1_1);
	if(res == JNI_EDETACHED)
	{
		auto attachRes = vmCache->AttachCurrentThread(&envPtr, nullptr);
		if(attachRes == JNI_OK)
		{
			detachInDestructor = true; // only detach if we actually attached env
		}
	}
	else
	{
		detachInDestructor = false;
	}
}
CAndroidVMHelper::~CAndroidVMHelper()
{
	if(envPtr && detachInDestructor)
	{
		vmCache->DetachCurrentThread();
		envPtr = nullptr;
	}
}

JNIEnv * CAndroidVMHelper::get()
{
	return envPtr;
}

jclass CAndroidVMHelper::findClassloadedClass(const std::string & name)
{
	auto env = get();
	return static_cast<jclass>(env->CallObjectMethod(vcmiClassLoader, vcmiFindClassMethod,
		env->NewStringUTF(name.c_str())));
}

void CAndroidVMHelper::callStaticVoidMethod(const std::string & cls, const std::string & method,
											bool classloaded)
{
	auto env = get();
	auto javaHelper = findClass(cls, classloaded);
	auto methodId = env->GetStaticMethodID(javaHelper, method.c_str(), "()V");
	env->CallStaticVoidMethod(javaHelper, methodId);
}

std::string CAndroidVMHelper::callStaticStringMethod(const std::string & cls, const std::string & method,
													 bool classloaded)
{
	auto env = get();
	auto javaHelper = findClass(cls, classloaded);
	auto methodId = env->GetStaticMethodID(javaHelper, method.c_str(), "()Ljava/lang/String;");
	jstring jres = static_cast<jstring>(env->CallStaticObjectMethod(javaHelper, methodId));
	return std::string(env->GetStringUTFChars(jres, nullptr));
}

void CAndroidVMHelper::callCustomMethod(const std::string & cls, const std::string & method,
										const std::string & signature,
										std::function<void(JNIEnv *, jclass, jmethodID)> fun, bool classloaded)
{
	auto env = get();
	auto javaHelper = findClass(cls, classloaded);
	auto methodId = env->GetStaticMethodID(javaHelper, method.c_str(), signature.c_str());
	fun(env, javaHelper, methodId);
}

void CAndroidVMHelper::publishThorContext(std::uint64_t revision, const std::string & contextId,
	const std::string & title, const std::string & status,
	const ThorContextDetails & details, std::uint64_t heroPortraitAssetKey)
{
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorContext",
		"(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
		"Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V",
		[revision, &contextId, &title, &status, &details, heroPortraitAssetKey](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			jstring javaContextId = env->NewStringUTF(contextId.c_str());
			jstring javaTitle = env->NewStringUTF(title.c_str());
			jstring javaStatus = env->NewStringUTF(status.c_str());
			std::array<jstring, THOR_CONTEXT_DETAIL_LINE_COUNT> javaDetails;
			for(std::size_t index = 0; index < details.size(); ++index)
				javaDetails[index] = env->NewStringUTF(details[index].c_str());
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), javaContextId, javaTitle, javaStatus,
				javaDetails[0], javaDetails[1], javaDetails[2], javaDetails[3], static_cast<jlong>(heroPortraitAssetKey));
			env->DeleteLocalRef(javaContextId);
			env->DeleteLocalRef(javaTitle);
			env->DeleteLocalRef(javaStatus);
			for(const auto javaDetail : javaDetails)
				env->DeleteLocalRef(javaDetail);
		}, true);
}

void CAndroidVMHelper::publishThorActionState(std::uint64_t revision, std::uint64_t enabledActionMask,
	std::uint64_t activeActionMask)
{
	static_assert(sizeof(jlong) == sizeof(std::uint64_t));
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorActionState", "(JJJ)V",
		[revision, enabledActionMask, activeActionMask](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), std::bit_cast<jlong>(enabledActionMask),
				std::bit_cast<jlong>(activeActionMask));
		}, true);
}

void CAndroidVMHelper::publishThorBrowser(std::uint64_t revision, int page, int pageCount,
	const std::vector<ThorBrowserEntry> & entries)
{
	if(entries.size() > THOR_MAX_TOWN_HALL_BUILDINGS)
		return;
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorBrowser", "(JII[I[Ljava/lang/String;[I)V",
		[revision, page, pageCount, &entries](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			const auto size = static_cast<jsize>(entries.size());
			jintArray targets = env->NewIntArray(size);
			jintArray flags = env->NewIntArray(size);
			jclass stringClass = env->FindClass("java/lang/String");
			jobjectArray labels = env->NewObjectArray(size, stringClass, nullptr);
			for(jsize index = 0; index < size; ++index)
			{
				const auto & entry = entries[index];
				const jint target = entry.target;
				const jint state = (entry.enabled ? 1 : 0) | (entry.selected ? 2 : 0)
					| (entry.completed ? 4 : 0);
				env->SetIntArrayRegion(targets, index, 1, &target);
				env->SetIntArrayRegion(flags, index, 1, &state);
				jstring label = env->NewStringUTF(entry.label.c_str());
				env->SetObjectArrayElement(labels, index, label);
				env->DeleteLocalRef(label);
			}
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision),
				static_cast<jint>(page), static_cast<jint>(pageCount), targets, labels, flags);
			env->DeleteLocalRef(targets);
			env->DeleteLocalRef(flags);
			env->DeleteLocalRef(labels);
			env->DeleteLocalRef(stringClass);
		}, true);
}

void CAndroidVMHelper::publishThorRecruitment(std::uint64_t revision,
	const ThorRecruitmentSnapshot & recruitment)
{
	if(recruitment.rows.size() > THOR_MAX_RECRUITMENT_ROWS)
		return;
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorRecruitment",
		"(JII[I[I[I[I[I[I[I[I[J[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
		[revision, &recruitment](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			const auto size = static_cast<jsize>(recruitment.rows.size());
			jintArray targets = env->NewIntArray(size);
			jintArray creatureIds = env->NewIntArray(size);
			jintArray available = env->NewIntArray(size);
			jintArray selected = env->NewIntArray(size);
			jintArray maximum = env->NewIntArray(size);
			jintArray variantIndexes = env->NewIntArray(size);
			jintArray variantCounts = env->NewIntArray(size);
			jintArray flags = env->NewIntArray(size);
			jlongArray visualKeys = env->NewLongArray(size);
			jclass stringClass = env->FindClass("java/lang/String");
			jobjectArray names = env->NewObjectArray(size, stringClass, nullptr);
			jobjectArray unitCosts = env->NewObjectArray(size, stringClass, nullptr);
			jobjectArray selectedCosts = env->NewObjectArray(size, stringClass, nullptr);
			for(jsize index = 0; index < size; ++index)
			{
				const auto & row = recruitment.rows[static_cast<std::size_t>(index)];
				const jint target = row.target, creatureId = row.creatureId;
				const jint availableCount = row.availableCount, selectedAmount = row.selectedAmount;
				const jint maximumAmount = row.maximumAmount, variantIndex = row.variantIndex;
				const jint variantCount = row.variantCount;
				const jint state = (row.enabled ? 1 : 0) | (row.selected ? 2 : 0)
					| (row.armyAvailable ? 4 : 0);
				const jlong visualKey = static_cast<jlong>(row.visualAssetKey);
				env->SetIntArrayRegion(targets, index, 1, &target);
				env->SetIntArrayRegion(creatureIds, index, 1, &creatureId);
				env->SetIntArrayRegion(available, index, 1, &availableCount);
				env->SetIntArrayRegion(selected, index, 1, &selectedAmount);
				env->SetIntArrayRegion(maximum, index, 1, &maximumAmount);
				env->SetIntArrayRegion(variantIndexes, index, 1, &variantIndex);
				env->SetIntArrayRegion(variantCounts, index, 1, &variantCount);
				env->SetIntArrayRegion(flags, index, 1, &state);
				env->SetLongArrayRegion(visualKeys, index, 1, &visualKey);
				jstring name = env->NewStringUTF(row.name.c_str());
				jstring unitCost = env->NewStringUTF(row.unitCost.c_str());
				jstring selectedCost = env->NewStringUTF(row.selectedCost.c_str());
				env->SetObjectArrayElement(names, index, name);
				env->SetObjectArrayElement(unitCosts, index, unitCost);
				env->SetObjectArrayElement(selectedCosts, index, selectedCost);
				env->DeleteLocalRef(name);
				env->DeleteLocalRef(unitCost);
				env->DeleteLocalRef(selectedCost);
			}
			jstring townName = env->NewStringUTF(recruitment.townName.c_str());
			jstring totalCost = env->NewStringUTF(recruitment.totalCost.c_str());
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision),
				static_cast<jint>(recruitment.mode), static_cast<jint>(recruitment.selectedTarget),
				targets, creatureIds, available, selected, maximum, variantIndexes, variantCounts, flags,
				visualKeys, names, unitCosts, selectedCosts, townName, totalCost);
			env->DeleteLocalRef(targets);
			env->DeleteLocalRef(creatureIds);
			env->DeleteLocalRef(available);
			env->DeleteLocalRef(selected);
			env->DeleteLocalRef(maximum);
			env->DeleteLocalRef(variantIndexes);
			env->DeleteLocalRef(variantCounts);
			env->DeleteLocalRef(flags);
			env->DeleteLocalRef(visualKeys);
			env->DeleteLocalRef(names);
			env->DeleteLocalRef(unitCosts);
			env->DeleteLocalRef(selectedCosts);
			env->DeleteLocalRef(townName);
			env->DeleteLocalRef(totalCost);
			env->DeleteLocalRef(stringClass);
		}, true);
}

void CAndroidVMHelper::acknowledgeThorAction(std::uint64_t revision, ThorAction action, std::uint64_t submittedRevision)
{
	if(submittedRevision == 0)
		submittedRevision = revision;
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "acknowledgeThorAction", "(JJI)V",
		[revision, submittedRevision, action](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision),
				static_cast<jlong>(submittedRevision),
				static_cast<jint>(action));
		}, true);
}

void CAndroidVMHelper::publishThorHeroes(std::uint64_t revision, const std::vector<ThorHeroEntry> & heroes)
{
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorHeroes", "(J[I[Ljava/lang/String;[I[I[I)V",
		[revision, &heroes](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			const auto size = static_cast<jsize>(heroes.size());
			jintArray ids = env->NewIntArray(size);
			jintArray movement = env->NewIntArray(size);
			jintArray maximum = env->NewIntArray(size);
			jintArray flags = env->NewIntArray(size);
			jclass stringClass = env->FindClass("java/lang/String");
			jobjectArray names = env->NewObjectArray(size, stringClass, nullptr);
			for(jsize index = 0; index < size; ++index)
			{
				const auto & hero = heroes[index];
				const jint id = hero.id, move = hero.movement, max = hero.maximumMovement;
				const jint state = (hero.selected ? 1 : 0) | (hero.sleeping ? 2 : 0);
				env->SetIntArrayRegion(ids, index, 1, &id);
				env->SetIntArrayRegion(movement, index, 1, &move);
				env->SetIntArrayRegion(maximum, index, 1, &max);
				env->SetIntArrayRegion(flags, index, 1, &state);
				jstring name = env->NewStringUTF(hero.name.c_str());
				env->SetObjectArrayElement(names, index, name);
				env->DeleteLocalRef(name);
			}
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), ids, names, movement, maximum, flags);
			env->DeleteLocalRef(ids);
			env->DeleteLocalRef(names);
			env->DeleteLocalRef(movement);
			env->DeleteLocalRef(maximum);
			env->DeleteLocalRef(flags);
			env->DeleteLocalRef(stringClass);
		}, true);
}

void CAndroidVMHelper::publishThorTowns(std::uint64_t revision, const std::vector<ThorTownEntry> & towns)
{
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorTowns", "(J[I[Ljava/lang/String;[I)V",
		[revision, &towns](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			const auto size = static_cast<jsize>(towns.size());
			jintArray ids = env->NewIntArray(size);
			jintArray flags = env->NewIntArray(size);
			jclass stringClass = env->FindClass("java/lang/String");
			jobjectArray names = env->NewObjectArray(size, stringClass, nullptr);
			for(jsize index = 0; index < size; ++index)
			{
				const auto & town = towns[index];
				const jint id = town.id;
				const jint selected = town.selected ? 1 : 0;
				env->SetIntArrayRegion(ids, index, 1, &id);
				env->SetIntArrayRegion(flags, index, 1, &selected);
				jstring name = env->NewStringUTF(town.name.c_str());
				env->SetObjectArrayElement(names, index, name);
				env->DeleteLocalRef(name);
			}
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), ids, names, flags);
			env->DeleteLocalRef(ids);
			env->DeleteLocalRef(names);
			env->DeleteLocalRef(flags);
			env->DeleteLocalRef(stringClass);
		}, true);
}

void CAndroidVMHelper::publishThorAdventureMap(std::uint64_t revision, const ThorAdventureMap & map,
	ThorMapViewport viewport, bool includeColors)
{
	// An invalid map is an explicit empty publication, never a partial/clipped raster.
	const bool valid = map.valid();
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorAdventureMap", "(JJIIIIIIII[B[I[I[Ljava/lang/String;Z)V",
		[&](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			jbyteArray colors = nullptr;
			jintArray markers = env->NewIntArray(valid ? static_cast<jsize>(map.markers.size() * 6) : 0);
			if(!markers)
				return;
			if(valid && includeColors)
			{
				colors = env->NewByteArray(static_cast<jsize>(map.rgb->size()));
				if(!colors)
				{
					env->DeleteLocalRef(markers);
					return;
				}
				env->SetByteArrayRegion(colors, 0, static_cast<jsize>(map.rgb->size()),
					reinterpret_cast<const jbyte *>(map.rgb->data()));
			}
			if(valid)
			{
				viewport = clampThorMapViewport(viewport, map.width, map.height);
				for(jsize index = 0; index < static_cast<jsize>(map.markers.size()); ++index)
				{
					const auto & marker = map.markers[index];
					const jint data[] = {marker.action, marker.id, marker.tile.x, marker.tile.y,
						marker.tile.level, marker.selected ? 1 : 0};
					env->SetIntArrayRegion(markers, index * 6, 6, data);
				}
			}
			jclass stringClass = env->FindClass("java/lang/String");
			const auto objectCount = valid ? static_cast<jsize>(map.objects.size()) : 0;
			jintArray objects = env->NewIntArray(objectCount * 5);
			jobjectArray labels = stringClass ? env->NewObjectArray(objectCount, stringClass, nullptr) : nullptr;
			if(!objects || !labels)
			{
				if(colors) env->DeleteLocalRef(colors);
				env->DeleteLocalRef(markers);
				if(objects) env->DeleteLocalRef(objects);
				if(labels) env->DeleteLocalRef(labels);
				if(stringClass) env->DeleteLocalRef(stringClass);
				return;
			}
			for(jsize index = 0; index < objectCount; ++index)
			{
				const auto & object = map.objects[index];
				const jint data[] = {object.id, object.tile.x, object.tile.y, object.tile.level, object.category};
				env->SetIntArrayRegion(objects, index * 5, 5, data);
				std::vector<jchar> utf16;
				for(std::size_t offset = 0; offset < object.label.size();)
				{
					const auto point = TextOperations::getUnicodeCodepoint(object.label.data() + offset, object.label.size() - offset);
					if(point <= 0xffff)
						utf16.push_back(static_cast<jchar>(point));
					else
					{
						utf16.push_back(static_cast<jchar>(0xd800 + ((point - 0x10000) >> 10)));
						utf16.push_back(static_cast<jchar>(0xdc00 + ((point - 0x10000) & 0x3ff)));
					}
					offset += TextOperations::getUnicodeCharacterSize(object.label[offset]);
				}
				jstring label = env->NewString(utf16.data(), static_cast<jsize>(utf16.size()));
				env->SetObjectArrayElement(labels, index, label);
				env->DeleteLocalRef(label);
			}
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision),
				static_cast<jlong>(valid ? map.contentRevision : 0), valid ? map.width : 0, valid ? map.height : 0,
				valid ? map.level : 0, valid ? map.levels : 0,
				valid ? viewport.x : 0, valid ? viewport.y : 0,
				valid ? viewport.width : 0, valid ? viewport.height : 0, colors, markers, objects, labels, static_cast<jboolean>(valid && map.objectsLimited));
			if(colors)
				env->DeleteLocalRef(colors);
			env->DeleteLocalRef(markers);
			env->DeleteLocalRef(objects);
			env->DeleteLocalRef(labels);
			env->DeleteLocalRef(stringClass);
		}, true);
}

void CAndroidVMHelper::publishThorHeroMeetingArmies(std::uint64_t revision, const ThorHeroMeetingArmies & armies)
{
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorHeroMeetingArmies",
		"(JII[Ljava/lang/String;[I[I[I[Ljava/lang/String;[II[J[J)V",
		[revision, &armies](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			constexpr auto size = static_cast<jsize>(THOR_HERO_MEETING_ARMY_SIZE);
			jintArray armyIds = env->NewIntArray(2);
			jintArray creatureIds = env->NewIntArray(size * 2);
			jintArray counts = env->NewIntArray(size * 2);
			jintArray flags = env->NewIntArray(size * 2);
			jlongArray visualKeys = env->NewLongArray(size * 2);
			jlongArray heroPortraitKeys = env->NewLongArray(2);
			jclass stringClass = env->FindClass("java/lang/String");
			jobjectArray heroNames = env->NewObjectArray(2, stringClass, nullptr);
			jobjectArray creatureNames = env->NewObjectArray(size * 2, stringClass, nullptr);
			const std::array armyIdValues = {static_cast<jint>(armies.leftArmyId), static_cast<jint>(armies.rightArmyId)};
			env->SetIntArrayRegion(armyIds, 0, 2, armyIdValues.data());
			const std::array heroNameValues = {armies.leftHeroName, armies.rightHeroName};
			const std::array<jlong, 2> heroPortraitKeyValues = {
				static_cast<jlong>(armies.heroPortraitAssetKeys[0]), static_cast<jlong>(armies.heroPortraitAssetKeys[1])};
			std::array<jlong, size * 2> visualKeyValues{};
			for(jsize side = 0; side < 2; ++side)
			{
				jstring name = env->NewStringUTF(heroNameValues[side].c_str());
				env->SetObjectArrayElement(heroNames, side, name);
				env->DeleteLocalRef(name);
				const auto & slots = side == 0 ? armies.leftSlots : armies.rightSlots;
				for(jsize index = 0; index < size; ++index)
				{
					const auto & slot = slots[index];
					const jsize output = side * size + index;
					const jint creatureId = slot.creatureId, count = slot.count, occupied = slot.occupied ? 1 : 0;
					visualKeyValues[static_cast<std::size_t>(output)] = static_cast<jlong>(slot.visualAssetKey);
					env->SetIntArrayRegion(creatureIds, output, 1, &creatureId);
					env->SetIntArrayRegion(counts, output, 1, &count);
					env->SetIntArrayRegion(flags, output, 1, &occupied);
					jstring creatureName = env->NewStringUTF(slot.creatureName.c_str());
					env->SetObjectArrayElement(creatureNames, output, creatureName);
					env->DeleteLocalRef(creatureName);
				}
			}
			env->SetLongArrayRegion(visualKeys, 0, size * 2, visualKeyValues.data());
			env->SetLongArrayRegion(heroPortraitKeys, 0, 2, heroPortraitKeyValues.data());
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), static_cast<jint>(armies.leftHeroId),
				static_cast<jint>(armies.rightHeroId), heroNames, armyIds, creatureIds, counts, creatureNames, flags,
				static_cast<jint>(armies.locallyControllable ? 1 : 0), visualKeys, heroPortraitKeys);
			env->DeleteLocalRef(armyIds);
			env->DeleteLocalRef(creatureIds);
			env->DeleteLocalRef(counts);
			env->DeleteLocalRef(flags);
			env->DeleteLocalRef(heroNames);
			env->DeleteLocalRef(creatureNames);
			env->DeleteLocalRef(visualKeys);
			env->DeleteLocalRef(heroPortraitKeys);
			env->DeleteLocalRef(stringClass);
		}, true);
}

void CAndroidVMHelper::publishThorHeroMeetingArtifacts(std::uint64_t revision, const ThorHeroMeetingArtifacts & artifacts)
{
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorHeroMeetingArtifacts",
		"(JII[Ljava/lang/String;[I[I[Ljava/lang/String;[J)V",
		[revision, &artifacts](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			const auto size = static_cast<jsize>(artifacts.artifactSlots.size());
			jintArray positions = env->NewIntArray(size);
			jintArray flags = env->NewIntArray(size);
			jclass stringClass = env->FindClass("java/lang/String");
			jobjectArray heroNames = env->NewObjectArray(2, stringClass, nullptr);
			jobjectArray names = env->NewObjectArray(size, stringClass, nullptr);
			jlongArray visualKeys = env->NewLongArray(size);
			std::vector<jlong> visualKeyValues(static_cast<std::size_t>(size));
			const std::array heroNameValues = {artifacts.leftHeroName, artifacts.rightHeroName};
			for(jsize side = 0; side < 2; ++side)
			{
				jstring name = env->NewStringUTF(heroNameValues[side].c_str());
				env->SetObjectArrayElement(heroNames, side, name);
				env->DeleteLocalRef(name);
			}
			for(jsize index = 0; index < size; ++index)
			{
				const auto & slot = artifacts.artifactSlots[index];
				const jint position = slot.position;
				visualKeyValues[static_cast<std::size_t>(index)] = static_cast<jlong>(slot.visualAssetKey);
				const jint state = (slot.occupied ? 1 : 0) | (slot.locked ? 2 : 0) | (slot.backpack ? 4 : 0);
				env->SetIntArrayRegion(positions, index, 1, &position);
				env->SetIntArrayRegion(flags, index, 1, &state);
				jstring name = env->NewStringUTF(slot.name.c_str());
				env->SetObjectArrayElement(names, index, name);
				env->DeleteLocalRef(name);
			}
			if(size > 0)
				env->SetLongArrayRegion(visualKeys, 0, size, visualKeyValues.data());
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), static_cast<jint>(artifacts.leftHeroId),
				static_cast<jint>(artifacts.rightHeroId), heroNames, positions, flags, names, visualKeys);
			env->DeleteLocalRef(positions);
			env->DeleteLocalRef(flags);
			env->DeleteLocalRef(heroNames);
			env->DeleteLocalRef(names);
			env->DeleteLocalRef(visualKeys);
			env->DeleteLocalRef(stringClass);
		}, true);
}

bool CAndroidVMHelper::hasThorVisualAsset(std::uint64_t key)
{
	bool cached = false;
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "hasThorVisualAsset", "(J)Z",
		[key, &cached](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			cached = env->CallStaticBooleanMethod(cls, methodId, static_cast<jlong>(key)) == JNI_TRUE;
		}, true);
	return cached;
}

void CAndroidVMHelper::publishThorVisualAsset(std::uint64_t revision, const ThorVisualAssetPayload & payload)
{
	if(!isThorVisualAssetPayloadValid(payload))
		return;
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorVisualAsset", "(JJII[B)V",
		[revision, &payload](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			const auto size = static_cast<jsize>(payload.pngBytes.size());
			jbyteArray bytes = env->NewByteArray(size);
			if(size > 0)
				env->SetByteArrayRegion(bytes, 0, size, reinterpret_cast<const jbyte *>(payload.pngBytes.data()));
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), static_cast<jlong>(payload.key),
				static_cast<jint>(payload.width), static_cast<jint>(payload.height), bytes);
			env->DeleteLocalRef(bytes);
		}, true);
}

jclass CAndroidVMHelper::findClass(const std::string & name, bool classloaded)
{
	if(alwaysUseLoadedClass || classloaded)
	{
		return findClassloadedClass(name);
	}
	return get()->FindClass(name.c_str());
}

void CAndroidVMHelper::initClassloader(void * baseEnv)
{
	CAndroidVMHelper::cacheVM(static_cast<JNIEnv *>(baseEnv));
	CAndroidVMHelper envHelper;
	auto env = envHelper.get();
	auto anyVCMIClass = env->FindClass(CAndroidVMHelper::NATIVE_METHODS_DEFAULT_CLASS);
	jclass classClass = env->GetObjectClass(anyVCMIClass);
	auto classLoaderClass = env->FindClass("java/lang/ClassLoader");
	auto getClassLoaderMethod = env->GetMethodID(classClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
	vcmiClassLoader = (jclass) env->NewGlobalRef(env->CallObjectMethod(anyVCMIClass, getClassLoaderMethod));
	vcmiFindClassMethod = env->GetMethodID(classLoaderClass, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
}
#endif
