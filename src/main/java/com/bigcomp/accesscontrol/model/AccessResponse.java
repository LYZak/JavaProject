// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.model;

/**
 * Access Response class - Represents access control system response
 */
public class AccessResponse {
    private String badgeReaderId; // Badge reader ID
    private boolean granted; // Whether granted
    private String message; // Response message

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

