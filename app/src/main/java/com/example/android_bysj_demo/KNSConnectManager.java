package com.example.android_bysj_demo;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.android_bysj_demo.shamir.ShamirSm4KeyReconstructor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket连接管理器（单例模式）
 * 用于在整个应用中共享WebSocket连接，避免在每个Activity中重复创建
 */
public class KNSConnectManager {
    private static final String TAG = "KNSConnectManager";

    // 服务器配置
    private static final String WEBSOCKET_SERVER_URL = "ws://52.80.120.124:3006?userId=";  // 本地服务器地址

    // 单例实例
    private static volatile KNSConnectManager instance;

    private WebSocketManager wsManager;

    // 使用Map存储分片，key为shareIndex，value为share内容
    private Map<Integer, String> shamirSharesMap = new HashMap<>();

    // 密钥恢复监听器
    private KeyRecoveryListener keyRecoveryListener;

    private MainActivity mainActivity;

    // 当前密钥版本号（初始为0，每次恢复后+1）
    private int currentKeyVersion = 0;

    public void setContext(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    /**
     * 密钥恢复监听器接口
     */
    public interface KeyRecoveryListener {
        /**
         * 密钥恢复成功回调
         * @param recoveredKey 恢复后的SM4密钥（16字节）
         * @param version 密钥版本号
         */
        void onKeyRecovered(byte[] recoveredKey, int version);

    }

    /**
     * 设置密钥恢复监听器
     */
    public void setKeyRecoveryListener(KeyRecoveryListener listener) {
        this.keyRecoveryListener = listener;
    }

    /**
     * 获取当前密钥版本号
     */
    public int getCurrentKeyVersion() {
        return currentKeyVersion;
    }

    /**
     * 断开 WebSocket 连接
     */
    public void disconnect() {
        if (wsManager != null) {
            wsManager.disconnect();
            wsManager = null;
            Log.i(TAG, "WebSocket connection disconnected");
        }
    }

    /**
     * 检查 WebSocket 是否已连接
     * @return true 表示已连接，false 表示未连接
     */
    public boolean isConnected() {
        return wsManager != null && wsManager.isConnected();
    }

    // 私有构造方法
    private KNSConnectManager() {
//        establishWebSocketConnection();
    }
    
    /**
     * 获取单例实例（双重检查锁定）
     * @return KNSConnectManager实例
     */
    public static KNSConnectManager getInstance() {
        if (instance == null) {
            synchronized (KNSConnectManager.class) {
                if (instance == null) {
                    instance = new KNSConnectManager();
                }
            }
        }
        return instance;
    }

    public void establishWebSocketConnection(String roomId, String userId) {
        try {
            // 使用标准 WebSocket 连接
            wsManager = new WebSocketManager(new WebSocketManager.WebSocketEventListener() {
                @Override
                public void onConnected() {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Log.d(TAG, "WebSocket connected successfully");
                        // 步骤2：WebSocket连接成功后，发送 create_room 事件创建房间
//                        sendCreateRoomEvent();
                        wsManager.createRoom(roomId, "room" + roomId);
                    });
                }

                @Override
                public void onDisconnected(int code, String reason) {
                    new Handler(Looper.getMainLooper()).post(() -> {
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
                        Log.e(TAG, "WebSocket error: " + error);
                    });
                }
            });

            // 开始连接
            Log.d(TAG, "Attempting to connect to: " + WEBSOCKET_SERVER_URL);
            wsManager.connect(WEBSOCKET_SERVER_URL + userId);

        } catch (Exception e) {
            Log.e(TAG, "WebSocket initialization error", e);
        }
    }

    /**
     * 处理服务器消息
     */
    public void handleServerMessage(String message) {
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
                case "room_share_first":
                    //第一个分片来临
                    handleShareFirst(json);
                    break;

                case "additional_shares_provided":
                    handleAdditionalSharesProvided(json);
                    break;

                case "room_shares_plain":
                    handleRoomSharesPlain(json);
                    break;

                case "user_left":
                    // 处理成员退出，进入密钥恢复界面
                    handleUserLeft(json);
                    break;
                // 其他消息类型处理...
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleAdditionalSharesProvided(JSONObject json) {
        try {
            String roomId = json.optString("roomId", "");
            String message = json.optString("message", "");
            
            Log.i(TAG, "[Shamir] 收到额外分片，房间ID: " + roomId + ", 消息: " + message);
            
            // 解析shares数组
            JSONArray sharesArray = json.getJSONArray("shares");
            for (int i = 0; i < sharesArray.length(); i++) {
                JSONObject shareObj = sharesArray.getJSONObject(i);
                int index = shareObj.getInt("index");
                String share = shareObj.getString("share");
                String sm3Hash = shareObj.optString("sm3Hash", "");
                
                // 存储分片到Map中
                shamirSharesMap.put(index, share);
                Log.i(TAG, "[Shamir] 收到分片[" + index + "]: " + share);
                Log.i(TAG, "[Shamir] SM3哈希: " + sm3Hash);
            }
            
            // 检查已收集的分片数量
            int collectedCount = shamirSharesMap.size();
            Log.i(TAG, "[Shamir] 已收集 " + collectedCount + " 个分片");
            
            // 如果收集到的分片数 >= 3，进行密钥恢复
            if (collectedCount >= 3) {
                Log.i(TAG, "[Shamir] 分片数量充足，开始密钥恢复...");
                
                // 构造分片列表用于恢复
                List<RoomInfo.ShamirShare> sharesForRecovery = new ArrayList<>();
                for (Map.Entry<Integer, String> entry : shamirSharesMap.entrySet()) {
                    RoomInfo.ShamirShare shamirShare = new RoomInfo.ShamirShare();
                    shamirShare.index = entry.getKey();
                    shamirShare.share = entry.getValue();
                    sharesForRecovery.add(shamirShare);
                }
                
                // 按index排序
                sharesForRecovery.sort((a, b) -> Integer.compare(a.index, b.index));
                
                // 只使用前3条分片进行恢复
                List<RoomInfo.ShamirShare> firstThreeShares = sharesForRecovery.subList(0, Math.min(3, sharesForRecovery.size()));
                
                Log.i(TAG, "[Shamir] 使用 " + firstThreeShares.size() + " 条分片进行密钥恢复");
                for (RoomInfo.ShamirShare share : firstThreeShares) {
                    Log.i(TAG, "[Shamir] Share[" + share.index + "]: " + share.share);
                }
                
                // 恢复SM4密钥
                String recoveredSm4Key = ShamirSm4KeyReconstructor.reconstructSm4Key(firstThreeShares);
                Log.i(TAG, "[Shamir] 恢复后的SM4密钥: " + recoveredSm4Key);
                
                // 转换为字节数组并通知监听器
                if (recoveredSm4Key != null) {
                    byte[] keyBytes = hexStringToBytes(recoveredSm4Key);
                    if (keyBytes != null && keyBytes.length == 16) {
                        notifyKeyRecovered(keyBytes);
                    } else {
                        Log.e(TAG, "[Shamir] 密钥转换失败，长度不正确");
                    }
                }
            } else {
                Log.w(TAG, "[Shamir] 分片数量不足，需要至少3个分片才能恢复密钥，当前: " + collectedCount);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "[Shamir] 解析额外分片数据失败", e);
        }
    }

    private void handleRoomSharesPlain(JSONObject json) {
        try {
            String roomId = json.optString("roomId", "");
            String roomName = json.optString("roomName", "");
            String message = json.optString("message", "");
            int totalShares = json.optInt("totalShares", 5);
            int threshold = json.optInt("threshold", 3);
            
            Log.i(TAG, "[Shamir] 收到明文分片，房间ID: " + roomId + ", 房间名: " + roomName);
            Log.i(TAG, "[Shamir] 消息: " + message);
            
            // 解析shares数组
            JSONArray sharesArray = json.getJSONArray("shares");
            for (int i = 0; i < sharesArray.length(); i++) {
                JSONObject shareObj = sharesArray.getJSONObject(i);
                int index = shareObj.getInt("index");
                String share = shareObj.getString("share");
                String sm3Hash = shareObj.optString("sm3Hash", "");
                
                // 存储分片到Map中
                shamirSharesMap.put(index, share);
                Log.i(TAG, "[Shamir] 收到明文分片[" + index + "]: " + share);
                Log.i(TAG, "[Shamir] SM3哈希: " + sm3Hash);
            }
            
            // 检查已收集的分片数量
            int collectedCount = shamirSharesMap.size();
            Log.i(TAG, "[Shamir] 已收集 " + collectedCount + " 个分片");
            
            // 如果收集到的分片数 >= 3，进行密钥恢复
            if (collectedCount >= threshold) {
                Log.i(TAG, "[Shamir] 分片数量充足，开始密钥恢复...");
                
                // 构造分片列表用于恢复
                List<RoomInfo.ShamirShare> sharesForRecovery = new ArrayList<>();
                for (Map.Entry<Integer, String> entry : shamirSharesMap.entrySet()) {
                    RoomInfo.ShamirShare shamirShare = new RoomInfo.ShamirShare();
                    shamirShare.index = entry.getKey();
                    shamirShare.share = entry.getValue();
                    sharesForRecovery.add(shamirShare);
                }
                
                // 按index排序
                sharesForRecovery.sort((a, b) -> Integer.compare(a.index, b.index));
                
                // 只使用前3条分片进行恢复
                List<RoomInfo.ShamirShare> firstThreeShares = sharesForRecovery.subList(0, Math.min(threshold, sharesForRecovery.size()));
                
                Log.i(TAG, "[Shamir] 使用 " + firstThreeShares.size() + " 条分片进行密钥恢复");
                for (RoomInfo.ShamirShare share : firstThreeShares) {
                    Log.i(TAG, "[Shamir] Share[" + share.index + "]: " + share.share);
                }
                
                // 恢复SM4密钥
                String recoveredSm4Key = ShamirSm4KeyReconstructor.reconstructSm4Key(firstThreeShares);
                Log.i(TAG, "[Shamir] 恢复后的SM4密钥: " + recoveredSm4Key);
                
                // 转换为字节数组并通知监听器
                if (recoveredSm4Key != null) {
                    byte[] keyBytes = hexStringToBytes(recoveredSm4Key);
                    if (keyBytes != null && keyBytes.length == 16) {
                        notifyKeyRecovered(keyBytes);
                    } else {
                        Log.e(TAG, "[Shamir] 密钥转换失败，长度不正确");
                    }
                }
            } else {
                Log.w(TAG, "[Shamir] 分片数量不足，需要至少" + threshold + "个分片才能恢复密钥，当前: " + collectedCount);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "[Shamir] 解析明文分片数据失败", e);
        }
    }

    private void handleShareFirst(JSONObject json) {
        try {
            String roomId = json.optString("roomId", "");
            String roomName = json.optString("roomName", "");
            int totalShares = json.optInt("totalShares", 3);
            int threshold = json.optInt("threshold", 3);
            
            // 解析share对象
            JSONObject shareObj = json.getJSONObject("share");
            int index = shareObj.getInt("index");
            String share = shareObj.getString("share");
            String sm3Hash = shareObj.optString("sm3Hash", "");
            
            // 存储分片到Map中
            shamirSharesMap.put(index, share);
            Log.i(TAG, "[Shamir] 收到第一个分片[" + index + "]: " + share);
            Log.i(TAG, "[Shamir] SM3哈希: " + sm3Hash);
            Log.i(TAG, "[Shamir] 房间ID: " + roomId + ", 房间名: " + roomName);
            Log.i(TAG, "[Shamir] 总分片数: " + totalShares + ", 阈值: " + threshold);
            
            // 检查已收集的分片数量
            int collectedCount = shamirSharesMap.size();
            Log.i(TAG, "[Shamir] 已收集 " + collectedCount + "/" + totalShares + " 个分片");
            
            // 检查是否达到阈值
            if (collectedCount >= threshold) {
                Log.i(TAG, "[Shamir] 已收集足够的分片，可以进行密钥恢复");
            }

            mainActivity.enterVideoChat();


        } catch (JSONException e) {
            Log.e(TAG, "[Shamir] 解析分片数据失败", e);
        }
    }

    public void handleRoomMessage(JSONObject json) {
    }

    public void handleRoomList(JSONObject json) {
    }

    public void handleWelcomeMessage(JSONObject json) {
    }

    /**
     * 处理创建房间响应
     */
    public void handleCreateRoomResponse(JSONObject response) throws JSONException {
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
        Log.d(TAG, "Room created successfully: " + resultInfo);

    }

    /**
     * 处理成员退出事件
     */
    public void handleUserLeft(JSONObject json) {
        try {
            String leftUserId = json.optString("userId", "");
            String userName = json.optString("userName", "未知用户");

            Log.d(TAG, "User left: " + userName + " (" + leftUserId + ")");

//            // 跳转到密钥恢复界面
//            Intent intent = new Intent(this, KeyRecoveryActivity.class);
//            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error handling user_left event", e);
        }
    }

    public void handlerRoomCreated(JSONObject json) {
        Log.d(TAG, "Room created: " + json.toString());
    }

    public void handlerRoomJoined(JSONObject json) {
        Log.d(TAG, "Room joined: " + json.toString());
//        try {
//            // 使用 RoomInfo JavaBean 解析
////            RoomInfo roomInfo = RoomInfo.fromJson(json);
////            RoomInfo.RoomData room = roomInfo.room;
////            Log.d(TAG, room.toString());
////
////            // 显示原始SM4密钥
////            String originSm4Key = room.sm4Key;
////
////            // SM3哈希校验（模拟）
////            Log.i(TAG, "[SM3] 开始哈希校验...");
////            Log.i(TAG, "[SM3] 原始数据: " + originSm4Key);
////            Log.i(TAG, "[SM3] 计算哈希值: ");
////            Log.i(TAG, "[SM3] 哈希校验成功 ✓");
////
////            // 只使用前3条分片数据进行恢复（threshold=3）
////            List<RoomInfo.ShamirShare> allShares = roomInfo.room.shamirShares;
////            List<RoomInfo.ShamirShare> sharesForRecovery = allShares.subList(0, Math.min(3, allShares.size()));
////
////            Log.i(TAG, "[Shamir] 使用 " + sharesForRecovery.size() + " 条分片进行密钥恢复");
////            for (RoomInfo.ShamirShare share : sharesForRecovery) {
////                Log.i(TAG, "[Shamir] Share[" + share.index + "]: " + share.share);
////            }
////
////            // 恢复SM4密钥
////            String recoveredSm4Key = ShamirSm4KeyReconstructor.reconstructSm4Key(sharesForRecovery);
////            Log.i(TAG, "[Shamir] 恢复后的SM4密钥: " + recoveredSm4Key);
//
//        } catch (JSONException e) {
//            Log.e(TAG, "Error parsing room_joined message", e);
//        }
    }

    /**
     * 通知监听器密钥已恢复
     */
    private void notifyKeyRecovered(byte[] keyBytes) {
        currentKeyVersion++;
        Log.i(TAG, "[Shamir] 密钥恢复成功，新版本号: " + currentKeyVersion);
        
        if (keyRecoveryListener != null) {
            keyRecoveryListener.onKeyRecovered(keyBytes, currentKeyVersion);
        } else {
            Log.w(TAG, "[Shamir] 未设置 KeyRecoveryListener，无法通知密钥更新");
        }
    }

    /**
     * 十六进制字符串转字节数组
     * @param hexString 十六进制字符串（如 "0123456789ABCDEF"）
     * @return 字节数组，失败返回 null
     */
    private byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.length() != 32) {
            Log.e(TAG, "无效的十六进制密钥字符串，长度应为32，实际: " + 
                  (hexString != null ? hexString.length() : "null"));
            return null;
        }
        
        try {
            byte[] bytes = new byte[16];
            for (int i = 0; i < 16; i++) {
                int index = i * 2;
                String hexPair = hexString.substring(index, index + 2);
                bytes[i] = (byte) Integer.parseInt(hexPair, 16);
            }
            return bytes;
        } catch (Exception e) {
            Log.e(TAG, "十六进制字符串转换失败: " + e.getMessage());
            return null;
        }
    }
}
