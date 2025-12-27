package com.bigcomp.accesscontrol.model;

import com.bigcomp.accesscontrol.util.SystemClock;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 读卡器类 - 模拟物理读卡器
 * 可以读取徽章信息并控制资源
 */
public class BadgeReader {
    private String id; // 读卡器唯一标识
    private String resourceId; // 关联的资源ID
    private boolean active; // 读卡器是否激活
    private PropertyChangeSupport pcs; // 用于事件通知

    public BadgeReader(String id, String resourceId) {
        this.id = id;
        this.resourceId = resourceId;
        this.active = true;
        this.pcs = new PropertyChangeSupport(this);
    }

    /**
     * 刷卡操作
     * @param badge 要刷的徽章
     * @return 访问请求对象
     */
    public AccessRequest swipeBadge(Badge badge) {
        if (!active) {
            return null; // 读卡器未激活
        }

        // 读取徽章代码
        String badgeCode = badge.getCode();
        
        // 创建访问请求
        AccessRequest request = new AccessRequest(
            badgeCode,
            this.id,
            this.resourceId,
            SystemClock.now()
        );

        // 通知路由器
        pcs.firePropertyChange("accessRequest", null, request);

        return request;
    }

    /**
     * 更新徽章代码（将徽章靠近读卡器，不刷卡）
     * @param badge 要更新的徽章
     * @return 是否更新成功
     */
    public boolean updateBadge(Badge badge) {
        if (!active) {
            return false;
        }

        if (badge.needsUpdate()) {
            badge.updateCode();
            return true;
        }
        return false;
    }

    /**
     * 处理访问响应
     * @param response 访问响应
     */
    public void handleAccessResponse(AccessResponse response) {
        this.active = false; // 暂时停用读卡器

        if (response.isGranted()) {
            // 访问被授权，激活资源
            activateResource();
        } else {
            // 访问被拒绝，显示拒绝消息
            displayMessage("访问被拒绝: " + response.getMessage());
        }

        // 几秒后重新激活读卡器
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000); // 3秒后重新激活
                this.active = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 激活资源（例如开门）
     */
    private void activateResource() {
        displayMessage("访问已授权");
        // 通知资源被激活
        pcs.firePropertyChange("resourceActivated", null, resourceId);
        
        // 模拟资源操作时间（例如门打开和关闭）
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // 5秒后资源恢复
                pcs.firePropertyChange("resourceDeactivated", null, resourceId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 显示消息（模拟读卡器显示屏）
     */
    private void displayMessage(String message) {
        System.out.println("读卡器 " + id + ": " + message);
        pcs.firePropertyChange("message", null, message);
    }

    /**
     * 添加属性变化监听器
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

