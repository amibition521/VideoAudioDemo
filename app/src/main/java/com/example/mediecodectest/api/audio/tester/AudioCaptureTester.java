package com.example.mediecodectest.api.audio.tester;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Environment;

import com.example.mediecodectest.MediaCodecWrapper;
import com.example.mediecodectest.api.audio.AudioCapture;
import com.example.mediecodectest.api.audio.wav.WavFileHeader;
import com.example.mediecodectest.api.audio.wav.WavFileWrite;

public class AudioCaptureTester extends Tester implements AudioCapture.OnAudioFrameCapturedListener{

    private static final String DEFAULT_TEST_FILE = Environment.getExternalStorageDirectory() + "/test.wav";

    private AudioCapture mAudioCapture;
    private WavFileWrite mWavFileWriter;


    @Override
    public boolean startTesting() {
        mAudioCapture = new AudioCapture();
        mWavFileWriter = new WavFileWrite();
        try {
            mWavFileWriter.openFile(DEFAULT_TEST_FILE, 44100, 1, 16);
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        mAudioCapture.setAudioFrameCaptureListener(this);
        return mAudioCapture.startCapture(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    @Override
    public boolean stopTesting() {
        mAudioCapture.stopCapture();

        try {
            mWavFileWriter.closeFile();
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return false;
    }

    @Override
    public void onAudioFrameCaptured(byte[] audioData) {
        mWavFileWriter.writeData(audioData, 0, audioData.length);
    }
}
