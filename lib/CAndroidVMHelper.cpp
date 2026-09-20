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
