package com.example.mediacodectest.api.audio.tester;

public class NativeAudioTest extends Tester {

    private boolean mIsTestCapture = true;


    public NativeAudioTest(boolean mIsTestCapture) {
        this.mIsTestCapture = mIsTestCapture;
    }

    static {
        System.loadLibrary("native_audio");
    }

    @Override
    public boolean startTesting() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mIsTestCapture){
                    nativeStartCapture();
                } else {
                    nativeStartPlayback();
                }
            }
        }).start();
        return false;
    }

    @Override
    public boolean stopTesting() {
        if (mIsTestCapture){
            nativeStopCapture();
        } else {
            nativeStopPlayback();
        }
        return false;
    }

    private native boolean nativeStartCapture();

    private native boolean nativeStopCapture();

    private native boolean nativeStartPlayback();

    private native boolean nativeStopPlayback();

}
