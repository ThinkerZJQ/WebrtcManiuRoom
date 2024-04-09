package com.example.normalwin;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity implements IPeerConnection {
    LocalSurfaceView mLocalSurfaceView;
    List<Surface> surfaceList = new ArrayList<>();
    SurfaceView first;
    SurfaceView second;

    SocketLive mSocketLive;
    List<SurfaceView> surfaceViews;
    private List<DecodecPlayerLiveH264> decoderList = new ArrayList<>();

    @Override protected void onDestroy() {
        super.onDestroy();
        mSocketLive.close();
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        setAllPermission();
        initView();
        Button text2 = findViewById(R.id.text2);
        mLocalSurfaceView = findViewById(R.id.localSurfaceView);
        text2.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mLocalSurfaceView.startCapture(mSocketLive);
            }
        });

    }

    private void initView() {
        first = findViewById(R.id.first_join);
        second = findViewById(R.id.second_join);
        surfaceViews = new ArrayList<>();
        surfaceViews.add(first);
        surfaceViews.add(second);
        for (SurfaceView view : surfaceViews) {
            view.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    surfaceList.add(holder.getSurface());
                }

                @Override public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

                }

                @Override public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

                }
            });
        }
        mSocketLive = new SocketLive(this);
        mSocketLive.start(this);
    }

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    private void setAllPermission() {
        checkStoragePermission();
        requestStoragePermission();
    }

    // 检查权限
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                normal();
            } else {
                Log.e("testTag", "checkStoragePermission");
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_STORAGE_PERMISSION);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_GRANTED) {
                normal();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION);
            }
        } else {
            normal();
        }
    }

    // 请求权限
    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，执行写文件的操作
                Log.e("testTag", "writeFile");

                normal();
            } else {
                Log.e("testTag", "deny");

                // 权限被拒绝，可以根据需要显示一个提示或执行其他操作
            }
        }
    }

    private void normal() {

    }

    private int surfaceIndex = 0;

    @Override public void newConnection(String remoteIp) {
        Log.e("testTag", "newConnection: remoteIp= " + remoteIp);
        Log.e("testTag", "newConnection: surfaceIndex= " + surfaceIndex);
        Log.e("testTag", "newConnection: surfaces.size()= " + surfaceList.size());
                    Observable.just(Unit.INSTANCE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {
                        Log.e("testTag","remoteReceiveData-start");
                        mLocalSurfaceView.startCapture(mSocketLive);
                    });
        if (surfaceIndex < surfaceList.size()) {
            DecodecPlayerLiveH264 decodecPlayerLiveH264 = new DecodecPlayerLiveH264();
            decodecPlayerLiveH264.initDecoder(remoteIp, surfaceList.get(surfaceIndex++));
            decoderList.add(decodecPlayerLiveH264);
        }
    }

    @Override public void remoteReceiveData(String remoteIp, byte[] data) {

        DecodecPlayerLiveH264 decodecPlayerLiveH264 = findDecodec(remoteIp);
        if (decodecPlayerLiveH264 != null) {
            decodecPlayerLiveH264.drawSurface(data);
        }
    }

    private DecodecPlayerLiveH264 findDecodec(String remoteIp) {
        for (DecodecPlayerLiveH264 decodecPlayerLiveH264 : decoderList) {
            if (decodecPlayerLiveH264.getRemoteIp().equals(remoteIp)) {
                return decodecPlayerLiveH264;
            }
        }
        return null;
    }
}
