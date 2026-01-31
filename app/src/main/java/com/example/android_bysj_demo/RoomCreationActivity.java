package com.example.android_bysj_demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Base64;
import android.widget.LinearLayout;

import com.example.android_bysj_demo.shamir.ShamirSm4KeyReconstructor;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 房间创建Activity
 * 包含创建房间功能，并保持密钥分片功能的完整性
 */
public class RoomCreationActivity extends AppCompatActivity {
    private static final String TAG = "RoomCreationActivity";

    // 服务器配置
    private static final String WEBSOCKET_SERVER_URL = "ws://192.168.2.191:3000";  // 本地服务器地址

    // UI组件
    private EditText etUserId;
    private EditText etRoomId;
    private Button btnCreateRoom;
    private Button btnRecoverKey;
    private Button btnEnterRoom;
    private TextView tvSm4Key;
    private TextView tvLocalShare;
    private LinearLayout llRemoteSharesContainer;
    private TextView tvRecoveredKeyInfo;

    // 动态创建的TextView列表
    private List<TextView> remoteShareTextViews;

    // 数据
    private String userId;
    private String roomId;
    private WebSocketManager wsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_creation);

        // 获取Intent传递的数据
        Intent intent = getIntent();
        userId = intent.getStringExtra("USER_ID");
        roomId = intent.getStringExtra("ROOM_ID");

        remoteShareTextViews = new ArrayList<>();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etUserId = findViewById(R.id.et_user_id);
        etRoomId = findViewById(R.id.et_room_id);
        btnCreateRoom = findViewById(R.id.btn_create_room);
        btnRecoverKey = findViewById(R.id.btn_recover_key);
        btnEnterRoom = findViewById(R.id.btn_enter_room);
        tvSm4Key = findViewById(R.id.tv_sm4_key);
        tvLocalShare = findViewById(R.id.tv_local_share);
        llRemoteSharesContainer = findViewById(R.id.ll_remote_shares_container);
        tvRecoveredKeyInfo = findViewById(R.id.tv_recovered_key_info);

        // 显示用户信息
        etUserId.setText(userId);
        etRoomId.setText(roomId);
    }

    private void setupClickListeners() {
        // 创建房间
        btnCreateRoom.setOnClickListener(v -> createRoom());

        // 恢复密钥
        btnRecoverKey.setOnClickListener(v -> recoverKey());

        // 进入房间
        btnEnterRoom.setOnClickListener(v -> enterVideoChat());
    }

    /**
     * Share数据类
     */
    private static class ShareData {
        int index;
        String share;

        ShareData(int index, String share) {
            this.index = index;
            this.share = share;
        }
    }

    /**
     * 创建房间
     * 步骤1：建立WebSocket连接
     * 步骤2：调用HTTP API创建房间
     */
    private void createRoom() {
        // 获取当前输入框中的数据
        String currentUserId = etUserId.getText().toString().trim();
        String currentRoomId = etRoomId.getText().toString().trim();

        // 简单校验
        if (currentUserId.isEmpty() || currentRoomId.isEmpty()) {
            Toast.makeText(this, "用户ID和房间号不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新内部数据
        userId = currentUserId;
        roomId = currentRoomId;

        // 步骤1：建立WebSocket连接
        establishWebSocketConnection();
    }

    /**
     * 建立WebSocket连接
     */
    private void establishWebSocketConnection() {
        try {
            // 使用标准 WebSocket 连接
            wsManager = new WebSocketManager(new WebSocketManager.WebSocketEventListener() {
                @Override
                public void onConnected() {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(RoomCreationActivity.this, "WebSocket连接成功", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "WebSocket connected successfully");
                        // 步骤2：WebSocket连接成功后，发送 create_room 事件创建房间
//                        sendCreateRoomEvent();
                        wsManager.createRoom("1", "room_1");
                    });
                }

                @Override
                public void onDisconnected(int code, String reason) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(RoomCreationActivity.this,
                                "WebSocket断开: " + reason + " (" + code + ")",
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "WebSocket disconnected: " + code + ", " + reason);
                    });
                }

                @Override
                public void onMessage(String message) {
                    // 处理服务器消息，切换到主线程更新UI
                    Log.d(TAG, "Received message: " + message);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        handleServerMessage(message);
                    });
                }

                @Override
                public void onError(String error) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(RoomCreationActivity.this,
                                "WebSocket连接失败: " + error,
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, "WebSocket error: " + error);
                    });
                }
            });

            // 开始连接
            Log.d(TAG, "Attempting to connect to: " + WEBSOCKET_SERVER_URL);
            wsManager.connect(WEBSOCKET_SERVER_URL);
            Toast.makeText(this, "正在连接WebSocket服务器...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "WebSocket初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "WebSocket initialization error", e);
        }
    }

    /**
     * 通过 WebSocket 发送 create_room 消息创建房间
     * 服务器端处理：handleCreateRoom(userId, data)
     */
    private void sendCreateRoomEvent() {
        try {
            // 构建请求数据
            JSONObject roomData = new JSONObject();
            roomData.put("roomId", roomId);
            roomData.put("roomName", "Room_" + roomId);  // 使用roomId作为房间名的一部分

            Log.d(TAG, "Sending create_room message: " + roomData.toString());

            // 通过标准 WebSocket 发送消息
            wsManager.sendJson(roomData);

        } catch (JSONException e) {
            Toast.makeText(this, "构建请求数据失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Build request data failed", e);
        }
    }

    /**
     * 处理服务器消息
     */
    private void handleServerMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            switch (type) {
                case "welcome":
                    // 处理欢迎消息和服务器公钥
                    handleWelcomeMessage(json);
                    break;
                case "room_list":
                    // 处理房间列表
                    handleRoomList(json);
                    break;

                case "room_joined":
                    handlerRoomJoined(json);
                    break;

                case "room_created":
                    handlerRoomCreated(json);
                    break;

                case "room_message":
                    // 处理房间消息
                    handleRoomMessage(json);
                    break;
                // 其他消息类型处理...
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handlerRoomCreated(JSONObject json) {
        Log.d(TAG, "Room created: " + json.toString());
    }

    private void handlerRoomJoined(JSONObject json) {
        Log.d(TAG, "Room joined: " + json.toString());
        try {
            // 使用 RoomInfo JavaBean 解析
            RoomInfo roomInfo = RoomInfo.fromJson(json);
            RoomInfo.RoomData room = roomInfo.room;
            Log.d(TAG, room.toString());

            // 解析SM4密钥
            String originSm4Key = room.sm4Key;
            tvSm4Key.setText(originSm4Key);
            String sm4Key = ShamirSm4KeyReconstructor.reconstructSm4Key(roomInfo.room.shamirShares);
            tvRecoveredKeyInfo.setText(sm4Key);


        } catch (JSONException e) {
            Log.e(TAG, "Error parsing room_joined message", e);
            Toast.makeText(this, "解析房间数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 动态创建他人分片的TextView
     * @param index 分片索引
     * @param share 分片内容
     */
    private void createRemoteShareTextView(int index, String share) {
        TextView textView = new TextView(this);
        textView.setText("Share[" + index + "]: " + share);
        textView.setTextColor(0xFF333333);
        textView.setTextSize(13);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        textView.setBackgroundColor(0xFFFFF3E0);
        textView.setPadding(10, 10, 10, 10);

        // 添加边距
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 0);
        textView.setLayoutParams(params);

        // 添加到容器
        llRemoteSharesContainer.addView(textView);
        remoteShareTextViews.add(textView);
    }

    /**
     * 清空所有动态创建的他人分片TextView
     */
    private void clearRemoteShareTextViews() {
        for (TextView textView : remoteShareTextViews) {
            llRemoteSharesContainer.removeView(textView);
        }
        remoteShareTextViews.clear();
    }

    private static final int THRESHOLD = 3; // 恢复阈值

    /**
     * 恢复密钥
     */
    private void recoverKey() {

    }


    /**
     * 将十六进制字符串转换为字节数组
     */
    private byte[] hexStringToByteArray(String hexString) {
        if (hexString == null || hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            int value = Integer.parseInt(hexString.substring(i, i + 2), 16);
            bytes[i / 2] = (byte) value;
        }
        return bytes;
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 将ID数组转换为字符串
     */
    private String idsToString(Integer[] ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.length; i++) {
            sb.append(ids[i]);
            if (i < ids.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private void handleRoomMessage(JSONObject json) {
    }

    private void handleRoomList(JSONObject json) {
    }

    private void handleWelcomeMessage(JSONObject json) {
    }

    /**
     * 处理创建房间响应
     */
    private void handleCreateRoomResponse(JSONObject response) throws JSONException {
        String createdRoomId = response.optString("roomId", "");
        String createdRoomName = response.optString("roomName", "");
        String message = response.optString("message", "房间创建成功");

        // 显示创建成功信息
        String resultInfo = String.format(
                "=== 房间创建成功 ===\n" +
                "房间ID: %s\n" +
                "房间名称: %s\n" +
                "消息: %s",
                createdRoomId, createdRoomName, message
        );
        tvRecoveredKeyInfo.setText(resultInfo);
        Log.d(TAG, "Room created successfully: " + resultInfo);

        Toast.makeText(this, "房间创建成功!", Toast.LENGTH_SHORT).show();
    }

    /**
     * 进入视频聊天房间
     */
    private void enterVideoChat() {
        // 获取当前输入框中的用户ID和房间ID
        String currentUserId = etUserId.getText().toString().trim();
        String currentRoomId = etRoomId.getText().toString().trim();

        // 简单校验
        if (currentUserId.isEmpty() || currentRoomId.isEmpty()) {
            Toast.makeText(this, "用户ID和房间号不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 跳转到VideoChatActivity，传递相同的数据
        Intent intent = new Intent(RoomCreationActivity.this, VideoChatActivity.class);
        intent.putExtra("USER_ID", currentUserId);
        intent.putExtra("ROOM_ID", currentRoomId);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 断开WebSocket连接
        if (wsManager != null) {
            wsManager.disconnect();
        }
    }
}
