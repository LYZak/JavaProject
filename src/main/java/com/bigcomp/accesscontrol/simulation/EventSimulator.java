// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.simulation;

import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.util.SystemClock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Event Simulator - Simulates user system activities
 */
public class EventSimulator {
    private List<SimulatedUser> simulatedUsers;
    private List<BadgeReader> badgeReaders;
    private Map<String, Resource> resourceMap; // Resource ID -> Resource object
    private ScheduledExecutorService executor;
    private Random random;
    private boolean running;
    private int intervalSeconds = 2; // Default interval 2 seconds
    private boolean consistentBehavior = true; // Whether to enable consistent behavior pattern

    public EventSimulator(List<BadgeReader> badgeReaders) {
        this.badgeReaders = badgeReaders;
        this.simulatedUsers = new ArrayList<>();
        this.resourceMap = new HashMap<>();
        this.executor = Executors.newScheduledThreadPool(10);
        this.random = new Random();
        this.running = false;
        
        // Load resource map
        loadResourceMap();
    }
    
    /**
     * Load resource map
     */
    private void loadResourceMap() {
        try {
            DatabaseManager dbManager = new DatabaseManager();
            resourceMap = dbManager.loadAllResources();
        } catch (Exception e) {
            System.err.println("Failed to load resource map: " + e.getMessage());
        }
    }
    
    /**
     * Set whether to enable consistent behavior pattern
     */
    public void setConsistentBehavior(boolean enabled) {
        this.consistentBehavior = enabled;
    }
    
    /**
     * Set event generation interval (seconds)
     */
    public void setInterval(int seconds) {
        this.intervalSeconds = Math.max(1, Math.min(60, seconds)); // Limit between 1-60 seconds
    }

    /**
     * Add simulated user
     */
    public void addSimulatedUser(User user, Badge badge) {
        simulatedUsers.add(new SimulatedUser(user, badge));
    }

    /**
     * Start simulation
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        // Periodically generate access events
        executor.scheduleAtFixedRate(() -> {
            if (!running) {
                return;
            }
            generateAccessEvent();
        }, 0, intervalSeconds, TimeUnit.SECONDS); // Use configurable interval
    }

    /**
     * Stop simulation
     */
    public void stop() {
        running = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }
    
    /**
     * Check if simulation is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Generate access event (supports consistent behavior pattern)
     */
    private void generateAccessEvent() {
        if (simulatedUsers.isEmpty() || badgeReaders.isEmpty()) {
            return;
        }

        SimulatedUser simUser = simulatedUsers.get(random.nextInt(simulatedUsers.size()));
        BadgeReader reader;
        
        if (consistentBehavior && simUser.getLastLocation() != null) {
            // Consistent behavior pattern: select next reasonable badge reader based on user's current location
            reader = selectNextReader(simUser);
        } else {
            // Random mode
            reader = badgeReaders.get(random.nextInt(badgeReaders.size()));
        }
        
        if (reader != null) {
            // Update user location
            Resource resource = resourceMap.get(reader.getResourceId());
            if (resource != null) {
                simUser.setLastLocation(resource.getLocation());
                simUser.setLastAccessTime(SystemClock.now());
            }
            
            // Simulate badge swipe
            reader.swipeBadge(simUser.getBadge());
        }
    }
    
    /**
     * Select next reasonable badge reader based on user's current location
     * Implements consistent behavior: first enter site entrance, then building, finally office
     */
    private BadgeReader selectNextReader(SimulatedUser simUser) {
        String lastLocation = simUser.getLastLocation();
        List<BadgeReader> candidates = new ArrayList<>();
        
        // If user has no access record yet, prioritize site entrance (GATE type)
        if (lastLocation == null || lastLocation.isEmpty()) {
            for (BadgeReader reader : badgeReaders) {
                Resource res = resourceMap.get(reader.getResourceId());
                if (res != null && res.getType() == Resource.ResourceType.GATE) {
                    candidates.add(reader);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }
        
        // If user is already on site, prioritize building entrance (DOOR type, 1st floor)
        if (lastLocation != null && (lastLocation.contains("Site") || lastLocation.contains("Parking"))) {
            for (BadgeReader reader : badgeReaders) {
                Resource res = resourceMap.get(reader.getResourceId());
                if (res != null && 
                    res.getType() == Resource.ResourceType.DOOR && 
                    res.getFloor() != null && res.getFloor().contains("1")) {
                    candidates.add(reader);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }
        
        // If user is already in building, can choose office, elevator, stairway, etc.
        if (lastLocation != null && lastLocation.contains("Main Office Building")) {
            for (BadgeReader reader : badgeReaders) {
                Resource res = resourceMap.get(reader.getResourceId());
                if (res != null && 
                    (res.getName().contains("Office") || 
                     res.getName().contains("Meeting Room") ||
                     res.getType() == Resource.ResourceType.ELEVATOR ||
                     res.getType() == Resource.ResourceType.STAIRWAY ||
                     res.getType() == Resource.ResourceType.PRINTER ||
                     res.getType() == Resource.ResourceType.BEVERAGE_DISPENSER)) {
                    candidates.add(reader);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }
        
        // If no suitable one found, randomly select one
        return badgeReaders.get(random.nextInt(badgeReaders.size()));
    }

    /**
     * Simulated user class
     */
    private static class SimulatedUser {
        private User user;
        private Badge badge;
        private LocalDateTime lastAccessTime;
        private String lastLocation;

        public SimulatedUser(User user, Badge badge) {
            this.user = user;
            this.badge = badge;
        }

        public User getUser() {
            return user;
        }

        public Badge getBadge() {
            return badge;
        }

        public LocalDateTime getLastAccessTime() {
            return lastAccessTime;
        }

        public void setLastAccessTime(LocalDateTime lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }

        public String getLastLocation() {
            return lastLocation;
        }

        public void setLastLocation(String lastLocation) {
            this.lastLocation = lastLocation;
        }
    }
}

