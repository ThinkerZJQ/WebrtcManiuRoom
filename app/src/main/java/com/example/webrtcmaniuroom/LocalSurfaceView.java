package com.example.webrtcmaniuroom;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.IOException;

public class LocalSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Camera.Size size;
    private Camera camera;
    private byte[] buffer;

    private EncoderPlayerLiveH264 encoderPlayerLiveH264;

    public void startCapture(SocketLive live) {
        encoderPlayerLiveH264 = new EncoderPlayerLiveH264(size.width, size.height);
        encoderPlayerLiveH264.startCapture(live);
    }

    public LocalSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    private void startPreview() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = camera.getParameters();
        size = parameters.getPreviewSize();
        try {
            camera.setPreviewDisplay(getHolder());
            camera.setDisplayOrientation(90);
            buffer = new byte[size.width * size.height * 3 / 2];
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);

            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //摄像头捕获的数据,进行编码
        if (encoderPlayerLiveH264 != null) {
            encoderPlayerLiveH264.encodeFrame(data);
        }
        camera.addCallbackBuffer(data);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }
}
