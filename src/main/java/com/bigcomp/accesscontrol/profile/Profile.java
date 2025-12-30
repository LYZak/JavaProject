// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.profile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Profile class - Defines user access permissions
 * Contains multiple access rights (group + time filter)
 */
public class Profile {
    private String name; // Profile name
    private Map<String, TimeFilter> accessRights; // Group name -> Time filter

    public Profile(String name) {
        this.name = name;
        this.accessRights = new HashMap<>();
    }

    /**
     * Add access right
     * @param groupName Group name
     * @param timeFilter Time filter
     */
    public void addAccessRight(String groupName, TimeFilter timeFilter) {
        accessRights.put(groupName, timeFilter);
    }

    /**
     * Remove access right
     */
    public void removeAccessRight(String groupName) {
        accessRights.remove(groupName);
    }

    /**
     * Check if has access permission
     * @param groupName Group name
     * @param dateTime Access time
     * @return Whether has permission
     */
    public boolean hasAccess(String groupName, LocalDateTime dateTime) {
        TimeFilter filter = accessRights.get(groupName);
        if (filter == null) {
            return false;
        }
        return filter.matches(dateTime);
    }

    /**
     * Get all access rights
     */
    public Map<String, TimeFilter> getAccessRights() {
        return accessRights;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

