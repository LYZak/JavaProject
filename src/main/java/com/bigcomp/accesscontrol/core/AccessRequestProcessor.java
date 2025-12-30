package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 访问请求处理器（ARP）- 核心访问控制逻辑
 * 必须在内存中高效处理请求，不能访问数据库
 */
public class AccessRequestProcessor {
    private DatabaseManager dbManager;
    private ProfileManager profileManager;
    
    // 内存中的快速查找结构
    private Map<String, User> usersByBadgeCode; // 通过徽章代码查找用户
    private Map<String, Set<String>> userProfiles; // 用户ID -> 配置文件名称集合
    private Map<String, Resource> resources; // 资源ID -> 资源对象
    private Map<String, String> resourceGroups; // 资源ID -> 组名称

    public AccessRequestProcessor(DatabaseManager dbManager, ProfileManager profileManager) {
        this.dbManager = dbManager;
        this.profileManager = profileManager;
        loadDataIntoMemory();
    }

    /**
     * 将数据加载到内存中以便快速访问
     */
    private void loadDataIntoMemory() {
        // 从数据库加载所有数据到内存
        usersByBadgeCode = dbManager.loadUsersByBadgeCode();
        userProfiles = dbManager.loadUserProfiles();
        resources = dbManager.loadAllResources();
        resourceGroups = dbManager.loadResourceGroups();
    }

    /**
     * 处理访问请求
     * @param request 访问请求
     * @return 访问响应
     */
    public AccessResponse processRequest(AccessRequest request) {
        String badgeCode = request.getBadgeCode();
        String resourceId = request.getResourceId();
        LocalDateTime requestTime = request.getTimestamp();

        // 1. 查找用户
        User user = usersByBadgeCode.get(badgeCode);
        if (user == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "未找到用户");
        }

        // 2. 检查资源状态
        Resource resource = resources.get(resourceId);
        if (resource == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "资源不存在");
        }

        if (resource.getState() == Resource.ResourceState.UNCONTROLLED) {
            return new AccessResponse(request.getBadgeReaderId(), true, "资源处于非受控状态");
        }

        // 3. 获取用户的配置文件
        Set<String> profileNames = userProfiles.get(user.getId());
        if (profileNames == null || profileNames.isEmpty()) {
            return new AccessResponse(request.getBadgeReaderId(), false, "用户没有配置访问权限");
        }

        // 4. 获取资源所属的组
        String groupName = resourceGroups.get(resourceId);
        if (groupName == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "资源不属于任何组");
        }

        // 5. 检查访问权限
        boolean hasAccess = false;
        String denyReason = null;
        for (String profileName : profileNames) {
            Profile profile = profileManager.getProfile(profileName);
            if (profile == null) {
                denyReason = "配置文件不存在: " + profileName;
                continue;
            }
            
            // 检查配置文件是否包含该资源组
            if (!profile.getAccessRights().containsKey(groupName)) {
                denyReason = "配置文件 \"" + profileName + "\" 未配置资源组 \"" + groupName + "\"";
                continue;
            }
            
            // 检查时间过滤器
            if (profile.hasAccess(groupName, requestTime)) {
                hasAccess = true;
                break;
            } else {
                // 详细检查为什么被拒绝
                TimeFilter filter = profile.getAccessRights().get(groupName);
                denyReason = buildDenyReason(filter, requestTime, profileName, groupName);
            }
        }

        // 6. 记录日志（由LogManager处理，这里不通过dbManager）
        String message = hasAccess ? "访问已授权" : 
            (denyReason != null ? denyReason : "访问被拒绝：权限不足");

        return new AccessResponse(request.getBadgeReaderId(), hasAccess, message);
    }

    /**
     * 重新加载内存中的数据（当数据更新时调用）
     */
    public void reloadData() {
        loadDataIntoMemory();
    }
    
    /**
     * 构建拒绝原因的详细说明
     */
    private String buildDenyReason(TimeFilter filter, LocalDateTime requestTime, 
                                   String profileName, String groupName) {
        StringBuilder reason = new StringBuilder();
        
        // 检查星期
        if (filter.getDaysOfWeek() != null && !filter.getDaysOfWeek().isEmpty()) {
            boolean weekMatch = filter.getDaysOfWeek().contains(requestTime.getDayOfWeek());
            if (filter.isExcludeDaysOfWeek()) {
                if (weekMatch) {
                    reason.append("星期").append(requestTime.getDayOfWeek())
                          .append("在排除列表中");
                    return reason.toString();
                }
            } else {
                if (!weekMatch) {
                    reason.append("星期").append(requestTime.getDayOfWeek())
                          .append("不在允许列表中(允许:").append(filter.getDaysOfWeek()).append(")");
                    return reason.toString();
                }
            }
        }
        
        // 检查时间范围
        if (filter.getTimeRanges() != null && !filter.getTimeRanges().isEmpty()) {
            int currentMinutes = requestTime.getHour() * 60 + requestTime.getMinute();
            boolean inRange = filter.getTimeRanges().stream()
                .anyMatch(range -> range.contains(currentMinutes));
            
            if (filter.isExcludeTimeRanges()) {
                if (inRange) {
                    reason.append("时间").append(String.format("%02d:%02d", 
                        requestTime.getHour(), requestTime.getMinute()))
                          .append("在排除范围内");
                    return reason.toString();
                }
            } else {
                if (!inRange) {
                    // 显示允许的时间范围
                    StringBuilder allowedRanges = new StringBuilder();
                    for (TimeFilter.TimeRange range : filter.getTimeRanges()) {
                        int startH = range.getStartMinutes() / 60;
                        int startM = range.getStartMinutes() % 60;
                        int endH = range.getEndMinutes() / 60;
                        int endM = range.getEndMinutes() % 60;
                        if (allowedRanges.length() > 0) {
                            allowedRanges.append(", ");
                        }
                        allowedRanges.append(String.format("%02d:%02d-%02d:%02d", 
                            startH, startM, endH, endM));
                    }
                    reason.append("时间").append(String.format("%02d:%02d", 
                        requestTime.getHour(), requestTime.getMinute()))
                          .append("不在允许范围内(允许:").append(allowedRanges).append(")");
                    return reason.toString();
                }
            }
        }
        
        return "时间过滤器不允许";
    }
    
    // Getters for accessing memory data
    public Map<String, User> getUsersByBadgeCode() {
        return usersByBadgeCode;
    }
    
    public Map<String, Resource> getResources() {
        return resources;
    }
}

