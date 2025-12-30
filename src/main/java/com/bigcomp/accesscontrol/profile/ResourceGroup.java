package com.bigcomp.accesscontrol.profile;

import java.util.List;
import java.util.ArrayList;

/**
 * 资源组类 - 表示一组具有相同安全级别的资源
 */
public class ResourceGroup {
    private String name; // 组名称
    private int securityLevel; // 安全级别
    private String filePath; // 定义组的文件路径
    private List<String> resourceIds; // 资源ID列表

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

