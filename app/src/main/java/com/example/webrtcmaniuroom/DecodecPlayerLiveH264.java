package com.example.webrtcmaniuroom;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecodecPlayerLiveH264 {

    private String remoteIp;
    private MediaCodec mediaCodec;
    public void  initDecoder(String remoteIp, Surface surface){
        this.remoteIp = remoteIp;
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC); //h264类型
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1080, 1920);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1080 * 1920);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void drawSurface(byte[] data){
        //解码 cpu-dsp-cpu

        //获取可用的buffer，超时30毫秒
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(30000);
        if (inputBufferIndex >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            byteBuffer.clear();
            byteBuffer.put(data,0,data.length);
            //pds = 当前帧对应的时间戳, 帧率 *帧index
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length, System.currentTimeMillis(), 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 30000);
        if (outputBufferIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
        }
        Log.d("WDY","解码");
    }

    public String getRemoteIp(){
        return remoteIp;
    }
}
