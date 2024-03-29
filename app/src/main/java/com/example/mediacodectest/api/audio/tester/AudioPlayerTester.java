package com.example.mediacodectest.api.audio.tester;

import android.os.Environment;

import com.example.mediacodectest.api.audio.AudioPlayer;
import com.example.mediacodectest.api.audio.wav.WavFileReader;

public class AudioPlayerTester extends Tester  {

    private static final String DEFAULT_TEST_FILE = Environment.getExternalStorageDirectory() + "/test.wav";

    private static final int SAMPLES_PER_FRAME = 1024;

    private AudioPlayer mAudioPlayer;
    private WavFileReader mWavFileReader;
    private volatile boolean mIsTestingExit = false;

    @Override
    public boolean startTesting() {
        mWavFileReader  = new WavFileReader();
        mAudioPlayer = new AudioPlayer();

        try {
            mWavFileReader.openFile(DEFAULT_TEST_FILE);
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        mAudioPlayer.startPlayer();
        new Thread(AudioPlayRunnable).start();
        return false;
    }

    @Override
    public boolean stopTesting() {
        mIsTestingExit = true;
        return true;
    }

    private Runnable AudioPlayRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] buffer = new byte[SAMPLES_PER_FRAME * 2];
            while(!mIsTestingExit && mWavFileReader.readData(buffer, 0, buffer.length) > 0){
                mAudioPlayer.play(buffer, 0, buffer.length);
            }
            mAudioPlayer.stopPlayer();

            try {
                mWavFileReader.closeFile();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };
}
