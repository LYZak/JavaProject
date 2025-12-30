package com.bigcomp.accesscontrol.model;

import com.bigcomp.accesscontrol.util.SystemClock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 徽章类 - 模拟物理或虚拟徽章
 * 包含唯一标识码，用于识别用户
 */
public class Badge {
    private String code; // 唯一标识码
    private String userId; // 徽章持有者的用户ID
    private LocalDateTime creationDate; // 创建日期
    private LocalDateTime expirationDate; // 过期日期
    private LocalDateTime lastUpdateDate; // 最后更新日期
    private boolean valid; // 徽章是否有效

    public Badge(String userId) {
        this.code = generateUniqueCode();
        this.userId = userId;
        LocalDateTime now = SystemClock.now();
        this.creationDate = now;
        this.expirationDate = now.plusYears(1); // 默认1年有效期
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
     * 生成唯一的徽章代码
     */
    private String generateUniqueCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 更新徽章代码（由读卡器执行）
     */
    public void updateCode() {
        this.code = generateUniqueCode();
        this.lastUpdateDate = SystemClock.now();
    }

    /**
     * 检查徽章是否过期
     */
    public boolean isExpired() {
        return SystemClock.now().isAfter(expirationDate);
    }

    /**
     * 检查徽章是否需要更新（基于最后更新日期）
     */
    public boolean needsUpdate() {
        // 如果距离上次更新超过6个月，需要更新
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

