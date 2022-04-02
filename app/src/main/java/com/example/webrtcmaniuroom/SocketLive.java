package com.example.webrtcmaniuroom;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SocketLive {

    //模拟客户端三个ip
    private String[] urls = {"ws://172.28.119.139:", "ws://172.28.121.112:", "ws://192.168.18.53:"};
    private int port = 1081;

    private ManiuSocketServer maniuSocketServer;

    private List<MyWebSocketClient> socketClientList = new ArrayList<>();

    private Timer timer;

    public SocketLive(IPeerConnection connection) {
        maniuSocketServer = new ManiuSocketServer(connection);
        maniuSocketServer.start();
    }

    public void start(final Context context) {
        for (String value : urls) {
            if (value.contains(getLocalIpAddress(context))) {
                continue;
            }
            URI url = null;
            try {
                url = new URI(value + port);
                MyWebSocketClient myWebSocketClient = new MyWebSocketClient(value, url);
                myWebSocketClient.connect();
                if (myWebSocketClient.isOpen()) {
                    socketClientList.add(myWebSocketClient);
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (String value : urls) {
                    if (value.contains(getLocalIpAddress(context))) {
                        continue;
                    }
                    boolean isSame = false;
                    for (MyWebSocketClient socketClient : socketClientList) {
                        if (socketClient.getURI().toString().contains(value)) {
                            isSame = true;
                            break;
                        }
                    }
                    if (isSame) {
                        continue;
                    }

                    URI url = null;
                    try {
                        url = new URI(value + port);
                        MyWebSocketClient webSocketClient = new MyWebSocketClient(value, url);
                        webSocketClient.connect();
                        if (webSocketClient.isOpen()) {
                            socketClientList.add(webSocketClient);
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 3 * 1000, 5 * 1000);
    }

    public static String getLocalIpAddress(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            String ip = int2ip(i);
            return ip;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    class MyWebSocketClient extends WebSocketClient {
        String url;

        public MyWebSocketClient(String url, URI serverUri) {
            super(serverUri);
            this.url = url;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            socketClientList.add(this);
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
            Log.d("WDY","onOpen");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            Log.d("WDY","onClose");
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            Log.d("WDY", "onMessage = " + message);
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
            Log.d("WDY","onError");
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
