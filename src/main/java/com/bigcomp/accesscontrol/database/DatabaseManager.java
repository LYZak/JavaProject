package com.bigcomp.accesscontrol.database;

import com.bigcomp.accesscontrol.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据库管理器 - 负责所有数据库操作
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/access_control.db";
    private Connection connection;

    public DatabaseManager() {
        initializeDatabase();
    }

    /**
     * 初始化数据库，创建所有必要的表
     */
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建所有数据库表
     */
    private void createTables() throws SQLException {
        // 用户表
        executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
            "id TEXT PRIMARY KEY, " +
            "gender TEXT, " +
            "first_name TEXT, " +
            "last_name TEXT, " +
            "user_type TEXT, " +
            "badge_id TEXT" +
            ")");

        // 徽章表
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

        // 资源表
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

        // 读卡器表
        executeUpdate("CREATE TABLE IF NOT EXISTS badge_readers (" +
            "id TEXT PRIMARY KEY, " +
            "resource_id TEXT, " +
            "FOREIGN KEY (resource_id) REFERENCES resources(id)" +
            ")");

        // 资源组表
        executeUpdate("CREATE TABLE IF NOT EXISTS resource_groups (" +
            "name TEXT PRIMARY KEY, " +
            "security_level INTEGER, " +
            "file_path TEXT" +
            ")");

        // 配置文件表
        executeUpdate("CREATE TABLE IF NOT EXISTS profiles (" +
            "name TEXT PRIMARY KEY, " +
            "file_path TEXT" +
            ")");

        // 徽章-配置文件关联表（多对多）
        executeUpdate("CREATE TABLE IF NOT EXISTS badge_profiles (" +
            "badge_id TEXT, " +
            "profile_name TEXT, " +
            "PRIMARY KEY (badge_id, profile_name), " +
            "FOREIGN KEY (badge_id) REFERENCES badges(id), " +
            "FOREIGN KEY (profile_name) REFERENCES profiles(name)" +
            ")");

        // 资源-组关联表（多对多）
        executeUpdate("CREATE TABLE IF NOT EXISTS resource_group_members (" +
            "resource_id TEXT, " +
            "group_name TEXT, " +
            "PRIMARY KEY (resource_id, group_name), " +
            "FOREIGN KEY (resource_id) REFERENCES resources(id), " +
            "FOREIGN KEY (group_name) REFERENCES resource_groups(name)" +
            ")");
    }

    /**
     * 执行更新操作
     */
    private void executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * 添加用户
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
     * 添加徽章
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
     * 添加资源
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
     * 添加读卡器
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
     * 加载所有用户（通过徽章代码索引）
     * 只返回有徽章的用户
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
            System.err.println("加载用户失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 加载所有用户（包括没有徽章的用户）
     * @return 用户ID -> 用户对象的映射
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
            System.err.println("加载所有用户失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 根据徽章ID加载徽章
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
            System.err.println("加载徽章失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 根据用户ID加载徽章
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
            System.err.println("加载用户徽章失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 加载所有徽章（通过用户ID索引）
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
            System.err.println("加载所有徽章失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 加载用户配置文件映射
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
            System.err.println("加载用户配置文件失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 加载所有资源
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
            System.err.println("加载资源失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 加载资源组映射
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
            System.err.println("加载资源组失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 记录访问日志
     * 注意：实际日志记录由LogManager处理，此方法保留用于兼容性
     */
    public void logAccess(AccessRequest request, User user, Resource resource, boolean granted) {
        // 日志记录由LogManager处理，这里只是占位
    }

    /**
     * 关联徽章和配置文件
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
     * 关联资源和组
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
     * 删除用户
     * 会同时删除关联的徽章和配置文件关联
     */
    public void deleteUser(String userId) throws SQLException {
        // 先获取用户的徽章ID
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
        
        // 如果用户有徽章，先删除徽章相关的关联
        if (badgeId != null) {
            // 删除徽章-配置文件关联
            sql = "DELETE FROM badge_profiles WHERE badge_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, badgeId);
                pstmt.executeUpdate();
            }
            
            // 删除徽章
            sql = "DELETE FROM badges WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, badgeId);
                pstmt.executeUpdate();
            }
        }
        
        // 最后删除用户
        sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * 删除资源
     * 会同时删除关联的读卡器和资源组关联
     */
    public void deleteResource(String resourceId) throws SQLException {
        // 先删除资源-组关联
        String sql = "DELETE FROM resource_group_members WHERE resource_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, resourceId);
            pstmt.executeUpdate();
        }
        
        // 获取关联的读卡器ID
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
        
        // 删除读卡器
        if (badgeReaderId != null) {
            sql = "DELETE FROM badge_readers WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, badgeReaderId);
                pstmt.executeUpdate();
            }
        }
        
        // 最后删除资源
        sql = "DELETE FROM resources WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, resourceId);
            pstmt.executeUpdate();
        }
    }

    /**
     * 获取连接（用于复杂查询）
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 关闭数据库连接
     */
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}

