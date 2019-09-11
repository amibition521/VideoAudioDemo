//
// Created by 张再东 on 2019-09-10.
//

#include <stdio.h>
#include <jni.h>
#include <android/log.h>
#include "opensl_io.h"

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG, "AudioDemo-JNI", __VA_ARGS__);

#define SAMPLERATE 44100
#define CHANNELS 1
#define PERIOD_TIME 20
#define FRAME_SIZE SAMPLERATE * PERIOD_TIME / 1000
#define BUFFER_SIZE FRAME_SIZE * CHANNELS
#define TEST_CAPTURE_FILE_PATH "/sdcard/audio.pcm"

static volatile int g_loop_exit =0;
//com.example.mediacodectest.api.audio.tester
JNIEXPORT jboolean JNICALL Java_com_example_mediacodectest_api_audio_test_NativeAudioTest_native_StartCapture(JNIENV *env, jobject thiz)
{
    FILE *fp = fopen(TEST_CAPTURE_FILE_PATH, "wb");
    if (fp == NULL){
        LOG("cannot open file (%s)\n", TEST_CAPTURE_FILE_PATH);
        return -1;
    }

    OPENSL_STREAM* stream = android_OpenAudioDevice(SAMPLERATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if (stream == NULL){
        fclose(fp);
        LOG("failed to open audio device!\n");
        return JNI_FALSE;
    }

    int samples;
    short buffer[BUFFER_SIZE];
    g_loop_exit = 0;
    while(!g_loope_exit){
        samples = android_AudioIn(stream, buffer, BUFFER_SIZE);
        if (samples < 0){
            LOG("android_AudioIn failed !\n");
            break;
        }
        if (fwrite((unsigned char*)buffer, samples * sizeof(short),1, fp) != 1){
            LOG("failed to save captured data !\n");
            break;
        }
        LOG("capture %d samples !\n", samples);
    }

    android_CloseAudioDevice(stream);
    fclose(fp);
    LOG("nativeStartCapture complete !");

    return JNI_TRUE;
}


//nativeStopCapture
JNIEXPORT jboolean JNICALL Java_com_example_mediacodectest_api_audio_test_NativeAudioTest_native_StopCapture(JNIENV *env, jobject thiz)
{
    g_loop_exit = 1;
    return JNI_TRUE;
}
JNIEXPORT jboolean JNICALL Java_com_example_mediacodectest_api_audio_test_NativeAudioTest_native_StartPlayback(JNIENV *env, jobject thiz)
{
    FILE *fp = fopen(TEST_CAPTURE_FILE_PATH, "rb");
    if (fp == NULL) {
        LOG("cannot open file (%s)!\n", TEST_CAPTURE_FILE_PATH);
        return -1;
    }

    OPENSL_STREAM* stream = android_OpenAudioDevice(SAMPLERATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if(stream == NULL){
        fclose(fp);
        LOG("failed to open audio device !\n");
        return JNI_FALSE;
    }

    int samples
    short buffer[BUFFER_SIZE];
    g_loop_exit = 0;
    while(!g_loop_exit && !feof(fp)){
        if (fread((unsigned char *)buffer, BUFFER_SIZE * 2, 1, fp) != 1){
            LOG("failed to read data \n");
            break;
        }
        samples = android_AudioOut(stream, buffer, BUFFER_SIZE);
        if (samples < 0){
            LOG("android_AudioOut failed !\n");
        }
        LOG("playback %d samples !\n", sample);
    }

    android_CloseAudioDevice(stream);
    fclose(fp);

    LOG("native Start Play back completed !");

    return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL Java_com_example_mediacodectest_api_audio_test_NativeAudioTest_native_StopPlayback(JNIENV *env, jobject thiz)
{
    g_loop_exit = 1;
    return JNI_TRUE;
}

