package com.bigcomp.accesscontrol.profile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 配置文件类 - 定义用户的访问权限
 * 包含多个访问权限（组 + 时间过滤器）
 */
public class Profile {
    private String name; // 配置文件名称
    private Map<String, TimeFilter> accessRights; // 组名称 -> 时间过滤器

    public Profile(String name) {
        this.name = name;
        this.accessRights = new HashMap<>();
    }

    /**
     * 添加访问权限
     * @param groupName 组名称
     * @param timeFilter 时间过滤器
     */
    public void addAccessRight(String groupName, TimeFilter timeFilter) {
        accessRights.put(groupName, timeFilter);
    }

    /**
     * 移除访问权限
     */
    public void removeAccessRight(String groupName) {
        accessRights.remove(groupName);
    }

    /**
     * 检查是否有访问权限
     * @param groupName 组名称
     * @param dateTime 访问时间
     * @return 是否有权限
     */
    public boolean hasAccess(String groupName, LocalDateTime dateTime) {
        TimeFilter filter = accessRights.get(groupName);
        if (filter == null) {
            return false;
        }
        return filter.matches(dateTime);
    }

    /**
     * 获取所有访问权限
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

