// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.database;

import com.bigcomp.accesscontrol.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Database Manager - Handles all database operations
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/access_control.db";
    private Connection connection;

    public DatabaseManager() {
        initializeDatabase();
        updateResourceGroupNames();
    }
    
    /**
     * Update resource group names from Chinese to English in database
     */
    private void updateResourceGroupNames() {
        Map<String, String> nameMapping = new HashMap<>();
        nameMapping.put("设备资源", "Equipment Resources");
        nameMapping.put("公共区域", "Public Area");
        nameMapping.put("办公区域", "Office Area");
        nameMapping.put("高安全区域", "High Security Area");
        nameMapping.put("停车场区域", "Parking Area");
        nameMapping.put("楼梯区域", "Stairway Area");
        
        try {
            for (Map.Entry<String, String> entry : nameMapping.entrySet()) {
                String chineseName = entry.getKey();
                String englishName = entry.getValue();
                
                // Check if Chinese name exists
                String checkSql = "SELECT COUNT(*) FROM resource_group_members WHERE group_name = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setString(1, chineseName);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Update to English name
                            String updateSql = "UPDATE resource_group_members SET group_name = ? WHERE group_name = ?";
                            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                                updateStmt.setString(1, englishName);
                                updateStmt.setString(2, chineseName);
                                updateStmt.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Ignore errors, table may not exist or already updated
            System.err.println("Note: Could not update resource group names: " + e.getMessage());
        }
    }

    /**
     * Initialize database, create all necessary tables
     */
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create all database tables
     */
    private void createTables() throws SQLException {
        // Users table
        executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
            "id TEXT PRIMARY KEY, " +
            "gender TEXT, " +
            "first_name TEXT, " +
            "last_name TEXT, " +
            "user_type TEXT, " +
            "badge_id TEXT" +
            ")");

        // Badges table
        executeUpdate("CREATE TABLE IF NOT EXISTS badges (" +
            "id TEXT PRIMARY KEY, " +
            "code TEXT UNIQUE, " +
            "user_id TEXT, " +
            "creation_date TEXT, " +
            "expiration_date TEXT, " +
            "last_update_date TEXT, " +
            "valid INTEGER, " +
            "FOREIGN KEY (user_id) REFERENCES users(id)" +
            ")");

        // Resources table
        executeUpdate("CREATE TABLE IF NOT EXISTS resources (" +
            "id TEXT PRIMARY KEY, " +
            "name TEXT, " +
            "type TEXT, " +
            "location TEXT, " +
            "building TEXT, " +
            "floor TEXT, " +
            "state TEXT, " +
            "badge_reader_id TEXT" +
            ")");

        // Badge readers table
        executeUpdate("CREATE TABLE IF NOT EXISTS badge_readers (" +
            "id TEXT PRIMARY KEY, " +
            "resource_id TEXT, " +
            "FOREIGN KEY (resource_id) REFERENCES resources(id)" +
            ")");

        // Resource groups table
        executeUpdate("CREATE TABLE IF NOT EXISTS resource_groups (" +
            "name TEXT PRIMARY KEY, " +
            "security_level INTEGER, " +
            "file_path TEXT" +
            ")");

        // Profiles table
        executeUpdate("CREATE TABLE IF NOT EXISTS profiles (" +
            "name TEXT PRIMARY KEY, " +
            "file_path TEXT" +
            ")");

        // Badge-Profile association table (many-to-many)
        executeUpdate("CREATE TABLE IF NOT EXISTS badge_profiles (" +
            "badge_id TEXT, " +
            "profile_name TEXT, " +
            "PRIMARY KEY (badge_id, profile_name), " +
            "FOREIGN KEY (badge_id) REFERENCES badges(id), " +
            "FOREIGN KEY (profile_name) REFERENCES profiles(name)" +
            ")");

        // Resource-Group association table (many-to-many)
        executeUpdate("CREATE TABLE IF NOT EXISTS resource_group_members (" +
            "resource_id TEXT, " +
            "group_name TEXT, " +
            "PRIMARY KEY (resource_id, group_name), " +
            "FOREIGN KEY (resource_id) REFERENCES resources(id), " +
            "FOREIGN KEY (group_name) REFERENCES resource_groups(name)" +
            ")");
    }

    /**
     * Execute update operation
     */
    private void executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * Add user
     */
    public void addUser(User user) throws SQLException {
        String sql = "INSERT OR REPLACE INTO users (id, gender, first_name, last_name, user_type, badge_id) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getGender().toString());
            pstmt.setString(3, user.getFirstName());
            pstmt.setString(4, user.getLastName());
            pstmt.setString(5, user.getUserType().toString());
            pstmt.setString(6, user.getBadgeId());
            pstmt.executeUpdate();
        }
    }

    /**
     * Add badge
     */
    public void addBadge(Badge badge, String badgeId) throws SQLException {
        String sql = "INSERT OR REPLACE INTO badges (id, code, user_id, creation_date, expiration_date, last_update_date, valid) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, badgeId);
            pstmt.setString(2, badge.getCode());
            pstmt.setString(3, badge.getUserId());
            pstmt.setString(4, badge.getCreationDate().toString());
            pstmt.setString(5, badge.getExpirationDate().toString());
            pstmt.setString(6, badge.getLastUpdateDate().toString());
            pstmt.setInt(7, badge.isValid() ? 1 : 0);
            pstmt.executeUpdate();
        }
    }

    /**
     * Add resource
     */
    public void addResource(Resource resource) throws SQLException {
        String sql = "INSERT OR REPLACE INTO resources (id, name, type, location, building, floor, state, badge_reader_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, resource.getId());
            pstmt.setString(2, resource.getName());
            pstmt.setString(3, resource.getType().toString());
            pstmt.setString(4, resource.getLocation());
            pstmt.setString(5, resource.getBuilding());
            pstmt.setString(6, resource.getFloor());
            pstmt.setString(7, resource.getState().toString());
            pstmt.setString(8, resource.getBadgeReaderId());
            pstmt.executeUpdate();
        }
    }

    /**
     * Add badge reader
     */
    public void addBadgeReader(BadgeReader reader) throws SQLException {
        String sql = "INSERT OR REPLACE INTO badge_readers (id, resource_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reader.getId());
            pstmt.setString(2, reader.getResourceId());
            pstmt.executeUpdate();
        }
    }

    /**
     * Load all users (indexed by badge code)
     * Only returns users with badges
     */
    public Map<String, User> loadUsersByBadgeCode() {
        Map<String, User> result = new HashMap<>();
        try {
            String sql = "SELECT u.*, b.code FROM users u " +
                "JOIN badges b ON u.badge_id = b.id";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    User user = new User(
                        rs.getString("id"),
                        User.Gender.valueOf(rs.getString("gender")),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        User.UserType.valueOf(rs.getString("user_type"))
                    );
                    user.setBadgeId(rs.getString("badge_id"));
                    result.put(rs.getString("code"), user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load users: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * Load all users (including users without badges)
     * @return Map of user ID -> User object
     */
    public Map<String, User> loadAllUsers() {
        Map<String, User> result = new HashMap<>();
        try {
            String sql = "SELECT * FROM users";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    User user = new User(
                        rs.getString("id"),
                        User.Gender.valueOf(rs.getString("gender")),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        User.UserType.valueOf(rs.getString("user_type"))
                    );
                    user.setBadgeId(rs.getString("badge_id"));
                    result.put(user.getId(), user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load all users: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * Load badge by badge ID
     */
    public Badge loadBadgeById(String badgeId) {
        try {
            String sql = "SELECT * FROM badges WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, badgeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Badge badge = new Badge(
                            rs.getString("code"),
                            rs.getString("user_id"),
                            LocalDateTime.parse(rs.getString("creation_date")),
                            LocalDateTime.parse(rs.getString("expiration_date")),
                            LocalDateTime.parse(rs.getString("last_update_date")),
                            rs.getInt("valid") == 1
                        );
                        return badge;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load badge: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Load badge by user ID
     */
    public Badge loadBadgeByUserId(String userId) {
        try {
            String sql = "SELECT b.* FROM badges b " +
                "JOIN users u ON b.id = u.badge_id WHERE u.id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Badge badge = new Badge(
                            rs.getString("code"),
                            rs.getString("user_id"),
                            LocalDateTime.parse(rs.getString("creation_date")),
                            LocalDateTime.parse(rs.getString("expiration_date")),
                            LocalDateTime.parse(rs.getString("last_update_date")),
                            rs.getInt("valid") == 1
                        );
                        return badge;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load user badge: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Load all badges (indexed by user ID)
     */
    public Map<String, Badge> loadAllBadges() {
        Map<String, Badge> result = new HashMap<>();
        try {
            String sql = "SELECT * FROM badges";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Badge badge = new Badge(
                        rs.getString("code"),
                        rs.getString("user_id"),
                        LocalDateTime.parse(rs.getString("creation_date")),
                        LocalDateTime.parse(rs.getString("expiration_date")),
                        LocalDateTime.parse(rs.getString("last_update_date")),
                        rs.getInt("valid") == 1
                    );
                    result.put(rs.getString("user_id"), badge);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load all badges: " + e.getMessage());
        }
        return result;
    }

    /**
     * Load user profile mapping
     */
    public Map<String, Set<String>> loadUserProfiles() {
        Map<String, Set<String>> result = new HashMap<>();
        try {
            String sql = "SELECT b.user_id, bp.profile_name FROM badges b " +
                "JOIN badge_profiles bp ON b.id = bp.badge_id";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String userId = rs.getString("user_id");
                    String profileName = rs.getString("profile_name");
                    result.computeIfAbsent(userId, k -> new HashSet<>()).add(profileName);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load user profiles: " + e.getMessage());
        }
        return result;
    }

    /**
     * Load all resources
     */
    public Map<String, Resource> loadAllResources() {
        Map<String, Resource> result = new HashMap<>();
        try {
            String sql = "SELECT * FROM resources";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Resource resource = new Resource(
                        rs.getString("id"),
                        rs.getString("name"),
                        Resource.ResourceType.valueOf(rs.getString("type")),
                        rs.getString("location"),
                        rs.getString("building"),
                        rs.getString("floor")
                    );
                    resource.setState(Resource.ResourceState.valueOf(rs.getString("state")));
                    resource.setBadgeReaderId(rs.getString("badge_reader_id"));
                    result.put(resource.getId(), resource);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load resources: " + e.getMessage());
        }
        return result;
    }

    /**
     * Load resource group mapping
     */
    public Map<String, String> loadResourceGroups() {
        Map<String, String> result = new HashMap<>();
        try {
            String sql = "SELECT resource_id, group_name FROM resource_group_members";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    result.put(rs.getString("resource_id"), rs.getString("group_name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load resource groups: " + e.getMessage());
        }
        return result;
    }

    /**
     * Log access event
     * Note: Actual logging is handled by LogManager, this method is kept for compatibility
     */
    public void logAccess(AccessRequest request, User user, Resource resource, boolean granted) {
        // Logging is handled by LogManager, this is just a placeholder
    }

    /**
     * Link badge to profile
     */
    public void linkBadgeToProfile(String badgeId, String profileName) throws SQLException {
        String sql = "INSERT OR IGNORE INTO badge_profiles (badge_id, profile_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, badgeId);
            pstmt.setString(2, profileName);
            pstmt.executeUpdate();
        }
    }

    /**
     * Link resource to group
     */
    public void linkResourceToGroup(String resourceId, String groupName) throws SQLException {
        String sql = "INSERT OR IGNORE INTO resource_group_members (resource_id, group_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, resourceId);
            pstmt.setString(2, groupName);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Delete user
     * Also deletes associated badges and profile associations
     */
    public void deleteUser(String userId) throws SQLException {
        // First get the user's badge ID
        String badgeId = null;
        String sql = "SELECT badge_id FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    badgeId = rs.getString("badge_id");
                }
            }
        }
        
        // If user has badge, first delete badge-related associations
        if (badgeId != null) {
            // Delete badge-profile associations
            sql = "DELETE FROM badge_profiles WHERE badge_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, badgeId);
                pstmt.executeUpdate();
            }
            
            // Delete badge
            sql = "DELETE FROM badges WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, badgeId);
                pstmt.executeUpdate();
            }
        }
        
        // Finally delete user
        sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Delete resource
     * Also deletes associated badge readers and resource group associations
     */
    public void deleteResource(String resourceId) throws SQLException {
        // First delete resource-group associations
        String sql = "DELETE FROM resource_group_members WHERE resource_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, resourceId);
            pstmt.executeUpdate();
        }
        
        // Get associated badge reader ID
        String badgeReaderId = null;
        sql = "SELECT badge_reader_id FROM resources WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, resourceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    badgeReaderId = rs.getString("badge_reader_id");
                }
            }
        }
        
        // Delete badge reader
        if (badgeReaderId != null) {
            sql = "DELETE FROM badge_readers WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, badgeReaderId);
                pstmt.executeUpdate();
            }
        }
        
        // Finally delete resource
        sql = "DELETE FROM resources WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, resourceId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Get connection (for complex queries)
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Close database connection
     */
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}

