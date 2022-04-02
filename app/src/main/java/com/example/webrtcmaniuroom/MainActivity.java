package com.example.webrtcmaniuroom;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements IPeerConnection {

    public static Context context;
    LocalSurfaceView localSurfaceView;

    SurfaceView surfaceView1;
    SurfaceView surfaceView2;
    SurfaceView surfaceView3;
    ArrayList<SurfaceView> surfaceViews;
    ArrayList<Surface> surfaces;

    ArrayList<DecodecPlayerLiveH264> decodecPlayerLiveH264s = new ArrayList<>();
    SocketLive socketLive;


    @RequiresApi(Build.VERSION_CODES.M)

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        checkPermission();

        localSurfaceView = findViewById(R.id.localSurfaceView);
        findViewById(R.id.join_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localSurfaceView.startCapture(socketLive);
            }
        });

        surfaceView1 = findViewById(R.id.surface1);
        surfaceView2 = findViewById(R.id.surface2);
        surfaceView3 = findViewById(R.id.surface3);

        surfaceViews = new ArrayList<>();
        surfaceViews.add(surfaceView1);
        surfaceViews.add(surfaceView2);
        surfaceViews.add(surfaceView3);

        for (SurfaceView surfaceView : surfaceViews) {
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    surfaces.add(holder.getSurface());
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

                }
            });
        }

        socketLive = new SocketLive(this);
        socketLive.start(this);
    }


    @RequiresApi(Build.VERSION_CODES.M)
    boolean checkPermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA}, 1
            );
        }
        return false;
    }

    private int surfaceIndex;

    //有人进入会议室
    @Override
    public void newConnection(String remoteIp) {
        DecodecPlayerLiveH264 decodecPlayerLiveH264 = new DecodecPlayerLiveH264();
        decodecPlayerLiveH264.initDecoder(remoteIp, surfaces.get(surfaceIndex++));
        decodecPlayerLiveH264s.add(decodecPlayerLiveH264);
    }


    @Override
    public void remoteReceiveData(String remoteIp, byte[] data) {
        //ip ---surface
        //ip --decodec

        DecodecPlayerLiveH264 decodecPlayerLiveH264 = findDecodec(remoteIp);
        if(decodecPlayerLiveH264 !=null){
            decodecPlayerLiveH264.drawSurface(data);
        }
    }

    private DecodecPlayerLiveH264 findDecodec(String remoteIp) {
        for (DecodecPlayerLiveH264 decodecPlayerLiveH264 : decodecPlayerLiveH264s) {
            if (decodecPlayerLiveH264.getRemoteIp().equals(remoteIp)) {
                return decodecPlayerLiveH264;
            }
        }
        return null;
    }
}
