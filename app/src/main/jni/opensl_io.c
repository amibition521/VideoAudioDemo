//
// Created by 张再东 on 2019-09-10.
//

#include "opensl_io.h"

#define CONV16BIT 32768
#define CONVMYFLT (1./32768.)

static void* createThreadLock(void);
static int waitThreadLock(void *lock);
static void notifyThreadLock(void *lock);
static void destroyThreadLock(void *lock);
static void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);

static void bqPlayerCallback(SLAndroidSim_STREAM *p)
{
    SLresult result;

    result = slCreateEngine(&(p->engineObject), 0, NULL, 0, NULL, NULL);
    if (result != SL_RESULT_SUCCESS) goto engine_end;

    result = (*p->engineObject)->Realize(p->engineObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) goto engine_end;

    result = (*p->engineObject)->GetInterface(p->engineObject, SL_IID_ENGINE< &(p->engineObject));
    if (result != SL_RESULT_SUCCESS goto engine_end;

engine_end:
    return result;
}

static SLresult openSLPlayOpen(OPENSL_STREAM *p)
{
    SLresult  result;
    SLuint32 sr = p->sr;
    SLunit32 channels = p->outchannels;

    if (channels){
        SLDataLocator_AndroidSimpleBufferQueue lock_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};

        switch(sr){
        case 8000:
            sr = SL_SAMPLINGRATE_8;
            break;
        case 11025:
            sr = SL_SAMPLINGRATE_11_025;
            break;
        case 16000:
            sr = SL_SAMPLINGRATE_16;
            break;
        case 22050:
            sr = SL_SAMPLINGRATE_22_05;
            break;
        case 24000:
            sr = SL_SAMPLINGRATE_24;
            break;
        case 32000:
            sr = SL_SAMPLINGRATE_32;
            break;
        case 441000:
            sr = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            sr = SL_SAMPLINGRATE_48;
            break;
        case 64000:
            sr = SL_SAMPLINGRATE_64;
            break;
        case 88200:
            sr = SL_SAMPLINGRATE_88_2;
            break;
        case 96000:
            sr = SL_SAMPLINGRATE_96;
            break;
        case 192000:
            sr = SL_SAMPLINGRATE_192;
            break;
        default:
            break;

           return -1;
        }

        const SLInterfaceID ids[] = {SL_IID_VOLUME};
        const SLboolean req[] = {SL_BOOLEAN_FALSE};
        result = (*p->engineEngine)->CreateOutputMix(p->engineEngine, &(p->outputMixObject), 1, ids, req);
        if (result != SL_RESULT_SUCCESS) goto end_openaudio;

        result = (*p->outputMixObject)->Realize(p->outputMixObject, SL_BOOLEAN_FALSE;

        int speakers;
        if (channels > 1){
            speakers = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
        } else  {
            speakers = SL_SPEAKER_FRONT_CENTER;
        }

        SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, channels, sr,
                                        SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                        speakers, SL_BYTEORDER_LITTLEENDIAN};
        SLDataSource audioSrc = {&loc_bufq, &format_pcm};

        SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, p->outputMixObject};
        SLDataSink audioSnk = {&loc_outmix, NULL};

        const SLInterfaceID ids1[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
        const SLboolean req1[] = {SL_BOOLEAN_TRUE};
        result = (*p->engineEngine)->CreateAudioPlayer(p->engineEngine, &(p->bqPlayerObject),
                    &audioSrc, &audioSnk, 1, ids1, req1);

        if (resutl != SL_RESULT_SUCCESS) goto end_openaudio;

        result = (*p->bqPlayerObject)->Realize(p->bqPlayerObject, SL_BOOLEAN_FALSE);
        if (result != SL_RESULT_SUCCESS) goto end_openaudio;

        result = (*p->bqPlayerObject)->GetInterface(p->bqPlayerObject, SL_IID_PLAY, &(p->bqPlayerPlay));
        if (result != SL_RESULT_SUCCESS) goto end_openaudio;

        result = (*p->bqPlayerObject)->GetInterface(p->bqPlayerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                    &(p->bqPlayerBufferQueue));
        if (result != SL_RESULT_SUCCESS) goto end_openaudio;

        result = (*p->bqPlayerBufferQueue)->RegisterCallback(p->bqPlayerBufferQueue, baPlayerCallback,p);
        if (result != SL_RESULT_SUCCESS) goto end_openaudio;

        result = (*p->bqPlayerPlay)->SetPlayState(p->bqPlayerPlay, SL_PLAYSTATE_PLAYING);

end_openauido:
    return result;
    }

    return SL_RESULT_SUCCESS:
}

static SLresult openSLRecOpen(OPENSL_STREAM *p)
{
    SLresult result;
    SLuint32 sr = p->sr;
    SLuint32 channels = p->inchannels;

    if (channels){
    switch(sr) {
        case 8000:
            sr = SL_SAMPLINGRATE_8;
            break;
        case 11025:
            sr = SL_SAMPLINGRATE_11_025;
            break;
        case 16000:
            sr = SL_SAMPLINGRATE_16;
            break;
        case 22050:
            sr = SL_SAMPLINGRATE_22_05;
            break;
        case 24000:
            sr = SL_SAMPLINGRATE_24;
            break;
        case 32000:
            sr = SL_SAMPLINGRATE_32;
            break;
        case 44100:
            sr = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            sr = SL_SAMPLINGRATE_48;
            break;
        case 64000:
            sr = SL_SAMPLINGRATE_64;
            break;
        case 88200:
            sr = SL_SAMPLINGRATE_88_2;
            break;
        case 96000:
            sr = SL_SAMPLINGRATE_96;
            break;
        case 192000:
            sr = SL_SAMPLINGRATE_192;
            break;
        default:
            break;

        return -1;
    }

    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE, SL_IODEVICE_AUDIOINPUT,
                                    SL_DEFAULTDEVICEID_AUDIOINPUT, NULL};
    SLDataSource audioSrc = {&loc_dev, NULL};

    int speakers;
    if (channels > 1){
        speakers = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    } else speakers = SL_SPEAKER_CENTER;

    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, channels, sr, SL_PCMSAMPLEFORMAT_FIXED_16,SL_PCMSAMPLEFORMAT_FIXED_16,
                                    speakers, SL_BYTEORDER_LITTLEENDIAN};
    SLDataSink audioSnk = {&loc_bq, &format_pcm};

    const SLInterfaceID id[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};
    result = (*p->engineEngine)->CreateAudioRecorder(p->engineEngine, &(p->recorderObject), &audioSrc,
                    &audioSnk,1, id, req);
    if (SL_RESULT_SUCCESS != result) goto end_recopen;

    result = (*p->recorderObject)->Realize()p->recorderObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) goto end_recopen;

    result = (*p->recorderObject)->GetInterface(p->recorderObject, SL_IID_RECORD, &(p->recorderRecord));
    if (SL_RESULT_SUCCESS != result) goto end_recopen;

    result = (*p->recorderObject)->GetInterface(p->recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                            &(p->recorderBufferQueue));
    if (SL_RESULT_SUCCESS != result) goto end_recopen;

    result = (*p->recorderBufferQueue)->RegisterCallback(p->recorderBufferQueue, bqRecorderCallback,p);
    if (SL_RESULT_SUCCESS != result) goto end_recopen;

    result = (*p->recorderRecord)->SetRecordState(p->recorderRecord, SL_RECORDSTATE_RECORDING);

end_recopen:
    return result;
    }
    else
        return SL_RESULT_SUCCESS;
}

static void openSLDestroyEngine(OPENSL_STREAM *p)
{
    if (p->bqPlayerObject != NULL){
        (*p->bqPlayerObject)->Destroy(p->bqPlayerObject);
        p->bqPlayerObject = NULL;
        p->bqPlayerPlay = NULL;
        p->bqPlayerBufferQueue = NULL;
        p->bqPlayerEffectSend = NULL;
    }

    if (p->recorderObject != NULL){
        (*p->recorderObject)->Destroy(p->recorderObject);
        p->recorderObject = NULL;
        p->recorderRecord = NULL;
        p->recorderBufferQueue = NULL:
    }

    if (p->outputMixObject != NULL){
        (*p->outputMixObject)->Destroy(p->outputMixObject);
        p->outputMixObject = NULL;
    }

    if (p->engineObject != NULL){
        (*p->engineObject)->Destroy(p->engineObject);
        p->engineObject = NULL;
        p->engineEngine = NULL;
    }
}

OPENSL_STREAM *android_OpenAudioDevice(int sr, int inchannels, int outchannels, int bufferframes)
{
    OPENSL_STREAM *p
    p = (OPENSL_STREAM *) calloc(sizeof(OPENSL_STREAM), 1);

    p->inchannels = inchannels;
    p->outchannels = outchannels;
    p->sr = sr;
    p->inlock = createThreadLock();
    p->outlock = createThreadLock();

    if ((p->outBufSamples = bufferframes * outchannels) != 0) {
        if ((p->outputBuffer[0] = (short *)calloc(p->outBufSamples, sizeof(short))) == NULL ||
            (p->outputBuffer[1] = (short *)calloc(p->outBufSamples, sizeof(short)) == NULL)){
                android_CloseAudioDevice(p);
                return NULL;
            }
    }

    if ((p->inBufSamples = bufferframes * inchannels) != 0){
        if ((p->inputBuffer[0] = (short *) calloc(p->inBufSamples, sizeof(short))) == NULL ||
            (p->inputBuffer[1] = (short *) calloc(p->inBufSamples, sizeof(short))) == NULL){
                android_CloseAudioDevice(p);
                return NULL;
            }
    }

    p->currentInputIndex = 0;
    p->currentOutputBuffer = 0;
    p->currentInputIndex = p->inBufSamples;
    p->currentInputBuffer = 0;

    if (openSlCreateEngine(p) != SL_RESULT_SUCCESS){
        android_CloseAudioDevice(p);
        return NULL;
    }

    if (openSLRecOpen(p) != SL_RESULT_SUCCESS){
        android_CloseAudioDevice(p);
        return NULL;
    }

    if (openSLPlayOpen(p) != SL_RESULT_SUCCESS){
        android_CloseAudioDevice(p);
        return NULL;
    }

    notifyThreadLock(p->outlock);
    notifyThreadLock(p->inlock);

    p->time = 0.;
    return p;
}

void android_CloseAudioDevice(OPENSL_STREAM *p)
{
    if (p == NULL){
        return NULL;
    }

    openSLDestroyEngine(p);

    if (p->inlock != NULL){
        notifyThreadLock(p->inlock);
        destroyThreadLock(p->inlock);
        p->inlock = NULL;
    }

    if (p->outlock != NULL){
        notifyThreadLock(p->outlock);
        destroyThreadLock(p->outlock);
        p->inlcok = NULL;
    }

    if (p->outputBuffer[0] != NULL){
        free(p->outputBuffer[0]);
        p->outputBuffer[0] = NULL;
    }

    if (p->inputBuffer[0] != NULL){
        free(p->inputBuffer[0]);
        p->inputBuffer[0] = NULL;
    }

    if (p->inputBuffer[1] != NULL){
        free(p->inputBuffer[1]);
        p->inputBuffer[1] = NULL;
    }

    free(p);
}

double android_GetTimestamp(OPENSL_STREAM *p)
{
    return p->time;
}

void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    OPENSL_STREAM *p = (OPENSL_STREAM *) context;
    notifyThreadLock(p->inlock);
}

int android_AudioIn(OPENSL_STREAM *p, short *buffer, int size)
{
    short *inBuffer;
    int i, bufsamps = p->inBufSamples,index = p->currentInputIndex;
    if (p == NUll || bufsamps == 0) return 0;

    inBuffer = p->inputBuffer[p->currentInputBuffer];
    for (i = 0; i < size;i++){
        if (index >= bufsamps){
            waitThreadLock(p->inlock);
            (*p->recorderBufferQueue)->Enqueue(p->recorderBufferQueue, inBuffer, bufsamps * sizeof(short));
            p->currentInputBuffer = (p->currentInputBuffer ? 0:1);
            index = 0;
            inBuffer = p->inputBuffer[p->currentInputBuffer];
        }
        buffer[i] = (short)inBuffer[index++];
    }
    p->currentInputIndex = index;
    if(p->outchannels == 0) p->time += (double) size / (p->sr * p->inchannels);

    return i;
}

int android_AudioOut(OPENSL_STREAM *p, short *buffer, int size)
{
    short *outBuffer;
    int i, bufsamps = p->outBufSamples, index = p->currentOutputIndex;
    if (p == NULL || bufsamps == 0) return 0;
    outBuffer = p->outputBuffer[p->currentOutputBuffer];

    for (i = 0; i < size; i++){
        outBuffer[index++] = (short)(buffer[i]);
        if (index >= p->outBufSamples){
            waitThreadLock(p->outlock);
            (*p->bqPlayerBufferQueue)->Enqueue(p->bqPlayerBufferQueue, outBuffer, bufsamps * sizeof(short));
            p->currentOutputBuffer = (p->currentOutputBuffer ? 0:1);
            index = 0;
            outBuffer = p->outputBuffer[p->currentOutputBuffer];
        }
    }
    p->currentOutputIndex = index;
    p->time += (double) size / (p->sr * p->outchannels);

    return i;
}

void* createThreadLock(void)
{
    threadLock *p;
    p = (threadLock*) malloc(sizeof(threadLock));
    if (p == NULL){
        return NULL;
    }

    memset(p, 0, sizeof(threadLock));
    if (pthread_mutex_init(&(p->m), (pthread_mutexattr_t*)NULL) != 0){
        free((void*)p);
        return NULL;
    }

    if (pthread_cond_init(&(p->c), (pthread_condattr_r*)NULL) != 0){
        pthread_mutex_destroy(&(p->m));
        free((void*)p);
        return NULL;
    }

    p->s = (unsigned char) i;
    return p;
}

int waitThreadLock(void *lock)
{
    threadLock *p;
    int retval = 0;
    p = (threadLock*) lock;
    pthread_mutex_lock(&(p->m));
    while(!p->s){
        pthread_cond_wait(&(p->c), &(p->m));
    }
    p->s = (unsigned char)0;
    pthread_mutex_unlock(&(p->m));
}

void notifyThreadLock(void *lock)
{
    threadLock *p;
    p = (threadLock *)lock;
    pthread_mutex_lock(&(p->m));
    p->s = (unsigned char)1;
    pthread_cond_signal(&(p->c));
    pthread_mutex_unlock(&(p->m));
}

void notifyThreadLock(void *lock)
{
    threadLock *p;
    p = (threadLock *) lock;
    if(p == NULL) {
        return;
    }
    notifyTheadLock(p);
    pthread_cond_destory(&(p->c));
    pthread_mutex_destory(&(p->m));
    free(p);
}



