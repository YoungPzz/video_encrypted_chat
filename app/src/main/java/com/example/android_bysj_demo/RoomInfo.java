package com.example.android_bysj_demo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间信息 JavaBean
 * 用于解析和存储 room_joined 消息中的数据
 */
public class RoomInfo {

    // 基础字段
    public String type;
    public RoomData room;

    /**
     * 房间数据内部类
     */
    public static class RoomData {
        public String id;
        public String name;
        public List<UserInfo> users;
        public String createdAt;
        public int maxUsers;
        public boolean isPrivate;
        public String description;
        public List<MessageInfo> messages;
        public String sm4Key;
        public int keyVersion;
        public String keyCreatedAt;
        public List<ShamirShare> shamirShares;
        public int shareCount;
        public int threshold;
        public int userCount;

        @Override
        public String toString() {
            return "RoomData{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", users=" + users +
                    ", createdAt='" + createdAt + '\'' +
                    ", maxUsers=" + maxUsers +
                    ", isPrivate=" + isPrivate +
                    ", description='" + description + '\'' +
                    ", messages=" + messages +
                    ", sm4Key='" + sm4Key + '\'' +
                    ", keyVersion=" + keyVersion +
                    ", keyCreatedAt='" + keyCreatedAt + '\'' +
                    ", shamirShares=" + shamirShares +
                    ", shareCount=" + shareCount +
                    ", threshold=" + threshold +
                    ", userCount=" + userCount +
                    '}';
        }
    }

    /**
     * 用户信息
     */
    public static class UserInfo {
        public String userId;
        public String username;

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userId='" + userId + '\'' +
                    ", username='" + username + '\'' +
                    '}';
        }
    }

    /**
     * 消息信息
     */
    public static class MessageInfo {
        public String type;
        public String content;
        public String username;
        public String timestamp;
        public String id;

        @Override
        public String toString() {
            return "MessageInfo{" +
                    "type='" + type + '\'' +
                    ", content='" + content + '\'' +
                    ", username='" + username + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", id='" + id + '\'' +
                    '}';
        }
    }

    /**
     * Shamir分片信息
     */
    public static class ShamirShare {
        public int index;
        public String share;

        @Override
        public String toString() {
            return "ShamirShare{" +
                    "index=" + index +
                    ", share='" + share + '\'' +
                    '}';
        }
    }

    /**
     * 从JSON对象解析
     * @param json JSON字符串
     * @return RoomInfo对象
     * @throws JSONException 解析异常
     */
    public static RoomInfo fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    /**
     * 从JSONObject解析
     * @param jsonObject JSONObject对象
     * @return RoomInfo对象
     * @throws JSONException 解析异常
     */
    public static RoomInfo fromJson(JSONObject jsonObject) throws JSONException {
        RoomInfo roomInfo = new RoomInfo();
        roomInfo.type = jsonObject.getString("type");
        roomInfo.room = parseRoomData(jsonObject.getJSONObject("room"));
        return roomInfo;
    }

    /**
     * 解析房间数据
     */
    private static RoomData parseRoomData(JSONObject roomJson) throws JSONException {
        RoomData roomData = new RoomData();
        roomData.id = roomJson.getString("id");
        roomData.name = roomJson.getString("name");
        roomData.createdAt = roomJson.getString("createdAt");
        roomData.maxUsers = roomJson.getInt("maxUsers");
        roomData.isPrivate = roomJson.getBoolean("isPrivate");
        roomData.description = roomJson.optString("description", "");
        roomData.sm4Key = roomJson.getString("sm4Key");
        roomData.keyVersion = roomJson.getInt("keyVersion");
        roomData.keyCreatedAt = roomJson.getString("keyCreatedAt");
        roomData.shareCount = roomJson.getInt("shareCount");
        roomData.threshold = roomJson.getInt("threshold");
        roomData.userCount = roomJson.getInt("userCount");

        // 解析用户列表
        roomData.users = new ArrayList<>();
        JSONArray usersArray = roomJson.getJSONArray("users");
        for (int i = 0; i < usersArray.length(); i++) {
            UserInfo userInfo = parseUserInfo(usersArray.getJSONObject(i));
            roomData.users.add(userInfo);
        }

        // 解析消息列表
        roomData.messages = new ArrayList<>();
        JSONArray messagesArray = roomJson.getJSONArray("messages");
        for (int i = 0; i < messagesArray.length(); i++) {
            MessageInfo messageInfo = parseMessageInfo(messagesArray.getJSONObject(i));
            roomData.messages.add(messageInfo);
        }

        // 解析Shamir分片
        roomData.shamirShares = new ArrayList<>();
        JSONArray sharesArray = roomJson.getJSONArray("shamirShares");
        for (int i = 0; i < sharesArray.length(); i++) {
            ShamirShare share = parseShamirShare(sharesArray.getJSONObject(i));
            roomData.shamirShares.add(share);
        }

        return roomData;
    }

    /**
     * 解析用户信息
     */
    private static UserInfo parseUserInfo(JSONObject userJson) throws JSONException {
        UserInfo userInfo = new UserInfo();
        userInfo.userId = userJson.getString("userId");
        userInfo.username = userJson.getString("username");
        return userInfo;
    }

    /**
     * 解析消息信息
     */
    private static MessageInfo parseMessageInfo(JSONObject messageJson) throws JSONException {
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.type = messageJson.getString("type");
        messageInfo.content = messageJson.getString("content");
        messageInfo.username = messageJson.getString("username");
        messageInfo.timestamp = messageJson.getString("timestamp");
        messageInfo.id = messageJson.getString("id");
        return messageInfo;
    }

    /**
     * 解析Shamir分片
     */
    private static ShamirShare parseShamirShare(JSONObject shareJson) throws JSONException {
        ShamirShare share = new ShamirShare();
        share.index = shareJson.getInt("index");
        share.share = shareJson.getString("share");
        return share;
    }

    @Override
    public String toString() {
        return "RoomInfo{" +
                "type='" + type + '\'' +
                ", room=" + room +
                '}';
    }
}
