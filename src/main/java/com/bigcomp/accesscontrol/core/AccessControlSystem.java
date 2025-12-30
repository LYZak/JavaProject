// Group 2 ChenGong ZhangZhao LiangYiKuo
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
 * Access Control System - Main system controller
 * Integrates all components
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
     * Process access request and log event
     */
    public void processAccessRequest(AccessRequest request, User user, Resource resource, boolean granted) {
        // Log event
        logManager.logAccess(request, user, resource, granted);
    }
    
    /**
     * Get user and resource information and log event
     */
    public void logAccessEvent(AccessRequest request, AccessResponse response) {
        // Get information from request
        String badgeCode = request.getBadgeCode();
        String resourceId = request.getResourceId();
        
        // Get user and resource information from memory
        Map<String, User> usersByBadgeCode = arp.getUsersByBadgeCode();
        Map<String, Resource> resources = arp.getResources();
        
        User user = usersByBadgeCode != null ? usersByBadgeCode.get(badgeCode) : null;
        Resource resource = resources != null ? resources.get(resourceId) : null;
        
        if (user != null && resource != null) {
            logManager.logAccess(request, user, resource, response.isGranted());
        }
    }
    
    /**
     * Initialize router event listeners for logging
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

