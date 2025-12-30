// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.profile;

import java.util.List;
import java.util.ArrayList;

/**
 * Resource Group class - Represents a group of resources with the same security level
 */
public class ResourceGroup {
    private String name; // Group name
    private int securityLevel; // Security level
    private String filePath; // File path that defines the group
    private List<String> resourceIds; // Resource ID list

    public ResourceGroup(String name, int securityLevel) {
        this.name = name;
        this.securityLevel = securityLevel;
        this.resourceIds = new ArrayList<>();
    }

    public void addResource(String resourceId) {
        if (!resourceIds.contains(resourceId)) {
            resourceIds.add(resourceId);
        }
    }

    public void removeResource(String resourceId) {
        resourceIds.remove(resourceId);
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<String> getResourceIds() {
        return new ArrayList<>(resourceIds);
    }
}

