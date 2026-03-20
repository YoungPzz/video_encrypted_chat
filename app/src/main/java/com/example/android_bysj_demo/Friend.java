package com.example.android_bysj_demo;

public class Friend {
    private String id;
    private String name;
    private SecurityStatus securityStatus;
    private String avatar;

    public enum SecurityStatus {
        SAFE("安全", 0xFF4CAF50),
        WARNING("警告", 0xFFFF9800),
        DANGER("高风险", 0xFFF44336);

        private final String displayName;
        private final int color;

        SecurityStatus(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    public Friend(String id, String name, SecurityStatus securityStatus) {
        this.id = id;
        this.name = name;
        this.securityStatus = securityStatus;
        this.avatar = "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SecurityStatus getSecurityStatus() {
        return securityStatus;
    }

    public String getAvatar() {
        return avatar;
    }
}
