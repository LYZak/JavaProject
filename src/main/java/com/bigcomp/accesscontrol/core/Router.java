package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.BadgeReader;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * 路由器类 - 负责在读卡器和访问控制系统之间转发消息
 */
public class Router implements PropertyChangeListener {
    private AccessRequestProcessor arp; // 访问请求处理器
    private Map<String, BadgeReader> badgeReaders; // 读卡器映射表
    private List<AccessEventListener> accessEventListeners; // 访问事件监听器列表

    public Router(AccessRequestProcessor arp) {
        this.arp = arp;
        this.badgeReaders = new ConcurrentHashMap<>();
        this.accessEventListeners = new ArrayList<>();
    }
    
    /**
     * 添加访问事件监听器
     */
    public void addAccessEventListener(AccessEventListener listener) {
        accessEventListeners.add(listener);
    }
    
    /**
     * 移除访问事件监听器
     */
    public void removeAccessEventListener(AccessEventListener listener) {
        accessEventListeners.remove(listener);
    }
    
    /**
     * 通知所有监听器访问事件
     */
    private void notifyAccessEvent(AccessRequest request, AccessResponse response) {
        for (AccessEventListener listener : accessEventListeners) {
            listener.onAccessEvent(request, response);
        }
    }

    /**
     * 注册读卡器
     */
    public void registerBadgeReader(BadgeReader reader) {
        badgeReaders.put(reader.getId(), reader);
        reader.addPropertyChangeListener(this);
    }

    /**
     * 注销读卡器
     */
    public void unregisterBadgeReader(String readerId) {
        BadgeReader reader = badgeReaders.remove(readerId);
        if (reader != null) {
            reader.removePropertyChangeListener(this);
        }
    }

    /**
     * 处理属性变化事件（来自读卡器）
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("accessRequest".equals(evt.getPropertyName())) {
            AccessRequest request = (AccessRequest) evt.getNewValue();
            handleAccessRequest(request);
        }
    }

    /**
     * 处理访问请求
     */
    private void handleAccessRequest(AccessRequest request) {
        // 转发到访问请求处理器
        AccessResponse response = arp.processRequest(request);
        
        // 通知监听器
        notifyAccessEvent(request, response);
        
        // 将响应转发回对应的读卡器
        BadgeReader reader = badgeReaders.get(request.getBadgeReaderId());
        if (reader != null) {
            reader.handleAccessResponse(response);
        }
    }
    
    /**
     * 访问事件监听器接口
     */
    public interface AccessEventListener {
        void onAccessEvent(AccessRequest request, AccessResponse response);
    }

    /**
     * 获取所有注册的读卡器
     */
    public Map<String, BadgeReader> getBadgeReaders() {
        return badgeReaders;
    }
}

