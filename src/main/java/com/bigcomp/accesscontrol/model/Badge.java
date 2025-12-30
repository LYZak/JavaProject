// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.model;

import com.bigcomp.accesscontrol.util.SystemClock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Badge class - Simulates physical or virtual badge
 * Contains unique identification code for user identification
 */
public class Badge {
    private String code; // Unique identification code
    private String userId; // User ID of badge holder
    private LocalDateTime creationDate; // Creation date
    private LocalDateTime expirationDate; // Expiration date
    private LocalDateTime lastUpdateDate; // Last update date
    private boolean valid; // Whether badge is valid

    public Badge(String userId) {
        this.code = generateUniqueCode();
        this.userId = userId;
        LocalDateTime now = SystemClock.now();
        this.creationDate = now;
        this.expirationDate = now.plusYears(1); // Default 1 year validity
        this.lastUpdateDate = now;
        this.valid = true;
    }

    public Badge(String code, String userId, LocalDateTime creationDate, 
                 LocalDateTime expirationDate, LocalDateTime lastUpdateDate, boolean valid) {
        this.code = code;
        this.userId = userId;
        this.creationDate = creationDate;
        this.expirationDate = expirationDate;
        this.lastUpdateDate = lastUpdateDate;
        this.valid = valid;
    }

    /**
     * Generate unique badge code
     */
    private String generateUniqueCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * Update badge code (executed by badge reader)
     */
    public void updateCode() {
        this.code = generateUniqueCode();
        this.lastUpdateDate = SystemClock.now();
    }

    /**
     * Check if badge is expired
     */
    public boolean isExpired() {
        return SystemClock.now().isAfter(expirationDate);
    }

    /**
     * Check if badge needs update (based on last update date)
     */
    public boolean needsUpdate() {
        // If more than 6 months since last update, needs update
        return lastUpdateDate.isBefore(SystemClock.now().minusMonths(6));
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(LocalDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public boolean isValid() {
        return valid && !isExpired();
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}

