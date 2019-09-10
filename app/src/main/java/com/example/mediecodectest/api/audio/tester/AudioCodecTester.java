package com.example.mediecodectest.api.audio.tester;

import com.example.mediecodectest.api.audio.AudioCapture;
import com.example.mediecodectest.api.audio.AudioDecoder;
import com.example.mediecodectest.api.audio.AudioEncoder;
import com.example.mediecodectest.api.audio.AudioPlayer;

public class AudioCodecTester extends Tester implements AudioCapture.OnAudioFrameCapturedListener,
        AudioEncoder.OnAudioEncodedListener, AudioDecoder.OnAudioDecodedListener {

    private AudioEncoder mAudioEncoder;
    private AudioDecoder mAudioDecoder;
    private AudioCapture mAudioCapture;
    private AudioPlayer mAudioPlayer;
    private volatile boolean mIsTestingExit = false;


    @Override
    public boolean startTesting() {
        mAudioCapture = new AudioCapture();
        mAudioEncoder = new AudioEncoder();
        mAudioDecoder = new AudioDecoder();
        mAudioPlayer = new AudioPlayer();

        if (!mAudioEncoder.open()|| !mAudioDecoder.open()){
            return false;
        }

        mAudioEncoder.setAudioEncodedListener(this);
        mAudioDecoder.setAudioDecodedListener(this);
        mAudioCapture.setAudioFrameCaptureListener(this);

        new Thread(mEncodeRenderRunnable).start();
        new Thread(mDecodeRenderRunnable).start();

        if (!mAudioCapture.startCapture()){
            return false;
        }
        mAudioPlayer.startPlayer();

        return false;
    }

    @Override
    public boolean stopTesting() {
        mIsTestingExit = true;
        mAudioCapture.stopCapture();
        return false;
    }



    @Override
    public void onAudioFrameCaptured(byte[] audioData) {
        long presentationTimeUs = (System.nanoTime()) / 1000L;
        mAudioEncoder.encode(audioData, presentationTimeUs);
    }

    @Override
    public void onFrameDecoded(byte[] decoded, long presentationTimeUs) {
        mAudioPlayer.play(decoded, 0, decoded.length);
    }

    @Override
    public void onFrameEncoded(byte[] encoded, long presentationTimeUs) {
        mAudioDecoder.decode(encoded, presentationTimeUs);
    }

    private Runnable mEncodeRenderRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mIsTestingExit){
                mAudioEncoder.retrieve();
            }
            mAudioEncoder.close();
        }
    };

    private Runnable mDecodeRenderRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mIsTestingExit){
                mAudioDecoder.retrieve();
            }
            mAudioDecoder.close();
        }
    };
}
