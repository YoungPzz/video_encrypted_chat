package com.example.android_bysj_demo;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * 标准 WebSocket 管理类
 * 使用 OkHttp 实现标准 WebSocket 连接
 */
public class WebSocketManager {
    private static final String TAG = "WebSocketManager";

    private OkHttpClient client;
    private WebSocket webSocket;
    private WebSocketEventListener listener;
    private String serverUrl;

    public interface WebSocketEventListener {
        void onConnected();
        void onDisconnected(int code, String reason);
        void onMessage(String message);
        void onError(String error);
    }

    public WebSocketManager(WebSocketEventListener listener) {
        this.listener = listener;
        this.client = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 连接到 WebSocket 服务器
     * @param url 服务器地址，如 ws://192.168.19.1:3000
     */
    public void connect(String url) {
        this.serverUrl = url;

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(okhttp3.WebSocket webSocket, Response response) {
                    Log.d(TAG, "WebSocket connected to: " + url);
                    if (listener != null) {
                        listener.onConnected();
                    }
                }

                @Override
                public void onMessage(okhttp3.WebSocket webSocket, String text) {
                    Log.d(TAG, "Received message: " + text);
                    if (listener != null) {
                        listener.onMessage(text);
                    }
                }

                @Override
                public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
                    Log.d(TAG, "Received binary message");
                    // 处理二进制消息
                }

                @Override
                public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
                    Log.d(TAG, "WebSocket closing: " + code + ", " + reason);
                }

                @Override
                public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
                    Log.d(TAG, "WebSocket closed: " + code + ", " + reason);
                    if (listener != null) {
                        listener.onDisconnected(code, reason);
                    }
                }

                @Override
                public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
                    String errorMsg = t.getMessage();
                    Log.e(TAG, "WebSocket error: " + errorMsg, t);
                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                }
            });

            Log.d(TAG, "Connecting to WebSocket: " + url);

        } catch (Exception e) {
            Log.e(TAG, "WebSocket connection error", e);
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        }
    }

    /**
     * 发送文本消息
     * @param message 要发送的文本消息
     */
    public void send(String message) {
        if (webSocket != null) {
            boolean sent = webSocket.send(message);
            Log.d(TAG, "Sent message: " + message + ", success: " + sent);
        } else {
            Log.e(TAG, "WebSocket not connected, cannot send message");
        }
    }

    /**
     * 发送 JSON 消息
     * @param json 要发送的 JSON 对象
     */
    public void sendJson(JSONObject json) {
        send(json.toString());
    }

    public void joinRoom(String roomId, String username) {
        String message = "{" +
                "\"type\":\"join_room\"," +
                "\"roomId\":\"" + roomId + "\"," +
                "\"username\":\"" + username + "\"" +
                "}";
        send(message);
    }

    public void createRoom(String roomId, String roomName) {
        String message = "{" +
                "\"type\":\"create_room\"," +
                "\"roomId\":\"" + roomId + "\"," +
                "\"roomName\":\"" + roomName + "\"" +
                "}";
        send(message);
    }


    /**
     * 断开连接
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
            Log.d(TAG, "WebSocket disconnected");
        }
    }

    /**
     * 检查连接状态
     * @return 是否已连接
     */
    public boolean isConnected() {
        return webSocket != null;
    }
}
