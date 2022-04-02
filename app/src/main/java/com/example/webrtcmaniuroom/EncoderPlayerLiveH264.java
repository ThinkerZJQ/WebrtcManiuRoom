package com.example.webrtcmaniuroom;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.function.IntUnaryOperator;

public class EncoderPlayerLiveH264 {

    private SocketLive socketLive;

    //采用硬编码
    private MediaCodec mediaCodec;

    private int width;
    private int height;

    private byte[] yuv;

    public EncoderPlayerLiveH264(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void startCapture(SocketLive live) {
        socketLive = live;
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC); //h264类型
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, height, width);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1080 * 1920);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //帧数
    private long frameIndex;

    //编码每一帧的数据，然后发送给网络层
    public void encodeFrame(byte[] input) {
        //数据是nv21，且是横屏相机数据，需要旋转
        byte[] nv12 = YuvUtils.nv21ToNV12(input);
        if (yuv == null) {
            yuv = new byte[nv12.length];
        }
        YuvUtils.portrailData2Raw(nv12, yuv, width, height);
        //数据从cpu进入dsp进行编码, 通过buffer

        //获取可用的buffer，超时30毫秒
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(30000);
        if (inputBufferIndex >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            byteBuffer.put(yuv);
            //pds = 当前帧对应的时间戳, 帧率 *帧index
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, computerPresentationTimeUs(frameIndex), 0);
            frameIndex++;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 30000);
        if (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            //把outerBuffer中的数据放到ba里，数据从dsp进入cpu
            byte[] ba = new byte[bufferInfo.size];
            //把编码后的数据写入文件，可以播放
            //  outputBuffer.get(ba);
            // YuvUtils.writeBytes(ba);

            dealFrame(outputBuffer, bufferInfo);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
        }
    }

    private byte[] configBuf;

    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        int type = bb.get(4);
        if (type == 0x67) {
            configBuf = new byte[bufferInfo.size];
            bb.get(configBuf);
        } else if (type == 0x65) {
            //配置帧和当前i帧一起发出去
            final byte[] Ibytes = new byte[bufferInfo.size];
            bb.get(Ibytes);

            byte[] newBuf = new byte[configBuf.length + Ibytes.length];
            System.arraycopy(configBuf, 0, newBuf, 0, configBuf.length);
            System.arraycopy(Ibytes, 0, newBuf, configBuf.length, Ibytes.length);
            //网络层发出去
            socketLive.sendData(newBuf);
        }else {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            socketLive.sendData(bytes);
        }
    }

    //pds = 当前帧对应的时间戳, 帧率 *帧index
    private long computerPresentationTimeUs(long frameIndex) {
        return frameIndex * 1000000 / 15;
    }
}
