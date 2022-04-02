package com.example.webrtcmaniuroom;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SocketLive {

    //模拟客户端三个ip
    private String[] urls = {"ws://192.168.18.51:", "ws://192.168.18.52:", "ws://192.168.18.53:"};
    private int port = 1081;

    private ManiuSocketServer maniuSocketServer;

    private List<MyWebSocketClient> socketClientList = new ArrayList<>();

    public SocketLive(IPeerConnection connection) {
        maniuSocketServer = new ManiuSocketServer(connection);
        maniuSocketServer.start();
    }

    class MyWebSocketClient extends WebSocketClient {


        public MyWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {

        }

        @Override
        public void onMessage(String message) {

        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

        }

        @Override
        public void onError(Exception ex) {

        }
    }

    //server端，接收别人编码好的数据流
    class ManiuSocketServer extends WebSocketServer {
        private IPeerConnection peerConnection;

        public ManiuSocketServer(IPeerConnection peerConnection) {
            super(new InetSocketAddress(port));
            this.peerConnection = peerConnection;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            this.peerConnection.newConnection(conn.getRemoteSocketAddress().getAddress().getHostAddress());
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        }

        @Override
        public void onMessage(WebSocket conn, String message) {

        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            super.onMessage(conn, message);
            //接收其他人的数据
            byte[] buf = new byte[message.remaining()];
            message.get(buf);
            this.peerConnection.remoteReceiveData(conn.getRemoteSocketAddress().getAddress().getHostAddress(), buf);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {

        }

        @Override
        public void onStart() {

        }
    }

    public void sendData(byte[] data) {
        for (MyWebSocketClient myWebSocketClient : socketClientList) {
            if (myWebSocketClient.isOpen()) {
                myWebSocketClient.send(data);
            }
        }
    }
}
