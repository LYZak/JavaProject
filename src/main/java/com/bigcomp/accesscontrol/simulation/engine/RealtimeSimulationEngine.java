package com.bigcomp.accesscontrol.simulation.engine;

import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.util.SystemClock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RealtimeSimulationEngine implements SimulationEngine {
    private final List<BadgeReader> badgeReaders;
    private final Map<String, Resource> resourcesById;
    private final List<SimulatedUser> simulatedUsers = new ArrayList<>();
    private final Random random;
    private final SimulationMetrics metrics;

    private volatile boolean running;
    private ScheduledExecutorService executor;
    private int intervalSeconds = 2;
    private boolean consistentBehavior = true;

    public RealtimeSimulationEngine(List<BadgeReader> badgeReaders, Map<String, Resource> resourcesById, long seed, SimulationMetrics metrics) {
        this.badgeReaders = badgeReaders;
        this.resourcesById = resourcesById != null ? resourcesById : new HashMap<>();
        this.random = new Random(seed);
        this.metrics = metrics;
    }

    public void setIntervalSeconds(int seconds) {
        this.intervalSeconds = Math.max(1, Math.min(60, seconds));
    }

    public void setConsistentBehavior(boolean enabled) {
        this.consistentBehavior = enabled;
    }

    public void clearUsers() {
        simulatedUsers.clear();
    }

    public void addSimulatedUser(User user, Badge badge) {
        simulatedUsers.add(new SimulatedUser(user, badge));
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::tick, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void tick() {
        if (!running) {
            return;
        }
        if (simulatedUsers.isEmpty() || badgeReaders.isEmpty()) {
            return;
        }

        SimulatedUser simUser = simulatedUsers.get(random.nextInt(simulatedUsers.size()));
        BadgeReader reader = consistentBehavior ? selectNextReader(simUser) : badgeReaders.get(random.nextInt(badgeReaders.size()));
        if (reader == null) {
            return;
        }

        Resource resource = resourcesById.get(reader.getResourceId());
        if (resource != null) {
            simUser.lastLocation = resource.getLocation();
            simUser.lastAccessTime = SystemClock.now();
        }

        Badge badge = simUser.badge;
        if (badge == null) {
            return;
        }

        metrics.recordSubmitted();

        if (badge.needsUpdate()) {
            if (random.nextDouble() < 0.8) {
                reader.updateBadge(badge);
            } else {
                reader.swipeBadge(badge);
            }
        } else {
            reader.swipeBadge(badge);
        }
    }

    private BadgeReader selectNextReader(SimulatedUser simUser) {
        String lastLocation = simUser.lastLocation;
        List<BadgeReader> candidates = new ArrayList<>();

        if (lastLocation == null || lastLocation.isEmpty()) {
            for (BadgeReader reader : badgeReaders) {
                Resource res = resourcesById.get(reader.getResourceId());
                if (res != null && res.getType() == Resource.ResourceType.GATE) {
                    candidates.add(reader);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }

        if (lastLocation != null && (lastLocation.contains("Site") || lastLocation.contains("Parking"))) {
            for (BadgeReader reader : badgeReaders) {
                Resource res = resourcesById.get(reader.getResourceId());
                if (res != null && res.getType() == Resource.ResourceType.DOOR && res.getFloor() != null && res.getFloor().contains("1")) {
                    candidates.add(reader);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }

        if (lastLocation != null && lastLocation.contains("Main Office Building")) {
            for (BadgeReader reader : badgeReaders) {
                Resource res = resourcesById.get(reader.getResourceId());
                if (res != null && (res.getName().contains("Office")
                    || res.getName().contains("Meeting Room")
                    || res.getType() == Resource.ResourceType.ELEVATOR
                    || res.getType() == Resource.ResourceType.STAIRWAY
                    || res.getType() == Resource.ResourceType.PRINTER
                    || res.getType() == Resource.ResourceType.BEVERAGE_DISPENSER)) {
                    candidates.add(reader);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }

        return badgeReaders.get(random.nextInt(badgeReaders.size()));
    }

    private static final class SimulatedUser {
        private final User user;
        private final Badge badge;
        private java.time.LocalDateTime lastAccessTime;
        private String lastLocation;

        private SimulatedUser(User user, Badge badge) {
            this.user = user;
            this.badge = badge;
        }
    }
}

