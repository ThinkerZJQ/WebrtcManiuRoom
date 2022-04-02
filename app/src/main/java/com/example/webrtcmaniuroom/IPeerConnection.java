package com.example.webrtcmaniuroom;

public interface IPeerConnection {
    //房间里面有人加入
    void newConnection(String remoteIp);
    //分发数据
    void remoteReceiveData(String remoteIp, byte[] data);
}
