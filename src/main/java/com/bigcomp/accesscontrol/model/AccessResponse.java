package com.bigcomp.accesscontrol.model;

/**
 * 访问响应类 - 表示访问控制系统的响应
 */
public class AccessResponse {
    private String badgeReaderId; // 读卡器ID
    private boolean granted; // 是否授权
    private String message; // 响应消息

    public AccessResponse(String badgeReaderId, boolean granted, String message) {
        this.badgeReaderId = badgeReaderId;
        this.granted = granted;
        this.message = message;
    }

    // Getters and Setters
    public String getBadgeReaderId() {
        return badgeReaderId;
    }

    public void setBadgeReaderId(String badgeReaderId) {
        this.badgeReaderId = badgeReaderId;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

