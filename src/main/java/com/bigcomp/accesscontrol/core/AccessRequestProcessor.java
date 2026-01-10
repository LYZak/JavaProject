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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Access Request Processor (ARP) - Core access control logic
 * Must process requests efficiently in memory, cannot access database
 */
public class AccessRequestProcessor {
    private static final int CACHE_VERSION = 2;
    private static final String CACHE_FILE_PROPERTY = "accesscontrol.cacheFile";
    private static final String CACHE_STRICT_PROPERTY = "accesscontrol.cacheStrict";
    private static final Path DEFAULT_CACHE_FILE = Paths.get("data", "cache", "access_context.bin");
    private static final Path DEFAULT_DB_FILE = Paths.get("data", "access_control.db");

    private DatabaseManager dbManager;
    private ProfileManager profileManager;
    
    // Thread-safe context container using Snapshot Pattern
    private volatile AccessControlContext context;
    
    // Immutable context to hold all data maps
    private static class AccessControlContext implements Serializable {
        private static final long serialVersionUID = 1L;

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

    private static class CacheEnvelope implements Serializable {
        private static final long serialVersionUID = 1L;

        final int cacheVersion;
        final long createdAtEpochMillis;
        final String dbUrl;
        final Long dbLastModifiedEpochMillis;
        final AccessControlContext context;

        CacheEnvelope(int cacheVersion, long createdAtEpochMillis, String dbUrl, Long dbLastModifiedEpochMillis, AccessControlContext context) {
            this.cacheVersion = cacheVersion;
            this.createdAtEpochMillis = createdAtEpochMillis;
            this.dbUrl = dbUrl;
            this.dbLastModifiedEpochMillis = dbLastModifiedEpochMillis;
            this.context = context;
        }
    }

    public AccessRequestProcessor(DatabaseManager dbManager, ProfileManager profileManager) {
        this.dbManager = dbManager;
        this.profileManager = profileManager;
        AccessControlContext cached = tryLoadContextFromCache();
        this.context = cached != null ? cached : loadDataIntoMemory();
    }

    public void persistContextToCache() {
        if (isNonPersistentDb()) {
            return;
        }
        AccessControlContext current = this.context;
        if (current == null) {
            return;
        }

        Path cacheFile = resolveCacheFile();
        try {
            Files.createDirectories(cacheFile.getParent());
        } catch (Exception e) {
            System.err.println("Failed to create cache directory: " + e.getMessage());
            return;
        }

        Long dbLastModified = getDbLastModifiedEpochMillis(resolveDbFileFromUrl());
        CacheEnvelope envelope = new CacheEnvelope(
            CACHE_VERSION,
            System.currentTimeMillis(),
            dbManager.getDbUrl(),
            dbLastModified,
            current
        );

        Path tmpFile = cacheFile.resolveSibling(cacheFile.getFileName().toString() + ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpFile)))) {
            oos.writeObject(envelope);
            oos.flush();
        } catch (Exception e) {
            System.err.println("Failed to write cache file: " + e.getMessage());
            try {
                Files.deleteIfExists(tmpFile);
            } catch (Exception ignored) {
            }
            return;
        }

        try {
            try {
                Files.move(tmpFile, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpFile, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("Failed to move cache file into place: " + e.getMessage());
            try {
                Files.deleteIfExists(tmpFile);
            } catch (Exception ignored) {
            }
        }
    }

    private AccessControlContext tryLoadContextFromCache() {
        if (isNonPersistentDb()) {
            return null;
        }
        Path cacheFile = resolveCacheFile();
        if (!Files.exists(cacheFile)) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(cacheFile)))) {
            Object obj = ois.readObject();
            if (!(obj instanceof CacheEnvelope envelope)) {
                return null;
            }
            if (envelope.cacheVersion != CACHE_VERSION) {
                return null;
            }
            if (envelope.dbUrl == null || !envelope.dbUrl.equals(dbManager.getDbUrl())) {
                return null;
            }
            if (!isCacheValidForCurrentDb(envelope.dbLastModifiedEpochMillis)) {
                return null;
            }
            return envelope.context;
        } catch (Exception e) {
            System.err.println("Failed to load cache file, falling back to DB: " + e.getMessage());
            return null;
        }
    }

    private boolean isCacheValidForCurrentDb(Long cachedDbLastModifiedEpochMillis) {
        if (!Boolean.parseBoolean(System.getProperty(CACHE_STRICT_PROPERTY, "true"))) {
            return true;
        }
        if (cachedDbLastModifiedEpochMillis == null) {
            return true;
        }
        Long currentDbLastModified = getDbLastModifiedEpochMillis(resolveDbFileFromUrl());
        if (currentDbLastModified == null) {
            return true;
        }
        return cachedDbLastModifiedEpochMillis.equals(currentDbLastModified);
    }

    private Long getDbLastModifiedEpochMillis(Path dbFile) {
        try {
            if (!Files.exists(dbFile)) {
                return null;
            }
            return Files.getLastModifiedTime(dbFile).toMillis();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isNonPersistentDb() {
        String url = dbManager.getDbUrl();
        if (url == null) {
            return true;
        }
        return url.contains(":memory:");
    }

    private Path resolveDbFileFromUrl() {
        String url = dbManager.getDbUrl();
        if (url == null) {
            return DEFAULT_DB_FILE;
        }
        String prefix = "jdbc:sqlite:";
        if (!url.startsWith(prefix)) {
            return DEFAULT_DB_FILE;
        }
        String path = url.substring(prefix.length());
        if (path.isBlank() || path.equals(":memory:")) {
            return DEFAULT_DB_FILE;
        }
        return Paths.get(path);
    }

    private Path resolveCacheFile() {
        String override = System.getProperty(CACHE_FILE_PROPERTY);
        if (override == null || override.isBlank()) {
            return DEFAULT_CACHE_FILE;
        }
        return Paths.get(override);
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
            if (badge.isExpired()) {
                return new AccessResponse(request.getBadgeReaderId(), false, "Badge is expired");
            }
            if (badge.isUpdateExpired()) {
                return new AccessResponse(request.getBadgeReaderId(), false, "Badge is invalid");
            }
            return new AccessResponse(request.getBadgeReaderId(), false, "Badge is invalid");
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
            if (badge.needsUpdate()) {
                return new AccessResponse(request.getBadgeReaderId(), true, "Resource is in uncontrolled state (badge must be updated)");
            }
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
        List<String> sortedProfiles = new ArrayList<>(profileNames);
        Collections.sort(sortedProfiles);
        for (String profileName : sortedProfiles) {
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
                String priorityDenied = checkPriorityPolicy(profile, user, resource, requestTime);
                if (priorityDenied != null) {
                    denyReason = priorityDenied;
                    continue;
                }

                String usageDenied = checkUsageLimits(profile, user, resource, groupName, requestTime);
                if (usageDenied != null) {
                    denyReason = usageDenied;
                    continue;
                }

                hasAccess = true;
                afterGranted(profile, user, resource, groupName, requestTime);
                break;
            } else {
                // Detailed check why denied
                TimeFilter filter = profile.getAccessRights().get(groupName);
                denyReason = buildDenyReason(filter, requestTime, profileName, groupName);
            }
        }

        // 6. Log event (handled by LogManager, not through dbManager here)
        String message;
        if (hasAccess) {
            message = badge.needsUpdate() ? "Access granted (badge must be updated)" : "Access granted";
        } else {
            String base = denyReason != null ? denyReason : "Access denied: Insufficient permissions";
            message = badge.needsUpdate() ? "Badge must be updated; " + base : base;
        }

        return new AccessResponse(request.getBadgeReaderId(), hasAccess, message);
    }

    private void afterGranted(Profile profile, User user, Resource resource, String groupName, LocalDateTime requestTime) {
        if (resource.getType() == Resource.ResourceType.GATE && resource.getBuilding() != null && !resource.getBuilding().isBlank()) {
            try {
                dbManager.upsertLastGateTime(user.getId(), resource.getBuilding(), requestTime);
            } catch (Exception e) {
                System.err.println("Failed to update gate time: " + e.getMessage());
            }
        }

        com.bigcomp.accesscontrol.profile.UsageLimit limit = resolveUsageLimit(profile, resource, groupName);
        if (limit == null) {
            return;
        }
        try {
            applyUsageCounters(profile, limit, user, resource, groupName, requestTime);
        } catch (Exception e) {
            System.err.println("Failed to update usage counters: " + e.getMessage());
        }
    }

    private String checkPriorityPolicy(Profile profile, User user, Resource resource, LocalDateTime requestTime) {
        com.bigcomp.accesscontrol.profile.PriorityPolicy policy = profile.getPriorityPolicy();
        if (policy == null || !policy.isRequireGateWithinMinutesEnabled()) {
            return null;
        }
        if (resource.getBuilding() == null || resource.getBuilding().isBlank()) {
            return null;
        }
        if (policy.getBuildings() != null && !policy.getBuildings().isEmpty() && !policy.getBuildings().contains(resource.getBuilding())) {
            return null;
        }
        if (resource.getType() == Resource.ResourceType.GATE) {
            return null;
        }
        LocalDateTime lastGate = dbManager.getLastGateTime(user.getId(), resource.getBuilding());
        if (lastGate == null) {
            return "Priority rule: no recent building entry recorded";
        }
        long minutes = Duration.between(lastGate, requestTime).toMinutes();
        if (minutes > policy.getRequireGateWithinMinutes()) {
            return "Priority rule: building entry expired (" + minutes + " minutes ago)";
        }
        if (minutes < 0) {
            return "Priority rule: time anomaly";
        }
        return null;
    }

    private String checkUsageLimits(Profile profile, User user, Resource resource, String groupName, LocalDateTime requestTime) {
        com.bigcomp.accesscontrol.profile.UsageLimit limit = resolveUsageLimit(profile, resource, groupName);
        if (limit == null) {
            return null;
        }
        String policyKey = buildPolicyKey(profile, resource, groupName);
        String resourceKey = limit.isPerUserPerDayPerResource() ? resource.getId() : "";

        Integer perUserDay = limit.getPerUserPerDayMax();
        if (perUserDay != null) {
            int count = dbManager.getUsageCount("USER", user.getId(), policyKey, resourceKey, "DAY", dayStart(requestTime));
            if (count >= perUserDay) {
                return "Usage limit exceeded: per-user daily max " + perUserDay;
            }
        }
        Integer globalDay = limit.getGlobalPerDayMax();
        if (globalDay != null) {
            int count = dbManager.getUsageCount("GLOBAL", "__GLOBAL__", policyKey, "", "DAY", dayStart(requestTime));
            if (count >= globalDay) {
                return "Usage limit exceeded: global daily max " + globalDay;
            }
        }

        Integer perUserWeek = limit.getPerUserPerWeekMax();
        if (perUserWeek != null) {
            int count = dbManager.getUsageCount("USER", user.getId(), policyKey, resourceKey, "WEEK", weekStart(requestTime));
            if (count >= perUserWeek) {
                return "Usage limit exceeded: per-user weekly max " + perUserWeek;
            }
        }
        Integer globalWeek = limit.getGlobalPerWeekMax();
        if (globalWeek != null) {
            int count = dbManager.getUsageCount("GLOBAL", "__GLOBAL__", policyKey, "", "WEEK", weekStart(requestTime));
            if (count >= globalWeek) {
                return "Usage limit exceeded: global weekly max " + globalWeek;
            }
        }

        Integer perUserMonth = limit.getPerUserPerMonthMax();
        if (perUserMonth != null) {
            int count = dbManager.getUsageCount("USER", user.getId(), policyKey, resourceKey, "MONTH", monthStart(requestTime));
            if (count >= perUserMonth) {
                return "Usage limit exceeded: per-user monthly max " + perUserMonth;
            }
        }
        Integer globalMonth = limit.getGlobalPerMonthMax();
        if (globalMonth != null) {
            int count = dbManager.getUsageCount("GLOBAL", "__GLOBAL__", policyKey, "", "MONTH", monthStart(requestTime));
            if (count >= globalMonth) {
                return "Usage limit exceeded: global monthly max " + globalMonth;
            }
        }

        return null;
    }

    private void applyUsageCounters(Profile profile, com.bigcomp.accesscontrol.profile.UsageLimit limit, User user, Resource resource, String groupName, LocalDateTime requestTime) throws Exception {
        String policyKey = buildPolicyKey(profile, resource, groupName);
        String resourceKey = limit.isPerUserPerDayPerResource() ? resource.getId() : "";

        if (limit.getPerUserPerDayMax() != null) {
            dbManager.incrementUsageCount("USER", user.getId(), policyKey, resourceKey, "DAY", dayStart(requestTime));
        }
        if (limit.getGlobalPerDayMax() != null) {
            dbManager.incrementUsageCount("GLOBAL", "__GLOBAL__", policyKey, "", "DAY", dayStart(requestTime));
        }
        if (limit.getPerUserPerWeekMax() != null) {
            dbManager.incrementUsageCount("USER", user.getId(), policyKey, resourceKey, "WEEK", weekStart(requestTime));
        }
        if (limit.getGlobalPerWeekMax() != null) {
            dbManager.incrementUsageCount("GLOBAL", "__GLOBAL__", policyKey, "", "WEEK", weekStart(requestTime));
        }
        if (limit.getPerUserPerMonthMax() != null) {
            dbManager.incrementUsageCount("USER", user.getId(), policyKey, resourceKey, "MONTH", monthStart(requestTime));
        }
        if (limit.getGlobalPerMonthMax() != null) {
            dbManager.incrementUsageCount("GLOBAL", "__GLOBAL__", policyKey, "", "MONTH", monthStart(requestTime));
        }
    }

    private com.bigcomp.accesscontrol.profile.UsageLimit resolveUsageLimit(Profile profile, Resource resource, String groupName) {
        if (profile == null) {
            return null;
        }
        if (resource != null && profile.getUsageLimitsByResourceType() != null) {
            com.bigcomp.accesscontrol.profile.UsageLimit byType = profile.getUsageLimitsByResourceType().get(resource.getType().name());
            if (byType != null) {
                return byType;
            }
        }
        if (profile.getUsageLimitsByGroup() != null) {
            return profile.getUsageLimitsByGroup().get(groupName);
        }
        return null;
    }

    private String buildPolicyKey(Profile profile, Resource resource, String groupName) {
        if (profile != null && resource != null && profile.getUsageLimitsByResourceType() != null && profile.getUsageLimitsByResourceType().containsKey(resource.getType().name())) {
            return "TYPE:" + resource.getType().name();
        }
        return "GROUP:" + groupName;
    }

    private String dayStart(LocalDateTime time) {
        return time.toLocalDate().toString();
    }

    private String weekStart(LocalDateTime time) {
        WeekFields wf = WeekFields.ISO;
        int week = time.get(wf.weekOfWeekBasedYear());
        int year = time.get(wf.weekBasedYear());
        return String.format("%04d-W%02d", year, week);
    }

    private String monthStart(LocalDateTime time) {
        return String.format("%04d-%02d", time.getYear(), time.getMonthValue());
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
        updateBadge(null, badge);
    }

    public void updateBadge(String oldBadgeCode, Badge badge) {
        try {
            // 1. Update Database
            dbManager.updateBadge(badge);
            
            // 2. Update Cache
            updateBadgeCache(oldBadgeCode, badge);
        } catch (Exception e) {
            System.err.println("Failed to update badge: " + e.getMessage());
        }
    }

    /**
     * Update cache for a single badge without full reload
     * Used when a badge is updated (e.g. rotated code)
     */
    private void updateBadgeCache(String oldBadgeCode, Badge badge) {
        AccessControlContext current = this.context;
        
        // 1. Create copies of maps that need update
        Map<String, Badge> newBadges = new HashMap<>(current.badgesByCode);
        Map<String, User> newUsersByCode = new HashMap<>(current.usersByBadgeCode);

        if (oldBadgeCode != null && !oldBadgeCode.isBlank() && !oldBadgeCode.equals(badge.getCode())) {
            newBadges.remove(oldBadgeCode);
            newUsersByCode.remove(oldBadgeCode);
        }
        
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
