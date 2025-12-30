// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.model;

import java.time.LocalDateTime;

/**
 * Access Request class - Represents an access request
 */
public class AccessRequest {
    private String badgeCode; // Badge code
    private String badgeReaderId; // Badge reader ID
    private String resourceId; // Resource ID
    private LocalDateTime timestamp; // Request timestamp

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

