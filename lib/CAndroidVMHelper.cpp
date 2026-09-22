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
										 const ThorContextDetails & details)
{
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorContext",
		"(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
		"Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
		[revision, &contextId, &title, &status, &details](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			jstring javaContextId = env->NewStringUTF(contextId.c_str());
			jstring javaTitle = env->NewStringUTF(title.c_str());
			jstring javaStatus = env->NewStringUTF(status.c_str());
			std::array<jstring, THOR_CONTEXT_DETAIL_LINE_COUNT> javaDetails;
			for(std::size_t index = 0; index < details.size(); ++index)
				javaDetails[index] = env->NewStringUTF(details[index].c_str());
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), javaContextId, javaTitle, javaStatus,
				javaDetails[0], javaDetails[1], javaDetails[2], javaDetails[3]);
			env->DeleteLocalRef(javaContextId);
			env->DeleteLocalRef(javaTitle);
			env->DeleteLocalRef(javaStatus);
			for(const auto javaDetail : javaDetails)
				env->DeleteLocalRef(javaDetail);
		}, true);
}

void CAndroidVMHelper::publishThorActionState(std::uint64_t revision, std::uint32_t enabledActionMask,
	std::uint32_t activeActionMask)
{
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorActionState", "(JII)V",
		[revision, enabledActionMask, activeActionMask](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), static_cast<jint>(enabledActionMask),
				static_cast<jint>(activeActionMask));
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

void CAndroidVMHelper::publishThorHeroMeeting(std::uint64_t revision, const ThorHeroMeetingArmy & meeting)
{
	callCustomMethod(NATIVE_METHODS_DEFAULT_CLASS, "publishThorHeroMeeting",
		"(J[I[Ljava/lang/String;[I[I[I[I[Ljava/lang/String;[I[I)V",
		[revision, &meeting](JNIEnv * env, jclass cls, jmethodID methodId)
		{
			const auto size = static_cast<jsize>(meeting.slots.size());
			jintArray heroIds = env->NewIntArray(2);
			jintArray keys = env->NewIntArray(size), sides = env->NewIntArray(size), slots = env->NewIntArray(size);
			jintArray creatureIds = env->NewIntArray(size), counts = env->NewIntArray(size), flags = env->NewIntArray(size);
			jclass stringClass = env->FindClass("java/lang/String");
			jobjectArray heroNames = env->NewObjectArray(2, stringClass, nullptr);
			jobjectArray creatureNames = env->NewObjectArray(size, stringClass, nullptr);
			for(jsize side = 0; side < 2; ++side)
			{
				const jint id = meeting.heroIds[side];
				env->SetIntArrayRegion(heroIds, side, 1, &id);
				jstring name = env->NewStringUTF(meeting.heroNames[side].c_str());
				env->SetObjectArrayElement(heroNames, side, name);
				env->DeleteLocalRef(name);
			}
			for(jsize index = 0; index < size; ++index)
			{
				const auto & entry = meeting.slots[index];
				const jint key = entry.key, side = entry.side, slot = entry.slot, creature = entry.creatureId;
				const jint count = entry.count, state = (entry.occupied ? 1 : 0) | (entry.movable ? 2 : 0);
				env->SetIntArrayRegion(keys, index, 1, &key); env->SetIntArrayRegion(sides, index, 1, &side);
				env->SetIntArrayRegion(slots, index, 1, &slot); env->SetIntArrayRegion(creatureIds, index, 1, &creature);
				env->SetIntArrayRegion(counts, index, 1, &count); env->SetIntArrayRegion(flags, index, 1, &state);
				jstring name = env->NewStringUTF(entry.creatureName.c_str());
				env->SetObjectArrayElement(creatureNames, index, name); env->DeleteLocalRef(name);
			}
			env->CallStaticVoidMethod(cls, methodId, static_cast<jlong>(revision), heroIds, heroNames, keys, sides,
				slots, creatureIds, creatureNames, counts, flags);
			env->DeleteLocalRef(heroIds); env->DeleteLocalRef(heroNames); env->DeleteLocalRef(keys);
			env->DeleteLocalRef(sides); env->DeleteLocalRef(slots); env->DeleteLocalRef(creatureIds);
			env->DeleteLocalRef(creatureNames); env->DeleteLocalRef(counts); env->DeleteLocalRef(flags);
			env->DeleteLocalRef(stringClass);
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
