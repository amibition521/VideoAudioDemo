//
// Created by 张再东 on 2019-09-10.
//

#ifndef DEMO_OPENSL_IO_H
#define DEMO_OPENSL_IO_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <pthread.h>
#include <stdlib.h>

typedef struct threadLock_ {
    pthread_mutex_t m;
    pthread_cond_t c;
    unsigned char s;
} threadLock;

#ifdef __cplusplus
extern "C" {
#endif

typedef struct opensl_stream {

    //engine interface
    SLObjectItf engineObject;
    SLEngineItf engineEngine;

    //output mix interfaces
    SLObjectItf outputMixObject;

    //buffer queue player interfaces
    SLObjectItf bqPlayerObject;
    SLPlayItf bqPlayerPlay;
    SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
    SLEffectSendItf bqPlayerEffectSend;

    //recorder interfaces
    SLObjectItf recorderObject;
    SLRecordItf recorderRecord;
    SLAndroidSimpleBufferQueueItf recorderbufferQueue;

    int currentInputIndex;
    int currentOutputIndex;

    int currentInputBuffer;
    int currentOutputBuffer;

    short *outputBuffer[2];
    short *inputBuffer[2];

    int outBufSamples;
    int inBufSamples;

    void* inlock;
    void* outlock;

    double time;
    int inchannels
    int outchannels;
    int sr;
} OPENSL_STREAM;

OPENSL_STREAM* android_OpenAudioDevice(int sr, int inchannels, int outchannels, int bufferframes);

void android_CloseAudioDevice(OPENSL_STREAM *p);

int android_AudioIn(OPENSL_STREAM *p, short *buffer, int size);

int android_AudioOut(OPENSL_STREAM *p, short *buffer, int size);

double android_GetTimeStamp(OPENSL_STREAM *p);

#ifdef __cplusplus
};
#endif

#endif






#endif //DEMO_OPENSL_IO_H
