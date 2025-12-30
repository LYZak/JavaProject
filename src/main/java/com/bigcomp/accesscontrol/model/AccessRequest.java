package com.bigcomp.accesscontrol.model;

import java.time.LocalDateTime;

/**
 * 访问请求类 - 表示一个访问请求
 */
public class AccessRequest {
    private String badgeCode; // 徽章代码
    private String badgeReaderId; // 读卡器ID
    private String resourceId; // 资源ID
    private LocalDateTime timestamp; // 请求时间戳

    public AccessRequest(String badgeCode, String badgeReaderId, String resourceId, LocalDateTime timestamp) {
        this.badgeCode = badgeCode;
        this.badgeReaderId = badgeReaderId;
        this.resourceId = resourceId;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getBadgeCode() {
        return badgeCode;
    }

    public void setBadgeCode(String badgeCode) {
        this.badgeCode = badgeCode;
    }

    public String getBadgeReaderId() {
        return badgeReaderId;
    }

    public void setBadgeReaderId(String badgeReaderId) {
        this.badgeReaderId = badgeReaderId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

