// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.util;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.util.SystemClock;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Access Control Diagnostic Tool
 * Used to diagnose why access requests are denied
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
     * Diagnose why access request was denied
     */
    public String diagnoseAccessIssue(String badgeCode, String resourceId) {
        StringBuilder report = new StringBuilder();
        report.append("=== Access Control Diagnostic Report ===\n\n");
        
        // 1. Check badge and user
        report.append("1. Check User and Badge:\n");
        Map<String, User> usersByBadgeCode = dbManager.loadUsersByBadgeCode();
        User user = usersByBadgeCode.get(badgeCode);
        if (user == null) {
            report.append("   ✗ User not found (Badge code: ").append(badgeCode).append(")\n");
            return report.toString();
        }
        report.append("   ✓ User found: ").append(user.getFullName()).append(" (").append(user.getId()).append(")\n");
        
        // Check badge (get through user)
        Badge badge = null;
        if (user.getBadgeId() != null) {
            badge = dbManager.loadBadgeById(user.getBadgeId());
        }
        if (badge == null) {
            // Try to load by user ID
            badge = dbManager.loadBadgeByUserId(user.getId());
        }
        if (badge != null) {
            report.append("   ✓ Badge found: ").append(badgeCode);
            if (!badge.isValid()) {
                report.append(" (Badge is invalid!)");
            }
            report.append("\n");
        } else {
            report.append("   ⚠ Badge record not found\n");
        }
        report.append("\n");
        
        // 2. Check resource
        report.append("2. Check Resource:\n");
        Map<String, Resource> resources = dbManager.loadAllResources();
        Resource resource = resources.get(resourceId);
        if (resource == null) {
            report.append("   ✗ Resource does not exist (Resource ID: ").append(resourceId).append(")\n");
            return report.toString();
        }
        report.append("   ✓ Resource found: ").append(resource.getName()).append(" (").append(resourceId).append(")\n");
        report.append("   ✓ Resource type: ").append(resource.getType()).append("\n");
        report.append("   ✓ Resource state: ").append(resource.getState()).append("\n\n");
        
        // 3. Check user profiles
        report.append("3. Check User Profiles:\n");
        Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
        Set<String> profileNames = userProfiles.get(user.getId());
        if (profileNames == null || profileNames.isEmpty()) {
            report.append("   ✗ User has no profiles assigned!\n");
            report.append("   Solution: Assign profiles to user in User Management\n\n");
            return report.toString();
        }
        report.append("   ✓ Number of user profiles: ").append(profileNames.size()).append("\n");
        for (String profileName : profileNames) {
            report.append("     - ").append(profileName).append("\n");
        }
        report.append("\n");
        
        // 4. Check resource group
        report.append("4. Check Resource Group:\n");
        Map<String, String> resourceGroups = dbManager.loadResourceGroups();
        String groupName = resourceGroups.get(resourceId);
        if (groupName == null) {
            report.append("   ✗ Resource does not belong to any resource group!\n");
            report.append("   Solution: Add resource to resource group in Resource Group Management\n\n");
            return report.toString();
        }
        report.append("   ✓ Resource group: ").append(groupName).append("\n\n");
        
        // 5. Check profile permissions
        report.append("5. Check Profile Permissions:\n");
        LocalDateTime currentTime = SystemClock.now();
        report.append("   Current system time: ").append(currentTime.format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("   Current day of week: ").append(currentTime.getDayOfWeek()).append("\n");
        report.append("   Current time: ").append(String.format("%02d:%02d", 
            currentTime.getHour(), currentTime.getMinute())).append("\n\n");
        
        boolean hasAccessInAnyProfile = false;
        for (String profileName : profileNames) {
            Profile profile = profileManager.getProfile(profileName);
            if (profile == null) {
                report.append("   ✗ Profile does not exist: ").append(profileName).append("\n");
                continue;
            }
            
            boolean hasAccess = profile.hasAccess(groupName, currentTime);
            report.append("   Profile: ").append(profileName).append("\n");
            
            // Check if resource group is configured
            if (!profile.getAccessRights().containsKey(groupName)) {
                report.append("     ✗ Resource group not configured\n");
                continue;
            }
            
            // Detailed check of time filter
            TimeFilter filter = profile.getAccessRights().get(groupName);
            report.append("     ✓ Resource group configured\n");
            report.append("     Time filter details:\n");
            
            // Check day of week
            if (filter.getDaysOfWeek() != null) {
                if (filter.getDaysOfWeek().isEmpty()) {
                    report.append("       ⚠ Warning: Day of week set is empty! This will deny all access!\n");
                } else {
                    boolean weekMatch = filter.getDaysOfWeek().contains(currentTime.getDayOfWeek());
                    report.append("       Allowed days of week: ").append(filter.getDaysOfWeek());
                    if (filter.isExcludeDaysOfWeek()) {
                        report.append(" (exclusion mode)");
                    }
                    report.append("\n");
                    report.append("       Current day match: ").append(weekMatch ? "✓" : "✗");
                    if (filter.isExcludeDaysOfWeek()) {
                        report.append(" (exclusion mode: ").append(weekMatch ? "deny" : "allow").append(")");
                    }
                    report.append("\n");
                }
            } else {
                report.append("       Day of week: Not restricted (all days allowed)\n");
            }
            
            // Check time range
            if (filter.getTimeRanges() != null && !filter.getTimeRanges().isEmpty()) {
                int currentMinutes = currentTime.getHour() * 60 + currentTime.getMinute();
                boolean inRange = filter.getTimeRanges().stream()
                    .anyMatch(range -> range.contains(currentMinutes));
                
                report.append("       Allowed time ranges:\n");
                for (TimeFilter.TimeRange range : filter.getTimeRanges()) {
                    int startH = range.getStartMinutes() / 60;
                    int startM = range.getStartMinutes() % 60;
                    int endH = range.getEndMinutes() / 60;
                    int endM = range.getEndMinutes() % 60;
                    report.append("         ").append(String.format("%02d:%02d - %02d:%02d", 
                        startH, startM, endH, endM));
                    if (range.contains(currentMinutes)) {
                        report.append(" ✓ (current time in this range)");
                    }
                    report.append("\n");
                }
                if (filter.isExcludeTimeRanges()) {
                    report.append("       Exclusion mode: ").append(inRange ? "✗ deny" : "✓ allow").append("\n");
                } else {
                    report.append("       Current time match: ").append(inRange ? "✓" : "✗").append("\n");
                }
            } else {
                report.append("       Time range: Not restricted (all times allowed)\n");
            }
            
            // Check year, month, day
            if (filter.getYears() != null && !filter.getYears().isEmpty()) {
                boolean yearMatch = filter.getYears().contains(currentTime.getYear());
                report.append("       Year: ").append(filter.getYears());
                if (filter.isExcludeYears()) {
                    report.append(" (exclusion mode)");
                }
                report.append(" - Match: ").append(yearMatch ? "✓" : "✗").append("\n");
            }
            
            if (filter.getMonths() != null && !filter.getMonths().isEmpty()) {
                boolean monthMatch = filter.getMonths().contains(currentTime.getMonth());
                report.append("       Month: ").append(filter.getMonths());
                if (filter.isExcludeMonths()) {
                    report.append(" (exclusion mode)");
                }
                report.append(" - Match: ").append(monthMatch ? "✓" : "✗").append("\n");
            }
            
            if (filter.getDaysOfMonth() != null && !filter.getDaysOfMonth().isEmpty()) {
                boolean dayMatch = filter.getDaysOfMonth().contains(currentTime.getDayOfMonth());
                report.append("       Day of month: ").append(filter.getDaysOfMonth());
                if (filter.isExcludeDaysOfMonth()) {
                    report.append(" (exclusion mode)");
                }
                report.append(" - Match: ").append(dayMatch ? "✓" : "✗").append("\n");
            }
            
            // Final result
            report.append("     Final permission judgment: ");
            if (hasAccess) {
                report.append("✓ Has access permission");
                hasAccessInAnyProfile = true;
            } else {
                report.append("✗ No access permission (time filter does not allow)");
            }
            report.append("\n\n");
        }
        
        if (!hasAccessInAnyProfile) {
            report.append("\n   ✗ All profiles have no permission to access this resource group!\n");
            report.append("   Possible reasons:\n");
            report.append("   - Profile does not include permissions for this resource group\n");
            report.append("   - Time filter does not allow access at current time\n");
            report.append("   Solution: Edit profile in Profile Management, add access permissions for this resource group\n");
        }
        
        report.append("\n=== Diagnosis Complete ===\n");
        return report.toString();
    }
    
    /**
     * Generate system status report
     */
    public String generateSystemStatusReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== System Status Report ===\n\n");
        
        // User statistics
        Map<String, User> allUsers = dbManager.loadAllUsers();
        Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
        int usersWithProfiles = 0;
        for (String userId : allUsers.keySet()) {
            Set<String> profiles = userProfiles.get(userId);
            if (profiles != null && !profiles.isEmpty()) {
                usersWithProfiles++;
            }
        }
        report.append("User Statistics:\n");
        report.append("  - Total users: ").append(allUsers.size()).append("\n");
        report.append("  - Users with profiles: ").append(usersWithProfiles).append("\n");
        report.append("  - Users without profiles: ").append(allUsers.size() - usersWithProfiles).append("\n\n");
        
        // Resource statistics
        Map<String, Resource> resources = dbManager.loadAllResources();
        Map<String, String> resourceGroups = dbManager.loadResourceGroups();
        int resourcesInGroups = 0;
        for (String resourceId : resources.keySet()) {
            if (resourceGroups.containsKey(resourceId)) {
                resourcesInGroups++;
            }
        }
        report.append("Resource Statistics:\n");
        report.append("  - Total resources: ").append(resources.size()).append("\n");
        report.append("  - Resources in groups: ").append(resourcesInGroups).append("\n");
        report.append("  - Ungrouped resources: ").append(resources.size() - resourcesInGroups).append("\n\n");
        
        // Profile statistics
        Map<String, Profile> allProfiles = profileManager.getAllProfiles();
        report.append("Profile Statistics:\n");
        report.append("  - Total profiles: ").append(allProfiles.size()).append("\n");
        for (Map.Entry<String, Profile> entry : allProfiles.entrySet()) {
            Profile profile = entry.getValue();
            report.append("  - ").append(entry.getKey()).append(": ")
                  .append(profile.getAccessRights().size()).append(" resource group permissions\n");
        }
        
        report.append("\n=== Report Complete ===\n");
        return report.toString();
    }
}

