// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Access Request Processor (ARP) - Core access control logic
 * Must process requests efficiently in memory, cannot access database
 */
public class AccessRequestProcessor {
    private DatabaseManager dbManager;
    private ProfileManager profileManager;
    
    // Thread-safe context container using Snapshot Pattern
    private volatile AccessControlContext context;
    
    // Immutable context to hold all data maps
    private static class AccessControlContext {
        final Map<String, User> usersByBadgeCode;
        final Map<String, User> usersById; // Added for fast user lookup
        final Map<String, Badge> badgesByCode;
        final Map<String, Set<String>> userProfiles;
        final Map<String, Resource> resources;
        final Map<String, String> resourceGroups;

        AccessControlContext(Map<String, User> usersByBadgeCode,
                           Map<String, User> usersById,
                           Map<String, Badge> badgesByCode,
                           Map<String, Set<String>> userProfiles,
                           Map<String, Resource> resources,
                           Map<String, String> resourceGroups) {
            this.usersByBadgeCode = usersByBadgeCode;
            this.usersById = usersById;
            this.badgesByCode = badgesByCode;
            this.userProfiles = userProfiles;
            this.resources = resources;
            this.resourceGroups = resourceGroups;
        }
    }

    public AccessRequestProcessor(DatabaseManager dbManager, ProfileManager profileManager) {
        this.dbManager = dbManager;
        this.profileManager = profileManager;
        this.context = loadDataIntoMemory();
    }

    /**
     * Load data into memory for fast access
     * Creates a new immutable context
     */
    private AccessControlContext loadDataIntoMemory() {
        // Load all data from database into memory
        Map<String, User> usersByBadgeCode = dbManager.loadUsersByBadgeCode();
        Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
        Map<String, Resource> resources = dbManager.loadAllResources();
        Map<String, String> resourceGroups = dbManager.loadResourceGroups();
        
        // Load badges for validation
        Map<String, Badge> badgesByCode = new HashMap<>();
        Map<String, Badge> allBadges = dbManager.loadAllBadges();
        for (Badge badge : allBadges.values()) {
            badgesByCode.put(badge.getCode(), badge);
        }
        
        // Build usersById for fast lookup during updates
        Map<String, User> usersById = new HashMap<>();
        for (User user : usersByBadgeCode.values()) {
            usersById.put(user.getId(), user);
        }
        
        return new AccessControlContext(
            usersByBadgeCode,
            usersById,
            badgesByCode,
            userProfiles,
            resources,
            resourceGroups
        );
    }

    /**
     * Process access request
     * @param request Access request
     * @return Access response
     */
    public AccessResponse processRequest(AccessRequest request) {
        // Get local reference to current context (thread-safe)
        AccessControlContext currentContext = this.context;
        
        String badgeCode = request.getBadgeCode();
        String resourceId = request.getResourceId();
        LocalDateTime requestTime = request.getTimestamp();

        // 0. Check badge validity
        Badge badge = currentContext.badgesByCode.get(badgeCode);
        if (badge == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "Badge not found");
        }
        
        if (!badge.isValid()) {
             return new AccessResponse(request.getBadgeReaderId(), false, "Badge is invalid or expired");
        }

        // 1. Find user
        User user = currentContext.usersByBadgeCode.get(badgeCode);
        if (user == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "User not found");
        }

        // 2. Check resource status
        Resource resource = currentContext.resources.get(resourceId);
        if (resource == null) {
            return new AccessResponse(request.getBadgeReaderId(), false, "Resource does not exist");
        }

        if (resource.getState() == Resource.ResourceState.UNCONTROLLED) {
            return new AccessResponse(request.getBadgeReaderId(), true, "Resource is in uncontrolled state");
        }

        // 3. Get user profiles
        Set<String> profileNames = currentContext.userProfiles.get(user.getId());
        if (profileNames == null || profileNames.isEmpty()) {
            return new AccessResponse(request.getBadgeReaderId(), false, "User has no access permissions configured");
        }

        // 4. Get resource group
        String groupName = currentContext.resourceGroups.get(resourceId);
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
     * Replaces the entire context atomically
     */
    public void reloadData() {
        this.context = loadDataIntoMemory();
    }
    
    /**
     * Update badge (DB persistence + Cache update)
     */
    public void updateBadge(Badge badge) {
        try {
            // 1. Update Database
            dbManager.updateBadge(badge);
            
            // 2. Update Cache
            updateBadgeCache(badge);
        } catch (Exception e) {
            System.err.println("Failed to update badge: " + e.getMessage());
        }
    }

    /**
     * Update cache for a single badge without full reload
     * Used when a badge is updated (e.g. rotated code)
     */
    public void updateBadgeCache(Badge badge) {
        AccessControlContext current = this.context;
        
        // 1. Create copies of maps that need update
        Map<String, Badge> newBadges = new HashMap<>(current.badgesByCode);
        Map<String, User> newUsersByCode = new HashMap<>(current.usersByBadgeCode);
        
        // 2. Update badge map
        newBadges.put(badge.getCode(), badge);
        
        // 3. Update user map (need to find user first)
        User user = current.usersById.get(badge.getUserId());
        if (user != null) {
            newUsersByCode.put(badge.getCode(), user);
        }
        
        // 4. Create new context (sharing other maps)
        this.context = new AccessControlContext(
            newUsersByCode,
            current.usersById, // User list didn't change
            newBadges,
            current.userProfiles,
            current.resources,
            current.resourceGroups
        );
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
    
    // Getters for accessing memory data (delegating to context)
    public Map<String, User> getUsersByBadgeCode() {
        return context.usersByBadgeCode;
    }
    
    public Map<String, Resource> getResources() {
        return context.resources;
    }
}
