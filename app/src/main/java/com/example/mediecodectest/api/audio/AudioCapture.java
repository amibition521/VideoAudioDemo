package com.example.mediecodectest.api.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioCapture {

    private static final String TAG = "AudioCapture";

    private static final int DEFAULT_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_CHANNAL_LAYOUT = AudioFormat.CHANNEL_IN_MONO;
    private static final int DEFAULT_DATA_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int SAMPLE_PER_FRAME = 1024;

    private AudioRecord mAudioRecord;

    private Thread mCaptureThread;
    private boolean mIsCaptureStarted = false;
    private volatile boolean mIsLoopExit = false;

    private OnAudioFrameCapturedListener mAudioFrameCaptureListener;

    public void setAudioFrameCaptureListener(OnAudioFrameCapturedListener audioFrameCaptureListener) {
        mAudioFrameCaptureListener = audioFrameCaptureListener;
    }

    public interface  OnAudioFrameCapturedListener {
        void onAudioFrameCaptured(byte[] audioData);
    }

    public boolean isIsCaptureStarted() {
        return mIsCaptureStarted;
    }

    public boolean startCapture(){
        return startCapture(DEFAULT_SOURCE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNAL_LAYOUT, DEFAULT_DATA_FORMAT);
    }

    public boolean startCapture(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat){
        if (mIsCaptureStarted){
            return false;
        }
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE){
            return false;
        }

        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz,channelConfig, audioFormat, minBufferSize * 4);
        if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED){
            return false;
        }
        mAudioRecord.startRecording();
        mIsLoopExit = false;
        mCaptureThread = new Thread(new AudioCaptureRunnable());
        mCaptureThread.start();

        mIsCaptureStarted = true;

        return true;
    }

    public void stopCapture(){
        if (!mIsCaptureStarted) {
            return;
        }
        mIsLoopExit = true;

        try {
            mCaptureThread.interrupt();
            mCaptureThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
            mAudioRecord.stop();
        }
        mAudioRecord.release();

        mIsCaptureStarted = false;
        mAudioFrameCaptureListener = null;
    }

    private class AudioCaptureRunnable implements Runnable {

        @Override
        public void run() {
            while (!mIsLoopExit){
                byte[] buffer = new byte[SAMPLE_PER_FRAME * 2];
                int ret = mAudioRecord.read(buffer, 0, buffer.length);
                if (ret == AudioRecord.ERROR_INVALID_OPERATION){
                    Log.e(TAG, "Error ERROR_INVALID_OPERATION");
                } else if (ret == AudioRecord.ERROR_BAD_VALUE) {
                    Log.d(TAG, "Error ERROR_BAD_VALUE");
                } else {
                    Log.d(TAG, "Audio captured: " + buffer.length);
                    if (mAudioFrameCaptureListener != null){
                        mAudioFrameCaptureListener.onAudioFrameCaptured(buffer);
                    }
                }
            }
        }
    }
}
