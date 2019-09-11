package com.example.mediacodectest.api.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecoder {

    private static final String TAG = "Audio Decoder";

    private static final String DEFAULT_MIME_TYPE = "audio/mp4a-latm";
    private static final int DEFAULT_CHANNEL_NUM = 1;
    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_MAX_BUFFER_SIZE = 16384;

    private MediaCodec mMediaCodec;
    private boolean mIsOpened = false;
    private boolean mIsFirstFrame = true;
    private OnAudioDecodedListener mAudioDecodedListener;

    public void setAudioDecodedListener(OnAudioDecodedListener audioDecodedListener) {
        mAudioDecodedListener = audioDecodedListener;
    }

    public interface OnAudioDecodedListener {
        void onFrameDecoded(byte[] decoded, long presentationTimeUs);
    }

    public boolean open() {
        if (mIsOpened) {
            return true;
        }
        return open(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_NUM, DEFAULT_MAX_BUFFER_SIZE);
    }

    public boolean open(int samplerate, int channels, int maxBufferSize) {
        if (mIsOpened) {
            return true;
        }

        try {
            mMediaCodec = MediaCodec.createDecoderByType(DEFAULT_MIME_TYPE);
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, DEFAULT_MIME_TYPE);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channels);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, samplerate);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize);
            mMediaCodec.configure(format, null, null, 0);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void close() {
        if (!mIsOpened) {
            return;
        }
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
        mIsOpened = false;
    }

    public boolean isOpened() {
        return mIsOpened;
    }

    public synchronized boolean decode(byte[] input, long presentationTimeUs) {
        if (!mIsOpened) {
            return false;
        }
        try {

            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(1000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                if (mIsFirstFrame) {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, presentationTimeUs, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    mIsFirstFrame = false;
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, presentationTimeUs, 0);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return false;
    }

    public synchronized boolean retrieve(){
        if (!mIsOpened) {return false;}

        try {
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
            if (outputBufferIndex >= 0){
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
                if (mAudioDecodedListener != null){
                    mAudioDecodedListener.onFrameDecoded(outData, bufferInfo.presentationTimeUs);
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
