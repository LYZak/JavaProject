// Group 2 ChenGong ZhangZhao LiangYiKuo
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
 * Access Request Processor (ARP) - Core access control logic
 * Must process requests efficiently in memory, cannot access database
 */
public class AccessRequestProcessor {
    private DatabaseManager dbManager;
    private ProfileManager profileManager;
    
    // In-memory fast lookup structures
    private Map<String, User> usersByBadgeCode; // Find user by badge code
    private Map<String, Set<String>> userProfiles; // User ID -> Profile name set
    private Map<String, Resource> resources; // Resource ID -> Resource object
    private Map<String, String> resourceGroups; // Resource ID -> Group name

    public AccessRequestProcessor(DatabaseManager dbManager, ProfileManager profileManager) {
        this.dbManager = dbManager;
        this.profileManager = profileManager;
        loadDataIntoMemory();
    }

    /**
     * Load data into memory for fast access
     */
    private void loadDataIntoMemory() {
        // Load all data from database into memory
        usersByBadgeCode = dbManager.loadUsersByBadgeCode();
        userProfiles = dbManager.loadUserProfiles();
        resources = dbManager.loadAllResources();
        resourceGroups = dbManager.loadResourceGroups();
    }

    /**
     * Process access request
     * @param request Access request
     * @return Access response
     */
    public AccessResponse processRequest(AccessRequest request) {
        String badgeCode = request.getBadgeCode();
        String resourceId = request.getResourceId();
        LocalDateTime requestTime = request.getTimestamp();

        // 1. Find user
        User user = usersByBadgeCode.get(badgeCode);
        if (user == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "User not found");
        }

        // 2. Check resource status
        Resource resource = resources.get(resourceId);
        if (resource == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "Resource does not exist");
        }

        if (resource.getState() == Resource.ResourceState.UNCONTROLLED) {
            return new AccessResponse(request.getBadgeReaderId(), true, "Resource is in uncontrolled state");
        }

        // 3. Get user profiles
        Set<String> profileNames = userProfiles.get(user.getId());
        if (profileNames == null || profileNames.isEmpty()) {
            return new AccessResponse(request.getBadgeReaderId(), false, "User has no access permissions configured");
        }

        // 4. Get resource group
        String groupName = resourceGroups.get(resourceId);
        if (groupName == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "Resource does not belong to any group");
        }

        // 5. Check access permissions
        boolean hasAccess = false;
        String denyReason = null;
        for (String profileName : profileNames) {
            Profile profile = profileManager.getProfile(profileName);
            if (profile == null) {
                denyReason = "Profile does not exist: " + profileName;
                continue;
            }
            
            // Check if profile contains this resource group
            if (!profile.getAccessRights().containsKey(groupName)) {
                denyReason = "Profile \"" + profileName + "\" does not have resource group \"" + groupName + "\" configured";
                continue;
            }
            
            // Check time filter
            if (profile.hasAccess(groupName, requestTime)) {
                hasAccess = true;
                break;
            } else {
                // Detailed check why denied
                TimeFilter filter = profile.getAccessRights().get(groupName);
                denyReason = buildDenyReason(filter, requestTime, profileName, groupName);
            }
        }

        // 6. Log event (handled by LogManager, not through dbManager here)
        String message = hasAccess ? "Access granted" : 
            (denyReason != null ? denyReason : "Access denied: Insufficient permissions");

        return new AccessResponse(request.getBadgeReaderId(), hasAccess, message);
    }

    /**
     * Reload data in memory (called when data is updated)
     */
    public void reloadData() {
        loadDataIntoMemory();
    }
    
    /**
     * Build detailed denial reason
     */
    private String buildDenyReason(TimeFilter filter, LocalDateTime requestTime, 
                                   String profileName, String groupName) {
        StringBuilder reason = new StringBuilder();
        
        // Check day of week
        if (filter.getDaysOfWeek() != null && !filter.getDaysOfWeek().isEmpty()) {
            boolean weekMatch = filter.getDaysOfWeek().contains(requestTime.getDayOfWeek());
            if (filter.isExcludeDaysOfWeek()) {
                if (weekMatch) {
                    reason.append("Day ").append(requestTime.getDayOfWeek())
                          .append(" is in exclusion list");
                    return reason.toString();
                }
            } else {
                if (!weekMatch) {
                    reason.append("Day ").append(requestTime.getDayOfWeek())
                          .append(" is not in allowed list (allowed: ").append(filter.getDaysOfWeek()).append(")");
                    return reason.toString();
                }
            }
        }
        
        // Check time range
        if (filter.getTimeRanges() != null && !filter.getTimeRanges().isEmpty()) {
            int currentMinutes = requestTime.getHour() * 60 + requestTime.getMinute();
            boolean inRange = filter.getTimeRanges().stream()
                .anyMatch(range -> range.contains(currentMinutes));
            
            if (filter.isExcludeTimeRanges()) {
                if (inRange) {
                    reason.append("Time ").append(String.format("%02d:%02d", 
                        requestTime.getHour(), requestTime.getMinute()))
                          .append(" is in exclusion range");
                    return reason.toString();
                }
            } else {
                if (!inRange) {
                    // Show allowed time ranges
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
                    reason.append("Time ").append(String.format("%02d:%02d", 
                        requestTime.getHour(), requestTime.getMinute()))
                          .append(" is not in allowed range (allowed: ").append(allowedRanges).append(")");
                    return reason.toString();
                }
            }
        }
        
        return "Time filter does not allow access";
    }
    
    // Getters for accessing memory data
    public Map<String, User> getUsersByBadgeCode() {
        return usersByBadgeCode;
    }
    
    public Map<String, Resource> getResources() {
        return resources;
    }
}

