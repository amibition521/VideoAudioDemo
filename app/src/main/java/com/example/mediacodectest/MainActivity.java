package com.example.mediacodectest;

import android.animation.TimeAnimator;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextureView mPlaybackView;
    private TimeAnimator mTimeAnimator = new TimeAnimator();

    private MediaCodecWrapper mCodecWrapper;
    private MediaExtractor mExtractor = new MediaExtractor();
    TextView mAttribView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPlaybackView = findViewById(R.id.PlaybackView);
        mAttribView = findViewById(R.id.AttribView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTimeAnimator != null && mTimeAnimator.isRunning()){
            mTimeAnimator.end();
        }

        if (mCodecWrapper != null){
            mCodecWrapper.stopAndRelease();
            mExtractor.release();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_play){
            mAttribView.setVisibility(View.VISIBLE);
            startPlayback();
            item.setEnabled(false);
        }
        return true;
    }

    public void startPlayback(){
        Uri videoUri = Uri.parse("android.resource://"
        + getPackageName() + "/" +R.raw.vid_bigbuckbunny);

        try {
            mExtractor.setDataSource(this, videoUri, null);
            dumpFormat(mExtractor);
            int nTracks = mExtractor.getTrackCount();
            for (int i = 0; i < nTracks; i++){
                mExtractor.unselectTrack(i);
            }

            for (int i = 0; i < nTracks; i++){
                mCodecWrapper = MediaCodecWrapper.fromVideoFormat(mExtractor.getTrackFormat(i), new Surface(mPlaybackView.getSurfaceTexture()));
                if (mCodecWrapper != null){
                    mExtractor.selectTrack(i);
                    break;
                }
            }

            mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
                @Override
                public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {

                    boolean isEos = ((mExtractor.getSampleFlags() & MediaCodec.BUFFER_FLAG_END_OF_STREAM) ==
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    if (!isEos){
                        boolean result = mCodecWrapper.writeSample(mExtractor, false,
                                mExtractor.getSampleTime(), mExtractor.getSampleFlags());
                        if (result){
                            mExtractor.advance();
                        }
                    }

                    MediaCodec.BufferInfo out_bufferInfo = new MediaCodec.BufferInfo();
                    mCodecWrapper.peekSample(out_bufferInfo);

                    if (out_bufferInfo.size <= 0 && isEos){
                        mTimeAnimator.end();
                        mCodecWrapper.stopAndRelease();
                        mExtractor.release();
                    } else if (out_bufferInfo.presentationTimeUs / 1000  < totalTime){
                        mCodecWrapper.popSample(true);
                    }
                }
            });

            mTimeAnimator.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpFormat(MediaExtractor extractor){
        int count = extractor.getTrackCount();
        Log.d("zzd", "play video: track count: "+ count);
        for (int i = 0; i < count; i++){
            MediaFormat format = extractor.getTrackFormat(i);
            Log.d("zzd", "play video: track " + i + ":" + getTrackInfo(format));
        }
    }

    private String getTrackInfo(MediaFormat format){
       String info = format.getString(MediaFormat.KEY_MIME);
       if (info.contains("audio/")){
           info += " samplerate: "+ format.getInteger(MediaFormat.KEY_SAMPLE_RATE) +", channel count: " + format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
       } else if (info.contains("video/")){
            info += " size: "+ format.getInteger(MediaFormat.KEY_WIDTH) +
                    "X" + format.getInteger(MediaFormat.KEY_HEIGHT);
       }
       return info;
    }


}
