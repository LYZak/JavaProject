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
 * 事件模拟器 - 模拟用户使用系统的活动
 */
public class EventSimulator {
    private List<SimulatedUser> simulatedUsers;
    private List<BadgeReader> badgeReaders;
    private Map<String, Resource> resourceMap; // 资源ID -> 资源对象
    private ScheduledExecutorService executor;
    private Random random;
    private boolean running;
    private int intervalSeconds = 2; // 默认间隔2秒
    private boolean consistentBehavior = true; // 是否启用一致的行为模式

    public EventSimulator(List<BadgeReader> badgeReaders) {
        this.badgeReaders = badgeReaders;
        this.simulatedUsers = new ArrayList<>();
        this.resourceMap = new HashMap<>();
        this.executor = Executors.newScheduledThreadPool(10);
        this.random = new Random();
        this.running = false;
        
        // 加载资源映射
        loadResourceMap();
    }
    
    /**
     * 加载资源映射
     */
    private void loadResourceMap() {
        try {
            DatabaseManager dbManager = new DatabaseManager();
            resourceMap = dbManager.loadAllResources();
        } catch (Exception e) {
            System.err.println("加载资源映射失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置是否启用一致的行为模式
     */
    public void setConsistentBehavior(boolean enabled) {
        this.consistentBehavior = enabled;
    }
    
    /**
     * 设置事件生成间隔（秒）
     */
    public void setInterval(int seconds) {
        this.intervalSeconds = Math.max(1, Math.min(60, seconds)); // 限制在1-60秒之间
    }

    /**
     * 添加模拟用户
     */
    public void addSimulatedUser(User user, Badge badge) {
        simulatedUsers.add(new SimulatedUser(user, badge));
    }

    /**
     * 开始模拟
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        // 定期生成访问事件
        executor.scheduleAtFixedRate(() -> {
            if (!running) {
                return;
            }
            generateAccessEvent();
        }, 0, intervalSeconds, TimeUnit.SECONDS); // 使用可配置的间隔
    }

    /**
     * 停止模拟
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
     * 检查模拟是否正在运行
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 生成访问事件（支持一致的行为模式）
     */
    private void generateAccessEvent() {
        if (simulatedUsers.isEmpty() || badgeReaders.isEmpty()) {
            return;
        }

        SimulatedUser simUser = simulatedUsers.get(random.nextInt(simulatedUsers.size()));
        BadgeReader reader;
        
        if (consistentBehavior && simUser.getLastLocation() != null) {
            // 一致的行为模式：根据用户当前位置选择下一个合理的读卡器
            reader = selectNextReader(simUser);
        } else {
            // 随机模式
            reader = badgeReaders.get(random.nextInt(badgeReaders.size()));
        }
        
        if (reader != null) {
            // 更新用户位置
            Resource resource = resourceMap.get(reader.getResourceId());
            if (resource != null) {
                simUser.setLastLocation(resource.getLocation());
                simUser.setLastAccessTime(SystemClock.now());
            }
            
            // 模拟刷卡
            reader.swipeBadge(simUser.getBadge());
        }
    }
    
    /**
     * 根据用户当前位置选择下一个合理的读卡器
     * 实现一致的行为：先进入场地入口，再进入建筑，最后进入办公室
     */
    private BadgeReader selectNextReader(SimulatedUser simUser) {
        String lastLocation = simUser.getLastLocation();
        List<BadgeReader> candidates = new ArrayList<>();
        
        // 如果用户还没有访问记录，优先选择场地入口（GATE类型）
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
        
        // 如果用户已经在场地，优先选择建筑入口（DOOR类型，1楼）
        if (lastLocation != null && (lastLocation.contains("场地") || lastLocation.contains("停车场"))) {
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
        
        // 如果用户已经在建筑内，可以选择办公室、电梯、楼梯等
        if (lastLocation != null && lastLocation.contains("主办公楼")) {
            for (BadgeReader reader : badgeReaders) {
                Resource res = resourceMap.get(reader.getResourceId());
                if (res != null && 
                    (res.getName().contains("办公室") || 
                     res.getName().contains("会议室") ||
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
        
        // 如果没有找到合适的，随机选择一个
        return badgeReaders.get(random.nextInt(badgeReaders.size()));
    }

    /**
     * 模拟用户类
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

