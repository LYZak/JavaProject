package com.bigcomp.accesscontrol.util;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 访问控制诊断工具
 * 用于诊断为什么访问请求被拒绝
 */
public class AccessDiagnostic {
    private AccessControlSystem accessControlSystem;
    private DatabaseManager dbManager;
    private ProfileManager profileManager;
    
    public AccessDiagnostic(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.dbManager = accessControlSystem.getDatabaseManager();
        this.profileManager = accessControlSystem.getProfileManager();
    }
    
    /**
     * 诊断访问请求被拒绝的原因
     */
    public String diagnoseAccessIssue(String badgeCode, String resourceId) {
        StringBuilder report = new StringBuilder();
        report.append("=== 访问控制诊断报告 ===\n\n");
        
        // 1. 检查徽章和用户
        report.append("1. 检查用户和徽章:\n");
        Map<String, User> usersByBadgeCode = dbManager.loadUsersByBadgeCode();
        User user = usersByBadgeCode.get(badgeCode);
        if (user == null) {
            report.append("   ✗ 未找到用户（徽章代码: ").append(badgeCode).append("）\n");
            return report.toString();
        }
        report.append("   ✓ 找到用户: ").append(user.getFullName()).append(" (").append(user.getId()).append(")\n");
        
        // 检查徽章（通过用户获取）
        Badge badge = null;
        if (user.getBadgeId() != null) {
            badge = dbManager.loadBadgeById(user.getBadgeId());
        }
        if (badge == null) {
            // 尝试通过用户ID加载
            badge = dbManager.loadBadgeByUserId(user.getId());
        }
        if (badge != null) {
            report.append("   ✓ 找到徽章: ").append(badgeCode);
            if (!badge.isValid()) {
                report.append(" (徽章已失效！)");
            }
            report.append("\n");
        } else {
            report.append("   ⚠ 未找到徽章记录\n");
        }
        report.append("\n");
        
        // 2. 检查资源
        report.append("2. 检查资源:\n");
        Map<String, Resource> resources = dbManager.loadAllResources();
        Resource resource = resources.get(resourceId);
        if (resource == null) {
            report.append("   ✗ 资源不存在 (资源ID: ").append(resourceId).append(")\n");
            return report.toString();
        }
        report.append("   ✓ 找到资源: ").append(resource.getName()).append(" (").append(resourceId).append(")\n");
        report.append("   ✓ 资源类型: ").append(resource.getType()).append("\n");
        report.append("   ✓ 资源状态: ").append(resource.getState()).append("\n\n");
        
        // 3. 检查用户配置文件
        report.append("3. 检查用户配置文件:\n");
        Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
        Set<String> profileNames = userProfiles.get(user.getId());
        if (profileNames == null || profileNames.isEmpty()) {
            report.append("   ✗ 用户没有分配配置文件！\n");
            report.append("   解决方案: 在用户管理中为用户分配配置文件\n\n");
            return report.toString();
        }
        report.append("   ✓ 用户配置文件数量: ").append(profileNames.size()).append("\n");
        for (String profileName : profileNames) {
            report.append("     - ").append(profileName).append("\n");
        }
        report.append("\n");
        
        // 4. 检查资源组
        report.append("4. 检查资源组:\n");
        Map<String, String> resourceGroups = dbManager.loadResourceGroups();
        String groupName = resourceGroups.get(resourceId);
        if (groupName == null) {
            report.append("   ✗ 资源不属于任何资源组！\n");
            report.append("   解决方案: 在资源组管理中将资源添加到资源组\n\n");
            return report.toString();
        }
        report.append("   ✓ 资源所属组: ").append(groupName).append("\n\n");
        
        // 5. 检查配置文件权限
        report.append("5. 检查配置文件权限:\n");
        LocalDateTime currentTime = SystemClock.now();
        report.append("   当前系统时间: ").append(currentTime.format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("   当前星期: ").append(currentTime.getDayOfWeek()).append("\n");
        report.append("   当前时间: ").append(String.format("%02d:%02d", 
            currentTime.getHour(), currentTime.getMinute())).append("\n\n");
        
        boolean hasAccessInAnyProfile = false;
        for (String profileName : profileNames) {
            Profile profile = profileManager.getProfile(profileName);
            if (profile == null) {
                report.append("   ✗ 配置文件不存在: ").append(profileName).append("\n");
                continue;
            }
            
            boolean hasAccess = profile.hasAccess(groupName, currentTime);
            report.append("   配置文件: ").append(profileName).append("\n");
            
            // 检查是否配置了该资源组
            if (!profile.getAccessRights().containsKey(groupName)) {
                report.append("     ✗ 未配置该资源组\n");
                continue;
            }
            
            // 详细检查时间过滤器
            TimeFilter filter = profile.getAccessRights().get(groupName);
            report.append("     ✓ 已配置该资源组\n");
            report.append("     时间过滤器详情:\n");
            
            // 检查星期
            if (filter.getDaysOfWeek() != null) {
                if (filter.getDaysOfWeek().isEmpty()) {
                    report.append("       ⚠ 警告: 星期集合为空！这将拒绝所有访问！\n");
                } else {
                    boolean weekMatch = filter.getDaysOfWeek().contains(currentTime.getDayOfWeek());
                    report.append("       允许的星期: ").append(filter.getDaysOfWeek());
                    if (filter.isExcludeDaysOfWeek()) {
                        report.append(" (排除模式)");
                    }
                    report.append("\n");
                    report.append("       当前星期匹配: ").append(weekMatch ? "✓" : "✗");
                    if (filter.isExcludeDaysOfWeek()) {
                        report.append(" (排除模式: ").append(weekMatch ? "拒绝" : "允许").append(")");
                    }
                    report.append("\n");
                }
            } else {
                report.append("       星期: 未限制（所有星期允许）\n");
            }
            
            // 检查时间范围
            if (filter.getTimeRanges() != null && !filter.getTimeRanges().isEmpty()) {
                int currentMinutes = currentTime.getHour() * 60 + currentTime.getMinute();
                boolean inRange = filter.getTimeRanges().stream()
                    .anyMatch(range -> range.contains(currentMinutes));
                
                report.append("       允许的时间范围:\n");
                for (TimeFilter.TimeRange range : filter.getTimeRanges()) {
                    int startH = range.getStartMinutes() / 60;
                    int startM = range.getStartMinutes() % 60;
                    int endH = range.getEndMinutes() / 60;
                    int endM = range.getEndMinutes() % 60;
                    report.append("         ").append(String.format("%02d:%02d - %02d:%02d", 
                        startH, startM, endH, endM));
                    if (range.contains(currentMinutes)) {
                        report.append(" ✓ (当前时间在此范围)");
                    }
                    report.append("\n");
                }
                if (filter.isExcludeTimeRanges()) {
                    report.append("       排除模式: ").append(inRange ? "✗ 拒绝" : "✓ 允许").append("\n");
                } else {
                    report.append("       当前时间匹配: ").append(inRange ? "✓" : "✗").append("\n");
                }
            } else {
                report.append("       时间范围: 未限制（所有时间允许）\n");
            }
            
            // 检查年份、月份、日期
            if (filter.getYears() != null && !filter.getYears().isEmpty()) {
                boolean yearMatch = filter.getYears().contains(currentTime.getYear());
                report.append("       年份: ").append(filter.getYears());
                if (filter.isExcludeYears()) {
                    report.append(" (排除模式)");
                }
                report.append(" - 匹配: ").append(yearMatch ? "✓" : "✗").append("\n");
            }
            
            if (filter.getMonths() != null && !filter.getMonths().isEmpty()) {
                boolean monthMatch = filter.getMonths().contains(currentTime.getMonth());
                report.append("       月份: ").append(filter.getMonths());
                if (filter.isExcludeMonths()) {
                    report.append(" (排除模式)");
                }
                report.append(" - 匹配: ").append(monthMatch ? "✓" : "✗").append("\n");
            }
            
            if (filter.getDaysOfMonth() != null && !filter.getDaysOfMonth().isEmpty()) {
                boolean dayMatch = filter.getDaysOfMonth().contains(currentTime.getDayOfMonth());
                report.append("       日期: ").append(filter.getDaysOfMonth());
                if (filter.isExcludeDaysOfMonth()) {
                    report.append(" (排除模式)");
                }
                report.append(" - 匹配: ").append(dayMatch ? "✓" : "✗").append("\n");
            }
            
            // 最终结果
            report.append("     最终权限判断: ");
            if (hasAccess) {
                report.append("✓ 有权限访问");
                hasAccessInAnyProfile = true;
            } else {
                report.append("✗ 无权限访问（时间过滤器不允许）");
            }
            report.append("\n\n");
        }
        
        if (!hasAccessInAnyProfile) {
            report.append("\n   ✗ 所有配置文件都没有权限访问该资源组！\n");
            report.append("   可能原因:\n");
            report.append("   - 配置文件未包含该资源组的权限\n");
            report.append("   - 时间过滤器不允许当前时间访问\n");
            report.append("   解决方案: 在配置文件管理中编辑配置文件，添加该资源组的访问权限\n");
        }
        
        report.append("\n=== 诊断完成 ===\n");
        return report.toString();
    }
    
    /**
     * 生成系统状态报告
     */
    public String generateSystemStatusReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 系统状态报告 ===\n\n");
        
        // 用户统计
        Map<String, User> allUsers = dbManager.loadAllUsers();
        Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
        int usersWithProfiles = 0;
        for (String userId : allUsers.keySet()) {
            Set<String> profiles = userProfiles.get(userId);
            if (profiles != null && !profiles.isEmpty()) {
                usersWithProfiles++;
            }
        }
        report.append("用户统计:\n");
        report.append("  - 总用户数: ").append(allUsers.size()).append("\n");
        report.append("  - 有配置文件的用户: ").append(usersWithProfiles).append("\n");
        report.append("  - 无配置文件的用户: ").append(allUsers.size() - usersWithProfiles).append("\n\n");
        
        // 资源统计
        Map<String, Resource> resources = dbManager.loadAllResources();
        Map<String, String> resourceGroups = dbManager.loadResourceGroups();
        int resourcesInGroups = 0;
        for (String resourceId : resources.keySet()) {
            if (resourceGroups.containsKey(resourceId)) {
                resourcesInGroups++;
            }
        }
        report.append("资源统计:\n");
        report.append("  - 总资源数: ").append(resources.size()).append("\n");
        report.append("  - 在资源组中的资源: ").append(resourcesInGroups).append("\n");
        report.append("  - 未分组的资源: ").append(resources.size() - resourcesInGroups).append("\n\n");
        
        // 配置文件统计
        Map<String, Profile> allProfiles = profileManager.getAllProfiles();
        report.append("配置文件统计:\n");
        report.append("  - 总配置文件数: ").append(allProfiles.size()).append("\n");
        for (Map.Entry<String, Profile> entry : allProfiles.entrySet()) {
            Profile profile = entry.getValue();
            report.append("  - ").append(entry.getKey()).append(": ")
                  .append(profile.getAccessRights().size()).append(" 个资源组权限\n");
        }
        
        report.append("\n=== 报告完成 ===\n");
        return report.toString();
    }
}

