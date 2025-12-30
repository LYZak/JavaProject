// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.test;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.*;
import com.bigcomp.accesscontrol.profile.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * System Function Test Runner
 * Automatically creates test data and demonstrates all functions
 */
public class SystemTestRunner {
    private AccessControlSystem accessControlSystem;
    private DatabaseManager dbManager;
    
    public SystemTestRunner() {
        this.accessControlSystem = new AccessControlSystem();
        this.dbManager = accessControlSystem.getDatabaseManager();
    }
    
    /**
     * Run full test
     */
    public void runFullTest() {
        System.out.println("==========================================");
        System.out.println("BigComp Access Control System - Full Function Test");
        System.out.println("==========================================\n");
        
        try {
            // 1. Create test users
            System.out.println("Step 1: Creating test users...");
            createTestUsers();
            System.out.println("✓ User creation completed\n");
            
            // 2. Create test resources
            System.out.println("Step 2: Creating test resources...");
            createTestResources();
            System.out.println("✓ Resource creation completed\n");
            
            // 3. Create badge readers
            System.out.println("Step 3: Creating badge readers...");
            createTestBadgeReaders();
            System.out.println("✓ Badge reader creation completed\n");
            
            // 4. Create resource groups
            System.out.println("Step 4: Creating resource groups...");
            createTestResourceGroups();
            System.out.println("✓ Resource group creation completed\n");
            
            // 5. Create profiles
            System.out.println("Step 5: Creating profiles...");
            createTestProfiles();
            System.out.println("✓ Profile creation completed\n");
            
            // 6. Assign profiles
            System.out.println("Step 6: Assigning profiles to users...");
            assignProfilesToUsers();
            System.out.println("✓ Profile assignment completed\n");
            
            // 7. Generate test logs
            System.out.println("Step 7: Generating test logs...");
            generateTestLogs();
            System.out.println("✓ Test log generation completed\n");
            
            System.out.println("==========================================");
            System.out.println("All test data creation completed!");
            System.out.println("==========================================");
            System.out.println("\nTest Data Summary:");
            printTestSummary();
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create test users
     */
    private void createTestUsers() throws Exception {
        // Create all types of users (cover all user types)
        User[] users = {
            // Employees
            new User("EMP001", User.Gender.MALE, "Zhang", "San", User.UserType.EMPLOYEE),
            new User("EMP002", User.Gender.FEMALE, "Li", "Si", User.UserType.EMPLOYEE),
            // Contractors
            new User("CON001", User.Gender.MALE, "Wang", "Wu", User.UserType.CONTRACTOR),
            // Interns
            new User("INT001", User.Gender.FEMALE, "Zhao", "Liu", User.UserType.INTERN),
            // Visitors
            new User("VIS001", User.Gender.MALE, "Qian", "Qi", User.UserType.VISITOR),
            // Project Managers
            new User("PM001", User.Gender.FEMALE, "Sun", "Ba", User.UserType.PROJECT_MANAGER)
        };
        
        for (User user : users) {
            dbManager.addUser(user);
            // Create badge for each user
            Badge badge = new Badge(user.getId());
            badge.setExpirationDate(LocalDateTime.now().plusYears(1));
            String badgeId = UUID.randomUUID().toString();
            dbManager.addBadge(badge, badgeId);
            // Update user's badge ID
            user.setBadgeId(badgeId);
            dbManager.addUser(user);
            System.out.println("  Created user: " + user.getFullName() + " (" + user.getUserType() + ") - Badge: " + badge.getCode());
        }
    }
    
    /**
     * Create test resources
     */
    private void createTestResources() throws Exception {
        Resource[] resources = {
            // Site entrances (GATE type)
            new Resource("RES001", "Main Entrance Gate", Resource.ResourceType.GATE, "Site", "Main Office Building", "1F"),
            new Resource("RES002", "Parking Entrance", Resource.ResourceType.GATE, "Site", "Parking Lot", "Ground"),
            new Resource("RES003", "Side Gate Entrance", Resource.ResourceType.GATE, "Site", "Main Office Building", "1F"),
            
            // Building entrances (DOOR type)
            new Resource("RES004", "Main Office Building Main Door", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "1F"),
            new Resource("RES005", "Main Office Building Side Door", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "1F"),
            new Resource("RES006", "Annex Building Entrance", Resource.ResourceType.DOOR, "Annex Building", "Annex Building", "1F"),
            
            // Office doors (DOOR type)
            new Resource("RES007", "Office 1", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "2F"),
            new Resource("RES008", "Office 2", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "2F"),
            new Resource("RES009", "Office 3", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "3F"),
            new Resource("RES010", "Meeting Room", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "2F"),
            new Resource("RES011", "Technical Room", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "3F"),
            
            // Elevators and stairways
            new Resource("RES012", "Main Elevator", Resource.ResourceType.ELEVATOR, "Main Office Building", "Main Office Building", "1F"),
            new Resource("RES013", "Annex Elevator", Resource.ResourceType.ELEVATOR, "Main Office Building", "Main Office Building", "1F"),
            new Resource("RES014", "Main Stairway", Resource.ResourceType.STAIRWAY, "Main Office Building", "Main Office Building", "1F"),
            new Resource("RES015", "Emergency Stairway", Resource.ResourceType.STAIRWAY, "Main Office Building", "Main Office Building", "1F"),
            
            // Printers (PRINTER type)
            new Resource("RES016", "Printer 1", Resource.ResourceType.PRINTER, "Main Office Building", "Main Office Building", "2F"),
            new Resource("RES017", "Printer 2", Resource.ResourceType.PRINTER, "Main Office Building", "Main Office Building", "3F"),
            new Resource("RES018", "Color Printer", Resource.ResourceType.PRINTER, "Main Office Building", "Main Office Building", "2F"),
            
            // Beverage dispensers (BEVERAGE_DISPENSER type)
            new Resource("RES019", "Beverage Dispenser 1", Resource.ResourceType.BEVERAGE_DISPENSER, "Main Office Building", "Main Office Building", "2F"),
            new Resource("RES020", "Beverage Dispenser 2", Resource.ResourceType.BEVERAGE_DISPENSER, "Main Office Building", "Main Office Building", "3F"),
            
            // Parking (PARKING type)
            new Resource("RES021", "Underground Parking Area A", Resource.ResourceType.PARKING, "Main Office Building", "Main Office Building", "B1"),
            new Resource("RES022", "Underground Parking Area B", Resource.ResourceType.PARKING, "Main Office Building", "Main Office Building", "B1"),
            new Resource("RES023", "Ground Parking", Resource.ResourceType.PARKING, "Site", "Parking Lot", "Ground"),
            
            // High security areas (DOOR type)
            new Resource("RES024", "Server Room", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "3F"),
            new Resource("RES025", "Data Center", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "3F"),
            new Resource("RES026", "Finance Room", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "2F")
        };
        
        for (Resource resource : resources) {
            dbManager.addResource(resource);
            System.out.println("  Created resource: " + resource.getName() + " (" + resource.getType() + ")");
        }
    }
    
    /**
     * Create badge readers
     */
    private void createTestBadgeReaders() throws Exception {
        // Create badge reader for each resource
        Map<String, Resource> resources = dbManager.loadAllResources();
        int readerIndex = 1;
        
        for (Resource resource : resources.values()) {
            BadgeReader reader = new BadgeReader("BR" + String.format("%03d", readerIndex), resource.getId());
            dbManager.addBadgeReader(reader);
            resource.setBadgeReaderId(reader.getId());
            
            // Register to router
            accessControlSystem.getRouter().registerBadgeReader(reader);
            
            System.out.println("  Created badge reader: " + reader.getId() + " -> " + resource.getName());
            readerIndex++;
        }
    }
    
    /**
     * Create resource groups
     */
    private void createTestResourceGroups() throws Exception {
        GroupManager groupManager = new GroupManager();
        Map<String, Resource> resources = dbManager.loadAllResources();
        
        // Public area group (low security level)
        ResourceGroup publicArea = new ResourceGroup("Public Area", 1);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.GATE || 
                res.getName().contains("Entrance") || 
                res.getName().contains("Gate")) {
                publicArea.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "Public Area");
            }
        }
        groupManager.saveGroup(publicArea);
        System.out.println("  Created resource group: Public Area (Security Level: 1, Resources: " + publicArea.getResourceIds().size() + ")");
        
        // Office area group (medium security level)
        ResourceGroup officeArea = new ResourceGroup("Office Area", 2);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.DOOR && 
                (res.getName().contains("Office") || res.getName().contains("Meeting Room"))) {
                officeArea.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "Office Area");
            }
        }
        groupManager.saveGroup(officeArea);
        System.out.println("  Created resource group: Office Area (Security Level: 2, Resources: " + officeArea.getResourceIds().size() + ")");
        
        // Equipment resource group
        ResourceGroup equipmentGroup = new ResourceGroup("Equipment Resources", 2);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.PRINTER || 
                res.getType() == Resource.ResourceType.BEVERAGE_DISPENSER) {
                equipmentGroup.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "Equipment Resources");
            }
        }
        groupManager.saveGroup(equipmentGroup);
        System.out.println("  Created resource group: Equipment Resources (Security Level: 2, Resources: " + equipmentGroup.getResourceIds().size() + ")");
        
        // High security area group
        ResourceGroup highSecurityArea = new ResourceGroup("High Security Area", 3);
        for (Resource res : resources.values()) {
            if (res.getName().contains("Server") || 
                res.getName().contains("Data Center") ||
                res.getName().contains("Finance") ||
                res.getType() == Resource.ResourceType.ELEVATOR) {
                highSecurityArea.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "High Security Area");
            }
        }
        groupManager.saveGroup(highSecurityArea);
        System.out.println("  Created resource group: High Security Area (Security Level: 3, Resources: " + highSecurityArea.getResourceIds().size() + ")");
        
        // Parking group
        ResourceGroup parkingGroup = new ResourceGroup("Parking Area", 1);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.PARKING) {
                parkingGroup.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "Parking Area");
            }
        }
        groupManager.saveGroup(parkingGroup);
        System.out.println("  Created resource group: Parking Area (Security Level: 1, Resources: " + parkingGroup.getResourceIds().size() + ")");
        
        // Stairway group
        ResourceGroup stairwayGroup = new ResourceGroup("Stairway Area", 2);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.STAIRWAY) {
                stairwayGroup.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "Stairway Area");
            }
        }
        groupManager.saveGroup(stairwayGroup);
        System.out.println("  Created resource group: Stairway Area (Security Level: 2, Resources: " + stairwayGroup.getResourceIds().size() + ")");
    }
    
    /**
     * Create profiles
     */
    private void createTestProfiles() throws Exception {
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        
        // Employee permission configuration
        Profile employeeProfile = new Profile("Employee Permission");
        TimeFilter employeeFilter = new TimeFilter();
        employeeFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        employeeFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(8, 0, 18, 0)
        ));
        employeeProfile.addAccessRight("Public Area", employeeFilter);
        employeeProfile.addAccessRight("Office Area", employeeFilter);
        employeeProfile.addAccessRight("Equipment Resources", employeeFilter);
        profileManager.saveProfile(employeeProfile);
        System.out.println("  Created profile: Employee Permission");
        
        // Contractor permission configuration
        Profile contractorProfile = new Profile("Contractor Permission");
        TimeFilter contractorFilter = new TimeFilter();
        contractorFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        contractorFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(9, 0, 17, 0)
        ));
        contractorProfile.addAccessRight("Public Area", contractorFilter);
        contractorProfile.addAccessRight("Office Area", contractorFilter);
        profileManager.saveProfile(contractorProfile);
        System.out.println("  Created profile: Contractor Permission");
        
        // Intern permission configuration
        Profile internProfile = new Profile("Intern Permission");
        TimeFilter internFilter = new TimeFilter();
        internFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        internFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(9, 0, 17, 0)
        ));
        internProfile.addAccessRight("Public Area", internFilter);
        internProfile.addAccessRight("Office Area", internFilter);
        profileManager.saveProfile(internProfile);
        System.out.println("  Created profile: Intern Permission");
        
        // Visitor permission configuration
        Profile visitorProfile = new Profile("Visitor Permission");
        TimeFilter visitorFilter = new TimeFilter();
        visitorFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        visitorFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(10, 0, 16, 0)
        ));
        visitorProfile.addAccessRight("Public Area", visitorFilter);
        profileManager.saveProfile(visitorProfile);
        System.out.println("  Created profile: Visitor Permission");
        
        // Project Manager permission configuration (advanced permission)
        Profile pmProfile = new Profile("Project Manager Permission");
        TimeFilter pmFilter = new TimeFilter();
        pmFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        pmFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(7, 0, 20, 0) // Extended access time
        ));
        pmProfile.addAccessRight("Public Area", pmFilter);
        pmProfile.addAccessRight("Office Area", pmFilter);
        pmProfile.addAccessRight("Equipment Resources", pmFilter);
        pmProfile.addAccessRight("High Security Area", pmFilter); // Can access high security area
        profileManager.saveProfile(pmProfile);
        System.out.println("  Created profile: Project Manager Permission");
        
        // Full-time access permission configuration (demonstrates ALL time filter)
        Profile fullAccessProfile = new Profile("Full-time Access");
        TimeFilter fullAccessFilter = new TimeFilter();
        // No restrictions set, meaning ALL (all times)
        fullAccessProfile.addAccessRight("Public Area", fullAccessFilter);
        fullAccessProfile.addAccessRight("Office Area", fullAccessFilter);
        profileManager.saveProfile(fullAccessProfile);
        System.out.println("  Created profile: Full-time Access (ALL time)");
        
        // Exclude time configuration (demonstrates EXCEPT functionality)
        // Note: In current TimeFilter implementation, excludeTimeRanges means excluding the entire timeRanges list
        // To implement "exclude 12:00-14:00", need to set two time ranges: 8:00-12:00 and 14:00-18:00
        Profile excludeLunchProfile = new Profile("Exclude Lunch Time");
        TimeFilter excludeLunchFilter = new TimeFilter();
        excludeLunchFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        // Set two time ranges, excluding the lunch time in between
        excludeLunchFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(8, 0, 12, 0),  // Morning
            new TimeFilter.TimeRange(14, 0, 18, 0)  // Afternoon
        ));
        excludeLunchProfile.addAccessRight("Office Area", excludeLunchFilter);
        profileManager.saveProfile(excludeLunchProfile);
        System.out.println("  Created profile: Exclude Lunch Time (8:00-12:00, 14:00-18:00)");
        
        // Exclude day of week configuration (demonstrates EXCEPT day of week functionality)
        Profile excludeWeekendProfile = new Profile("Exclude Weekend");
        TimeFilter excludeWeekendFilter = new TimeFilter();
        // Exclude weekends (Saturday and Sunday)
        excludeWeekendFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.SATURDAY,
            java.time.DayOfWeek.SUNDAY
        ));
        excludeWeekendFilter.setExcludeDaysOfWeek(true); // Exclude these days
        excludeWeekendFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(8, 0, 18, 0)
        ));
        excludeWeekendProfile.addAccessRight("Public Area", excludeWeekendFilter);
        excludeWeekendProfile.addAccessRight("Office Area", excludeWeekendFilter);
        profileManager.saveProfile(excludeWeekendProfile);
        System.out.println("  Created profile: Exclude Weekend (EXCEPT Saturday, Sunday)");
    }
    
    /**
     * Assign profiles to users
     */
    private void assignProfilesToUsers() throws Exception {
        Map<String, User> users = dbManager.loadAllUsers();
        
        for (User user : users.values()) {
            // Find badge by user ID
            Badge badge = dbManager.loadBadgeByUserId(user.getId());
            if (badge == null) continue;
            
            String profileName = null;
            switch (user.getUserType()) {
                case EMPLOYEE:
                    profileName = "Employee Permission";
                    break;
                case CONTRACTOR:
                    profileName = "Contractor Permission";
                    break;
                case INTERN:
                    profileName = "Intern Permission";
                    break;
                case VISITOR:
                    profileName = "Visitor Permission";
                    break;
                case PROJECT_MANAGER:
                    profileName = "Project Manager Permission";
                    break;
                default:
                    break;
            }
            
            if (profileName != null) {
                // Need to find badge ID by badge code
                String badgeId = findBadgeIdByCode(badge.getCode());
                if (badgeId != null) {
                    dbManager.linkBadgeToProfile(badgeId, profileName);
                    System.out.println("  Assigned profile: " + user.getFullName() + " -> " + profileName);
                }
            }
        }
    }
    
    /**
     * Find badge ID by badge code
     */
    private String findBadgeIdByCode(String badgeCode) {
        try {
            String sql = "SELECT id FROM badges WHERE code = ?";
            try (var pstmt = dbManager.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, badgeCode);
                try (var rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("id");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to find badge ID: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Generate test logs
     */
    private void generateTestLogs() {
        // This function is automatically generated by LogManager at runtime
        // This is just a reminder
        System.out.println("  Note: Logs will be automatically generated when access events occur");
        System.out.println("  Log location: data/logs/year/month/date.csv");
        System.out.println("\nSimulation Function Instructions:");
        System.out.println("  1. In the GUI's 'Event Simulation' tab:");
        System.out.println("     - Select users and click 'Add User' to add to simulation list");
        System.out.println("     - Set event interval (1-60 seconds)");
        System.out.println("     - Click 'Start Simulation' to begin generating access events");
        System.out.println("  2. Simulator supports consistent behavior pattern:");
        System.out.println("     - Users will first access site entrance (GATE)");
        System.out.println("     - Then access building entrance (DOOR, 1st floor)");
        System.out.println("     - Finally access offices, elevators and other internal resources");
        System.out.println("  3. Time testing function:");
        System.out.println("     - Click 'Set Time' to modify system time");
        System.out.println("     - Used to test access rules at different time periods (e.g., weekends, non-working hours)");
        System.out.println("     - Click 'Reset Time' to restore system time");
    }
    
    /**
     * Print test summary
     */
    private void printTestSummary() {
        try {
            Map<String, User> users = dbManager.loadAllUsers();
            Map<String, Resource> resources = dbManager.loadAllResources();
            
            GroupManager groupManager = new GroupManager();
            Map<String, ResourceGroup> groups = groupManager.getAllGroups();
            
            ProfileManager profileManager = accessControlSystem.getProfileManager();
            Map<String, Profile> profiles = profileManager.getAllProfiles();
            
            System.out.println("Number of users: " + users.size());
            System.out.println("Number of resources: " + resources.size());
            System.out.println("Number of resource groups: " + groups.size());
            System.out.println("Number of profiles: " + profiles.size());
            
            System.out.println("\nUser List:");
            for (User user : users.values()) {
                Badge badge = dbManager.loadBadgeByUserId(user.getId());
                System.out.println("  - " + user.getFullName() + " (" + user.getUserType() + 
                    ") - Badge: " + (badge != null ? badge.getCode() : "None"));
            }
            
            System.out.println("\nResource Group List:");
            for (ResourceGroup group : groups.values()) {
                System.out.println("  - " + group.getName() + " (Security Level: " + 
                    group.getSecurityLevel() + ", Resources: " + group.getResourceIds().size() + ")");
            }
            
            System.out.println("\nProfile List:");
            for (Profile profile : profiles.values()) {
                System.out.println("  - " + profile.getName() + " (Access Rights: " + 
                    profile.getAccessRights().size() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Error printing summary: " + e.getMessage());
        }
    }
    
    /**
     * Main method - Run test
     */
    public static void main(String[] args) {
        SystemTestRunner runner = new SystemTestRunner();
        runner.runFullTest();
        
        System.out.println("\n==========================================");
        System.out.println("Test completed! You can now start the GUI application.");
        System.out.println("Run Main.java to start the graphical interface.");
        System.out.println("==========================================");
        System.out.println("\nNext Steps:");
        System.out.println("1. Start GUI: Run Main.java or use run_all.bat");
        System.out.println("2. Test simulation function:");
        System.out.println("   - Go to 'Event Simulation' tab");
        System.out.println("   - Select users and add to simulation list");
        System.out.println("   - Set event interval and start simulation");
        System.out.println("   - Observe access events in real-time monitor panel");
        System.out.println("   - View access records in log panel");
        System.out.println("3. Test time function:");
        System.out.println("   - Click 'Set Time' in event simulation panel");
        System.out.println("   - Set to weekend or non-working hours");
        System.out.println("   - Observe if access is correctly denied");
        System.out.println("==========================================");
    }
}

