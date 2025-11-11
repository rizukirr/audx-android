#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include "include/audx/denoiser.h"
}

#define LOG_TAG "DenoiserJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_android_audx_AudxDenoiser_createNative(
        JNIEnv *env,
        jobject /* this */,
        jint modelPreset,
        jstring modelPath,
        jfloat vadThreshold,
        jboolean enableVadOutput) {

    struct DenoiserConfig config{};
    config.model_preset = static_cast<ModelPreset>(modelPreset);

    const char *model_path_str = nullptr;
    if (modelPath != nullptr) {
        model_path_str = env->GetStringUTFChars(modelPath, nullptr);
    }
    config.model_path = model_path_str;
    config.vad_threshold = vadThreshold;
    config.enable_vad_output = enableVadOutput;

    auto *denoiser = new Denoiser();

    int ret = denoiser_create(&config, denoiser);
    if (ret != AUDX_SUCCESS) {
        delete denoiser;
        return 0;
    }

    if (model_path_str != nullptr) {
        env->ReleaseStringUTFChars(modelPath, model_path_str);
    }

    if (ret < 0) {
        LOGE("Failed to create denoiser: %d", ret);
        delete denoiser;
        return 0;
    }

    return reinterpret_cast<jlong>(denoiser);
}

extern "C" JNIEXPORT void JNICALL
Java_com_android_audx_AudxDenoiser_destroyNative(
        JNIEnv *env,
        jobject /* this */,
        jlong handle) {

    auto *denoiser = reinterpret_cast<Denoiser *>(handle);
    if (denoiser != nullptr) {
        denoiser_destroy(denoiser);
        delete denoiser;
        LOGI("Denoiser destroyed");
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_android_audx_AudxDenoiser_processNative(
        JNIEnv *env,
        jobject /* this */,
        jlong handle,
        jshortArray inputArray,
        jshortArray outputArray) {

    auto *denoiser = reinterpret_cast<Denoiser *>(handle);
    if (denoiser == nullptr) {
        LOGE("Invalid denoiser handle");
        return nullptr;
    }

    // Get array pointers
    jshort *input = env->GetShortArrayElements(inputArray, nullptr);
    jshort *output = env->GetShortArrayElements(outputArray, nullptr);

    struct DenoiserResult result{};
    int ret = denoiser_process(denoiser, input, output, &result);

    // Release arrays
    env->ReleaseShortArrayElements(inputArray, input, JNI_ABORT);
    env->ReleaseShortArrayElements(outputArray, output, 0);

    if (ret != AUDX_SUCCESS) {
        LOGE("Denoiser processing failed: %d", ret);
        return nullptr;
    }

    // Find Kotlin class
    jclass resultClass = env->FindClass("com/android/audx/DenoiserResult");
    if (resultClass == nullptr) {
        LOGE("Cannot find DenoiserResult class");
        return nullptr;
    }

    // Find constructor: (FFI)V — 2 floats + int
    jmethodID ctor = env->GetMethodID(resultClass, "<init>", "(FZI)V");
    if (ctor == nullptr) {
        LOGE("Cannot find DenoiserResult constructor");
        return nullptr;
    }

    // Create and return Kotlin object
    jobject resultObj = env->NewObject(
            resultClass,
            ctor,
            result.vad_probability,
            result.is_speech,
            result.samples_processed
    );

    return resultObj;
}

// Expose native audio format constants to Kotlin
extern "C" JNIEXPORT jint JNICALL
Java_com_android_audx_AudxDenoiser_getSampleRateNative(
        JNIEnv *env,
        jclass /* clazz */) {
    return AUDX_SAMPLE_RATE_48KHZ;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_android_audx_AudxDenoiser_getChannelsNative(
        JNIEnv *env,
        jclass /* clazz */) {
    return AUDX_CHANNELS_MONO;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_android_audx_AudxDenoiser_getBitDepthNative(
        JNIEnv *env,
        jclass /* clazz */) {
    return AUDX_BIT_DEPTH_16;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_android_audx_AudxDenoiser_getFrameSizeNative(
        JNIEnv *env,
        jclass /* clazz */) {
    return AUDX_FRAME_SIZE;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_android_audx_AudxDenoiser_getStatsNative(
        JNIEnv *env,
        jobject /* this */,
        jlong handle) {

    auto *denoiser = reinterpret_cast<Denoiser *>(handle);
    if (denoiser == nullptr) {
        LOGE("Invalid denoiser handle");
        return nullptr;
    }

    struct DenoiserStats stats{};
    int ret = get_denoiser_stats(denoiser, &stats);

    if (ret != AUDX_SUCCESS) {
        LOGE("Failed to get denoiser stats: %d", ret);
        return nullptr;
    }

    // Find Kotlin class
    jclass statsClass = env->FindClass("com/android/audx/DenoiserStats");
    if (statsClass == nullptr) {
        LOGE("Cannot find DenoiserStats class");
        return nullptr;
    }

    // Find constructor: (IFFFFFFF)V — int + 7 floats
    jmethodID ctor = env->GetMethodID(statsClass, "<init>", "(IFFFFFFF)V");
    if (ctor == nullptr) {
        LOGE("Cannot find DenoiserStats constructor");
        return nullptr;
    }

    // Create and return Kotlin object
    jobject statsObj = env->NewObject(
            statsClass,
            ctor,
            stats.frame_processed,
            stats.speech_detected,
            stats.vscores_avg,
            stats.vscores_min,
            stats.vscores_max,
            stats.ptime_total,
            stats.ptime_avg,
            stats.ptime_last
    );

    return statsObj;
}

extern "C" JNIEXPORT void JNICALL
Java_com_android_audx_AudxDenoiser_resetStatsNative(
        JNIEnv *env,
        jobject /* this */,
        jlong handle) {

    auto *denoiser = reinterpret_cast<Denoiser *>(handle);
    if (denoiser == nullptr) {
        LOGE("Invalid denoiser handle");
        return;
    }

    // Manually reset all statistics fields
    denoiser->frames_processed = 0;
    denoiser->speech_frames = 0;
    denoiser->total_vad_score = 0.0f;
    denoiser->min_vad_score = 1.0f;  // Reset to max so first frame sets new min
    denoiser->max_vad_score = 0.0f;  // Reset to min so first frame sets new max
    denoiser->total_processing_time = 0.0;
    denoiser->last_frame_time = 0.0;

    LOGI("Denoiser statistics reset");
}
