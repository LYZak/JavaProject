package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.logging.LogManager;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.model.Resource;
import java.util.Map;

/**
 * 访问控制系统 - 系统主控制器
 * 整合所有组件
 */
public class AccessControlSystem {
    private DatabaseManager dbManager;
    private ProfileManager profileManager;
    private AccessRequestProcessor arp;
    private Router router;
    private LogManager logManager;

    public AccessControlSystem() {
        this.dbManager = new DatabaseManager();
        this.profileManager = new ProfileManager();
        this.arp = new AccessRequestProcessor(dbManager, profileManager);
        this.router = new Router(arp);
        this.logManager = new LogManager();
        initializeEventLogging();
    }

    /**
     * 处理访问请求并记录日志
     */
    public void processAccessRequest(AccessRequest request, User user, Resource resource, boolean granted) {
        // 记录日志
        logManager.logAccess(request, user, resource, granted);
    }
    
    /**
     * 获取用户和资源信息并记录日志
     */
    public void logAccessEvent(AccessRequest request, AccessResponse response) {
        // 从请求中获取信息
        String badgeCode = request.getBadgeCode();
        String resourceId = request.getResourceId();
        
        // 从内存中获取用户和资源信息
        Map<String, User> usersByBadgeCode = arp.getUsersByBadgeCode();
        Map<String, Resource> resources = arp.getResources();
        
        User user = usersByBadgeCode != null ? usersByBadgeCode.get(badgeCode) : null;
        Resource resource = resources != null ? resources.get(resourceId) : null;
        
        if (user != null && resource != null) {
            logManager.logAccess(request, user, resource, response.isGranted());
        }
    }
    
    /**
     * 初始化路由器的事件监听，用于记录日志
     */
    public void initializeEventLogging() {
        router.addAccessEventListener((request, response) -> {
            logAccessEvent(request, response);
        });
    }

    public Router getRouter() {
        return router;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public AccessRequestProcessor getAccessRequestProcessor() {
        return arp;
    }

    public LogManager getLogManager() {
        return logManager;
    }
}

