// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.util;

import com.bigcomp.accesscontrol.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to update resource group names in database from Chinese to English
 */
public class UpdateResourceGroupNames {
    
    // Mapping of Chinese group names to English
    private static final Map<String, String> GROUP_NAME_MAPPING = new HashMap<>();
    static {
        GROUP_NAME_MAPPING.put("设备资源", "Equipment Resources");
        GROUP_NAME_MAPPING.put("公共区域", "Public Area");
        GROUP_NAME_MAPPING.put("办公区域", "Office Area");
        GROUP_NAME_MAPPING.put("高安全区域", "High Security Area");
        GROUP_NAME_MAPPING.put("停车场区域", "Parking Area");
        GROUP_NAME_MAPPING.put("楼梯区域", "Stairway Area");
    }
    
    /**
     * Update all resource group names in database from Chinese to English
     */
    public static void updateDatabaseGroupNames(DatabaseManager dbManager) throws SQLException {
        Connection conn = dbManager.getConnection();
        
        System.out.println("Updating resource group names in database...");
        
        for (Map.Entry<String, String> entry : GROUP_NAME_MAPPING.entrySet()) {
            String chineseName = entry.getKey();
            String englishName = entry.getValue();
            
            // Update resource_group_members table
            String sql = "UPDATE resource_group_members SET group_name = ? WHERE group_name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, englishName);
                pstmt.setString(2, chineseName);
                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    System.out.println("  Updated " + updated + " records: \"" + chineseName + "\" -> \"" + englishName + "\"");
                }
            }
        }
        
        System.out.println("Database update complete!");
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        try {
            DatabaseManager dbManager = new DatabaseManager();
            updateDatabaseGroupNames(dbManager);
            dbManager.close();
        } catch (Exception e) {
            System.err.println("Error updating database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

