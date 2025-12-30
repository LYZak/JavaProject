// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.util;

import com.bigcomp.accesscontrol.database.DatabaseManager;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Database Export Tool - Exports SQLite database to MySQL format SQL file
 */
public class DatabaseExporter {
    private DatabaseManager dbManager;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DatabaseExporter(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Export database to MySQL format SQL file
     */
    public void exportToMySQL(String outputFile) throws IOException, SQLException {
        Connection conn = dbManager.getConnection();
        
        try (FileWriter writer = new FileWriter(outputFile, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write("-- BigComp Access Control System Database\n");
            writer.write("-- Exported from SQLite to MySQL format\n");
            writer.write("-- Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER) + "\n\n");
            
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
            writer.write("DROP DATABASE IF EXISTS access_control;\n");
            writer.write("CREATE DATABASE access_control CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n");
            writer.write("USE access_control;\n\n");
            
            // Export table structure
            exportTableStructure(conn, writer);
            
            // Export table data
            exportTableData(conn, writer);
            
            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
        }
    }

    private void exportTableStructure(Connection conn, FileWriter writer) throws SQLException, IOException {
        writer.write("-- ============================================\n");
        writer.write("-- Table Structure\n");
        writer.write("-- ============================================\n\n");
        
        String[] tables = {"users", "badges", "resources", "badge_readers", 
                          "badge_profiles", "resource_group_members"};
        
        for (String tableName : tables) {
            writer.write("-- Table: " + tableName + "\n");
            writer.write("DROP TABLE IF EXISTS `" + tableName + "`;\n");
            
            // Get SQLite table structure and convert to MySQL format
            String createTableSQL = getMySQLCreateTable(conn, tableName);
            if (createTableSQL != null) {
                writer.write(createTableSQL + "\n\n");
            }
        }
    }

    private String getMySQLCreateTable(Connection conn, String tableName) throws SQLException {
        // Generate MySQL CREATE TABLE statement based on table name
        switch (tableName) {
            case "users":
                return "CREATE TABLE `users` (\n" +
                       "  `id` VARCHAR(50) PRIMARY KEY,\n" +
                       "  `first_name` VARCHAR(100) NOT NULL,\n" +
                       "  `last_name` VARCHAR(100) NOT NULL,\n" +
                       "  `gender` VARCHAR(20) NOT NULL,\n" +
                       "  `user_type` VARCHAR(50) NOT NULL,\n" +
                       "  `badge_id` VARCHAR(50),\n" +
                       "  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            
            case "badges":
                return "CREATE TABLE `badges` (\n" +
                       "  `id` VARCHAR(50) PRIMARY KEY,\n" +
                       "  `code` VARCHAR(50) UNIQUE NOT NULL,\n" +
                       "  `user_id` VARCHAR(50) NOT NULL,\n" +
                       "  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                       "  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE\n" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            
            case "resources":
                return "CREATE TABLE `resources` (\n" +
                       "  `id` VARCHAR(50) PRIMARY KEY,\n" +
                       "  `name` VARCHAR(200) NOT NULL,\n" +
                       "  `type` VARCHAR(50) NOT NULL,\n" +
                       "  `location` VARCHAR(200),\n" +
                       "  `building` VARCHAR(100),\n" +
                       "  `floor` VARCHAR(50),\n" +
                       "  `state` VARCHAR(20) DEFAULT 'CONTROLLED',\n" +
                       "  `badge_reader_id` VARCHAR(50)\n" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            
            case "badge_readers":
                return "CREATE TABLE `badge_readers` (\n" +
                       "  `id` VARCHAR(50) PRIMARY KEY,\n" +
                       "  `resource_id` VARCHAR(50) NOT NULL,\n" +
                       "  `is_active` BOOLEAN DEFAULT TRUE,\n" +
                       "  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE CASCADE\n" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            
            case "badge_profiles":
                return "CREATE TABLE `badge_profiles` (\n" +
                       "  `badge_id` VARCHAR(50) NOT NULL,\n" +
                       "  `profile_name` VARCHAR(100) NOT NULL,\n" +
                       "  PRIMARY KEY (`badge_id`, `profile_name`),\n" +
                       "  FOREIGN KEY (`badge_id`) REFERENCES `badges`(`id`) ON DELETE CASCADE\n" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            
            case "resource_group_members":
                return "CREATE TABLE `resource_group_members` (\n" +
                       "  `resource_id` VARCHAR(50) NOT NULL,\n" +
                       "  `group_name` VARCHAR(100) NOT NULL,\n" +
                       "  PRIMARY KEY (`resource_id`, `group_name`),\n" +
                       "  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE CASCADE\n" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            
            default:
                return null;
        }
    }

    private void exportTableData(Connection conn, FileWriter writer) throws SQLException, IOException {
        writer.write("-- ============================================\n");
        writer.write("-- Table Data\n");
        writer.write("-- ============================================\n\n");
        
        String[] tables = {"users", "badges", "resources", "badge_readers", 
                          "badge_profiles", "resource_group_members"};
        
        for (String tableName : tables) {
            exportTableData(conn, writer, tableName);
        }
    }

    private void exportTableData(Connection conn, FileWriter writer, String tableName) 
            throws SQLException, IOException {
        String sql = "SELECT * FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            int rowCount = 0;
            while (rs.next()) {
                if (rowCount == 0) {
                    writer.write("-- Data for table: " + tableName + "\n");
                }
                
                writer.write("INSERT INTO `" + tableName + "` (");
                for (int i = 1; i <= columnCount; i++) {
                    writer.write("`" + metaData.getColumnName(i) + "`");
                    if (i < columnCount) writer.write(", ");
                }
                writer.write(") VALUES (");
                
                for (int i = 1; i <= columnCount; i++) {
                    String columnType = metaData.getColumnTypeName(i).toUpperCase();
                    Object value = rs.getObject(i);
                    
                    if (value == null) {
                        writer.write("NULL");
                    } else if (columnType.contains("INT") || columnType.contains("BOOLEAN")) {
                        writer.write(value.toString());
                    } else {
                        // Escape string
                        String strValue = value.toString()
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r");
                        writer.write("'" + strValue + "'");
                    }
                    
                    if (i < columnCount) writer.write(", ");
                }
                writer.write(");\n");
                rowCount++;
            }
            
            if (rowCount > 0) {
                writer.write("\n");
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Manually load SQLite driver
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.err.println("Warning: Unable to load SQLite driver, trying to continue...");
            }
            
            DatabaseManager dbManager = new DatabaseManager();
            DatabaseExporter exporter = new DatabaseExporter(dbManager);
            exporter.exportToMySQL("db.sql");
            System.out.println("Database successfully exported to db.sql");
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

