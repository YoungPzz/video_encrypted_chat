package com.example.android_bysj_demo;

public class FriendRequest {
    private String id;
    private String fromUserId;
    private String fromUserName;
    private String message;

    public FriendRequest(String id, String fromUserId, String fromUserName, String message) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public String getMessage() {
        return message;
    }
}
