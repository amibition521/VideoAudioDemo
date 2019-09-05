package com.example.mediecodectest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayer {

    private static final String TAG = "AudioPlayer";

    private static final int DEFAULT_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int DEFAULT_PLAY_MODE = AudioTrack.MODE_STREAM;

    private volatile boolean mIsPlayStarted = false;
    private AudioTrack mAudioTrack;

    public boolean startPlayer(){
        return startPlayer(DEFAULT_STREAM_TYPE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT);
    }
    public boolean startPlayer(int streamType, int sampleRateInHz, int channelConfig, int audioFormat){
        if (mIsPlayStarted){return false;}

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (bufferSizeInBytes == AudioTrack.ERROR_BAD_VALUE){
            return false;
        }

        mAudioTrack = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, DEFAULT_PLAY_MODE);
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED){
            return false;
        }
        mIsPlayStarted = true;
        return true;
    }

    public boolean play(byte[] audioData, int offsetInBytes, int sizeInBytes){
        if (!mIsPlayStarted){return false;}
        if (mAudioTrack.write(audioData, offsetInBytes, sizeInBytes) != sizeInBytes){
            Log.d(TAG, "Could not write all the samples to the audio device !");
        }
        mAudioTrack.play();
        return true;
    }

    public void stopPlayer(){
        if (!mIsPlayStarted){
            return;
        }

        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING){
            mAudioTrack.stop();
        }
        mAudioTrack.release();
        mIsPlayStarted = false;
    }

}
