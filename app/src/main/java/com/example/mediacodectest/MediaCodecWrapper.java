package com.example.mediacodectest;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;

public class MediaCodecWrapper {

    private Handler mHandler;

    public interface OutputFormatChangeListener {
        void outputFormatChanged(MediaCodecWrapper sender, MediaFormat newFormat);
    }

    private OutputFormatChangeListener mOutputFormatChangeListener = null;

    public interface OutputSampleListener {
        void outputSample(MediaCodecWrapper sender, MediaCodec.BufferInfo info, ByteBuffer buffer);
    }

    private MediaCodec mDecoder;

    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;

    private Queue<Integer> mAvailableInputBuffers;
    private Queue<Integer> mAvailableOutputBuffers;

    private MediaCodec.BufferInfo[] mOutputBufferInfo;

    public MediaCodecWrapper(MediaCodec codec) {
        this.mDecoder = codec;
        codec.start();
        mInputBuffers = codec.getInputBuffers();
        mOutputBuffers = codec.getOutputBuffers();
        mOutputBufferInfo = new MediaCodec.BufferInfo[mOutputBuffers.length];
        mAvailableInputBuffers = new ArrayDeque<>(mOutputBuffers.length);
        mAvailableOutputBuffers = new ArrayDeque<>(mInputBuffers.length);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initMediaCodec(MediaExtractor extractor, Surface surface) throws IOException {
        MediaFormat selTrackFmt = chooseVideoTrack(extractor);
        mDecoder = createCodec(selTrackFmt, surface);
    }

    private MediaFormat chooseVideoTrack(MediaExtractor extractor){
        int count = extractor.getTrackCount();
        for (int i = 0; i < count; i++){
            MediaFormat format = extractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")){
                extractor.selectTrack(i);
                return  format;
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private MediaCodec createCodec(MediaFormat format , Surface surface) throws IOException {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodec codec = MediaCodec.createByCodecName(codecList.findDecoderForFormat(format));
        codec.configure(format, surface, null, 0);
        return codec;
    }

    public void stopAndRelease() {
        if (mDecoder != null){
            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
            mHandler = null;
        }
    }

    public OutputFormatChangeListener getOutputFormatChangeListener() {
        return mOutputFormatChangeListener;
    }

    public void setOutputFormatChangeListener(final OutputFormatChangeListener outputFormatChangeListener, Handler handler) {
        mOutputFormatChangeListener = outputFormatChangeListener;
        mHandler = handler;
        if (outputFormatChangeListener != null && mHandler == null) {
            if (Looper.myLooper() != null) {
                mHandler = new Handler();
            } else {
                throw new IllegalArgumentException("Looper doesn't exist in the calling thread");
            }
        }
    }

    public static MediaCodecWrapper fromVideoFormat(final MediaFormat trackFormat, Surface surface) {
        MediaCodecWrapper result = null;
        MediaCodec videoCodec = null;

        final String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
        if (mimeType.contains("video/")) {
            try {
                videoCodec = MediaCodec.createDecoderByType(mimeType);
                videoCodec.configure(trackFormat, surface, null, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (videoCodec != null) {
            result = new MediaCodecWrapper(videoCodec);
        }
        return result;
    }

    public boolean writeSample(final ByteBuffer input, final MediaCodec.CryptoInfo crypto,
                               final long presentationTimeUs, final int flags) throws WriteExpection {
        boolean result = false;
        int size = input.remaining();

        if (size > 0 && !mAvailableInputBuffers.isEmpty()) {
            int index = mAvailableInputBuffers.remove();
            ByteBuffer buffer = mInputBuffers[index];

            if (size > buffer.capacity()) {
                String exception = String.format(Locale.US,
                        "Insufficient capcity in MediaCodec buffer: tried to write %d, buffer capacity is %d.",
                        input.remaining(), buffer.capacity());
                throw new MediaCodecWrapper.WriteExpection(exception);
            }

            buffer.clear();
            buffer.put(input);

            if (crypto == null) {
                mDecoder.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
            } else {
                mDecoder.queueSecureInputBuffer(index, 0, crypto, presentationTimeUs, flags);
            }
            result = true;
        }
        return result;
    }

    private static MediaCodec.CryptoInfo sCryptoInfo = new MediaCodec.CryptoInfo();

    public boolean writeSample(final MediaExtractor extractor, final boolean isSecure,
                               final long presentationTimeUs, int flags) {
        boolean result = false;

        if (!mAvailableInputBuffers.isEmpty()) {
            int index = mAvailableInputBuffers.remove();
            ByteBuffer buffer = mInputBuffers[index];

            int size = extractor.readSampleData(buffer, 0);
            if (size <= 0) {
                flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }

            if (!isSecure) {
                mDecoder.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
            } else {
                extractor.getSampleCryptoInfo(sCryptoInfo);
                mDecoder.queueSecureInputBuffer(index, 0, sCryptoInfo, presentationTimeUs, flags);
            }
            result = true;
        }

        return result;
    }

    public boolean peekSample(MediaCodec.BufferInfo out_bufferInfo) {
        update();
        boolean result = false;
        if (!mAvailableOutputBuffers.isEmpty()) {
            int index = mAvailableOutputBuffers.peek();
            MediaCodec.BufferInfo info = mOutputBufferInfo[index];
            out_bufferInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags);
            result = true;
        }

        return result;
    }

    public void popSample(boolean render) {
        update();
        if (!mAvailableOutputBuffers.isEmpty()) {
            int index = mAvailableOutputBuffers.remove();
            mDecoder.releaseOutputBuffer(index, render);
        }
    }

    private void update() {
        int index;

        while ((index = mDecoder.dequeueInputBuffer(0)) != MediaCodec.INFO_TRY_AGAIN_LATER) {
            mAvailableInputBuffers.add(index);
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while ((index = mDecoder.dequeueOutputBuffer(info, 0)) != MediaCodec.INFO_TRY_AGAIN_LATER) {
            switch (index) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    mOutputBuffers = mDecoder.getOutputBuffers();
                    mOutputBufferInfo = new MediaCodec.BufferInfo[mOutputBuffers.length];
                    mAvailableOutputBuffers.clear();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    if (mOutputFormatChangeListener != null){
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mOutputFormatChangeListener.outputFormatChanged(MediaCodecWrapper.this,
                                        mDecoder.getOutputFormat());
                            }
                        });
                    }
                    break;
                default:
                    if (index >= 0){
                        mOutputBufferInfo[index] = info;
                        mAvailableOutputBuffers.add(index);
                    } else {
                        throw new IllegalStateException("Unknown, status from dequeueOutputBuffer");
                    }
                    break;
            }
        }
    }

    /**
     * 异步方式
     * @param extractor
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void AsynchronousMode(final MediaExtractor extractor){
        mDecoder.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                ByteBuffer buffer = codec.getInputBuffer(index);
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0){
                    codec.queueInputBuffer(index, 0,0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    long sampleTime = extractor.getSampleTime();
                    codec.queueInputBuffer(index, 0, sampleSize, sampleTime, 0);
                    extractor.advance();
                }
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                codec.releaseOutputBuffer(index, true);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.d("zzd", e.getMessage());
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                Log.d("zzd", "output format change "+ format);
            }
        });
    }

    private class WriteExpection extends Throwable{
        private WriteExpection(final String detailMessage){
            super(detailMessage);
        }
    }

}
