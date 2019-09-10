package com.example.mediecodectest.api.audio.wav;

import android.util.Log;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavFileReader {
    private static final String TAG = "WavFileReader";

    private DataInputStream mDataInputStream;
    private WavFileHeader mWavFileHeader;

    public boolean openFile(String filePath) throws IOException {
        if (mDataInputStream != null){
            closeFile();
        }
        mDataInputStream = new DataInputStream(new FileInputStream(filePath));
        return readHeader();
    }

    public void closeFile() throws IOException {
        if (mDataInputStream != null){
            mDataInputStream.close();
            mDataInputStream = null;
        }
    }

    public WavFileHeader getmWavFileHeader() {
        return mWavFileHeader;
    }

    public int readData(byte[] buffer, int offset, int count){
        if (mDataInputStream == null || mWavFileHeader == null){
            return -1;
        }

        try {
            int nbytes = mDataInputStream.read(buffer, offset, count);
            if (nbytes == -1){
                return 0;
            }
            return nbytes;
        }catch (Exception e){
            e.printStackTrace();
        }
        return  -1;
    }

    private boolean readHeader() {
        if (mDataInputStream == null){
            return false;
        }

        WavFileHeader header = new WavFileHeader();
        byte[] intValue = new byte[4];
        byte[] shortValue = new byte[2];

        try {
            header.mChunkID = "" + mDataInputStream.readByte() + mDataInputStream.readByte() + mDataInputStream.readByte() + mDataInputStream.readByte();

            mDataInputStream.read(intValue);
            header.mChunkSize =  byteArrayToInt(intValue);
            header.mFormat = "" + mDataInputStream.readByte() + mDataInputStream.readByte() + mDataInputStream.readByte() + mDataInputStream.readByte();
            header.mSubChunk1ID = "" + mDataInputStream.readByte() + mDataInputStream.readByte() + mDataInputStream.readByte() + mDataInputStream.readByte();

            mDataInputStream.read(intValue);
            header.mSubChunk1Size = byteArrayToInt(intValue);

            mDataInputStream.read(shortValue);
            header.mAudioFormat = byteArrayToShort(shortValue);

            mDataInputStream.read(shortValue);
            header.mNumChannel = byteArrayToShort(shortValue);

            mDataInputStream.read(intValue);
            header.mSampleRate = byteArrayToInt(intValue);

            mDataInputStream.read(intValue);
            header.mByteRate = byteArrayToInt(intValue);

            mDataInputStream.read(shortValue);
            header.mBlockAlign = byteArrayToShort(shortValue);

            mDataInputStream.read(shortValue);
            header.mBitsPerSample = byteArrayToShort(shortValue);

            header.mSubChunk2ID = "" + mDataInputStream.readByte() + mDataInputStream.readByte() + mDataInputStream.readByte() + mDataInputStream.readByte();

            mDataInputStream.read(intValue);
            header.mSubChunk2Size = byteArrayToInt(intValue);

            Log.d(TAG, "Read wav file success!!!");

        } catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }

    private static short byteArrayToShort(byte[] b){
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    private static int byteArrayToInt(byte[] b){
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
}
