// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * User Management Panel
 */
public class UserManagementPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private DatabaseManager dbManager;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JComboBox<User.Gender> genderCombo;
    private JComboBox<User.UserType> userTypeCombo;

    public UserManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.dbManager = accessControlSystem.getDatabaseManager();
        initializeComponents();
        setupLayout();
        loadUsers();
    }

    private void initializeComponents() {
        // Table
        String[] columnNames = {"ID", "Name", "Gender", "Type", "Badge ID"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Input fields
        firstNameField = new JTextField(15);
        lastNameField = new JTextField(15);
        genderCombo = new JComboBox<>(User.Gender.values());
        userTypeCombo = new JComboBox<>(User.UserType.values());

        // Buttons
        JButton addButton = new JButton("Add User");
        addButton.addActionListener(e -> addUser());

        JButton deleteButton = new JButton("Delete User");
        deleteButton.addActionListener(e -> deleteUser());

        JButton createBadgeButton = new JButton("Create Badge");
        createBadgeButton.addActionListener(e -> createBadge());
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());

        // Top: Input form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Gender:"), gbc);
        gbc.gridx = 1;
        formPanel.add(genderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(userTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("Add User") {{
            addActionListener(e -> addUser());
        }});
        buttonPanel.add(new JButton("Delete User") {{
            addActionListener(e -> deleteUser());
        }});
        buttonPanel.add(new JButton("Create Badge") {{
            addActionListener(e -> createBadge());
        }});
        buttonPanel.add(new JButton("Assign Profile") {{
            addActionListener(e -> assignProfile());
        }});
        buttonPanel.add(new JButton("Auto-assign Profiles") {{
            addActionListener(e -> autoAssignProfilesForAll());
        }});
        formPanel.add(buttonPanel, gbc);

        // Center: Table
        JScrollPane scrollPane = new JScrollPane(userTable);

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addUser() {
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            
            if (firstName.isEmpty() || lastName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter name", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String userId = UUID.randomUUID().toString();
            User user = new User(
                userId,
                (User.Gender) genderCombo.getSelectedItem(),
                firstName,
                lastName,
                (User.UserType) userTypeCombo.getSelectedItem()
            );

            dbManager.addUser(user);
            loadUsers();
            
            // Clear input fields
            firstNameField.setText("");
            lastNameField.setText("");
            
            JOptionPane.showMessageDialog(this, "User added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to add user: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        String userName = (String) tableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete user \"" + userName + "\"?\nThis operation will also delete the user's badge and related configurations.", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteUser(userId);
                loadUsers();
                JOptionPane.showMessageDialog(this, "User deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to delete user: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void createBadge() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        try {
            // Get user information
            Map<String, User> allUsers = dbManager.loadAllUsers();
            User user = allUsers.get(userId);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "User does not exist", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Badge badge = new Badge(userId);
            String badgeId = UUID.randomUUID().toString();
            dbManager.addBadge(badge, badgeId);
            
            // Update user's badge ID
            user.setBadgeId(badgeId);
            dbManager.addUser(user);
            
            // Automatically assign profile based on user type
            autoAssignProfileByUserType(user, badgeId);
            
            // Reload in-memory data
            accessControlSystem.getAccessRequestProcessor().reloadData();
            
            loadUsers();
            JOptionPane.showMessageDialog(this, "Badge created successfully, profile automatically assigned", "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to create badge: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Automatically assign profile based on user type
     */
    private void autoAssignProfileByUserType(User user, String badgeId) {
        try {
            var profileManager = accessControlSystem.getProfileManager();
            String profileName = getDefaultProfileName(user.getUserType());
            
            if (profileName == null) {
                // No corresponding default profile, do not auto-assign
                return;
            }
            
            // Check if profile exists, if not create default profile
            Profile profile = profileManager.getProfile(profileName);
            if (profile == null) {
                // Create default profile
                createDefaultProfile(profileName, user.getUserType());
                profile = profileManager.getProfile(profileName);
            }
            
            if (profile != null) {
                // Assign profile
                dbManager.linkBadgeToProfile(badgeId, profileName);
                System.out.println("Auto-assigned profile: " + user.getFullName() + " (" + 
                    user.getUserType() + ") -> " + profileName);
            }
        } catch (Exception e) {
            System.err.println("Failed to auto-assign profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get default profile name based on user type
     */
    private String getDefaultProfileName(User.UserType userType) {
        switch (userType) {
            case EMPLOYEE:
                return "Employee Permission";
            case CONTRACTOR:
                return "Contractor Permission";
            case INTERN:
                return "Intern Permission";
            case VISITOR:
                return "Visitor Permission";
            case PROJECT_MANAGER:
                return "Project Manager Permission";
            default:
                return null;
        }
    }
    
    /**
     * Create default profile (if it doesn't exist)
     */
    private void createDefaultProfile(String profileName, User.UserType userType) {
        try {
            var profileManager = accessControlSystem.getProfileManager();
            Profile profile = new Profile(profileName);
            TimeFilter filter = new TimeFilter();
            
            // Set default time filter based on user type
            switch (userType) {
                case EMPLOYEE:
                    // Employee: Weekdays 8:00-18:00
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(8, 0, 18, 0)
                    ));
                    profile.addAccessRight("Public Area", filter);
                    profile.addAccessRight("Office Area", filter);
                    profile.addAccessRight("Equipment Resources", filter);
                    break;
                    
                case CONTRACTOR:
                    // Contractor: Weekdays 9:00-17:00
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(9, 0, 17, 0)
                    ));
                    profile.addAccessRight("Public Area", filter);
                    profile.addAccessRight("Office Area", filter);
                    break;
                    
                case INTERN:
                    // Intern: Weekdays 9:00-17:00
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(9, 0, 17, 0)
                    ));
                    profile.addAccessRight("Public Area", filter);
                    profile.addAccessRight("Office Area", filter);
                    break;
                    
                case VISITOR:
                    // Visitor: Weekdays 10:00-16:00
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(10, 0, 16, 0)
                    ));
                    profile.addAccessRight("Public Area", filter);
                    break;
                    
                case PROJECT_MANAGER:
                    // Project Manager: Weekdays 7:00-20:00, can access high security area
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(7, 0, 20, 0)
                    ));
                    profile.addAccessRight("Public Area", filter);
                    profile.addAccessRight("Office Area", filter);
                    profile.addAccessRight("Equipment Resources", filter);
                    profile.addAccessRight("High Security Area", filter);
                    break;
                    
                default:
                    // Default: Full-time access to public area
                    profile.addAccessRight("Public Area", filter);
                    break;
            }
            
            // Save profile
            profileManager.saveProfile(profile);
            System.out.println("Created default profile: " + profileName);
        } catch (Exception e) {
            System.err.println("Failed to create default profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void assignProfile() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        User user = dbManager.loadAllUsers().get(userId);
        
        if (user == null) {
            JOptionPane.showMessageDialog(this, "User does not exist", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (user.getBadgeId() == null) {
            JOptionPane.showMessageDialog(this, "This user does not have a badge yet, please create a badge first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get all available profiles
        var profileManager = accessControlSystem.getProfileManager();
        Map<String, Profile> profiles = profileManager.getAllProfiles();
        
        if (profiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No available profiles, please create profiles in Profile Management first", 
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String[] profileNames = profiles.keySet().toArray(new String[0]);
        
        // Show selection dialog
        String selectedProfile = (String) JOptionPane.showInputDialog(this,
            "Select profile to assign to user:",
            "Assign Profile",
            JOptionPane.QUESTION_MESSAGE,
            null,
            profileNames,
            profileNames.length > 0 ? profileNames[0] : null);
        
        if (selectedProfile != null) {
            try {
                // Check if user already has this profile
                Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
                Set<String> existingProfiles = userProfiles.get(user.getId());
                if (existingProfiles != null && existingProfiles.contains(selectedProfile)) {
                    JOptionPane.showMessageDialog(this, 
                        "User already has profile \"" + selectedProfile + "\"", 
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                
                // Assign profile (will be added to existing profiles, not overwritten)
                dbManager.linkBadgeToProfile(user.getBadgeId(), selectedProfile);
                accessControlSystem.getAccessRequestProcessor().reloadData();
                
                // Display all current profiles for user
                userProfiles = dbManager.loadUserProfiles();
                existingProfiles = userProfiles.get(user.getId());
                StringBuilder profileList = new StringBuilder();
                if (existingProfiles != null && !existingProfiles.isEmpty()) {
                    for (String profile : existingProfiles) {
                        if (profileList.length() > 0) {
                            profileList.append(", ");
                        }
                        profileList.append(profile);
                    }
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Profile \"" + selectedProfile + "\" has been assigned to user\n\n" +
                    "User's current profiles:\n" + profileList.toString(), 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to assign profile: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void loadUsers() {
        tableModel.setRowCount(0);
        try {
            // Use loadAllUsers() to load all users, including those without badges
            Map<String, User> users = dbManager.loadAllUsers();
            for (User user : users.values()) {
                tableModel.addRow(new Object[]{
                    user.getId(),
                    user.getFullName(),
                    user.getGender().toString(),
                    user.getUserType().toString(),
                    user.getBadgeId() != null ? user.getBadgeId() : "None"
                });
            }
            
            // Reload in-memory data (for access control, only users with badges are needed)
            accessControlSystem.getAccessRequestProcessor().reloadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load users: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Automatically assign profiles to all users with badges but without profiles
     */
    private void autoAssignProfilesForAll() {
        try {
            Map<String, User> allUsers = dbManager.loadAllUsers();
            Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
            
            int assignedCount = 0;
            int skippedWithProfile = 0;
            int skippedNoBadge = 0;
            int errorCount = 0;
            StringBuilder details = new StringBuilder();
            
            for (User user : allUsers.values()) {
                // Check if user has badge
                if (user.getBadgeId() == null || user.getBadgeId().isEmpty()) {
                    skippedNoBadge++;
                    continue;
                }
                
                // Check if user already has profiles
                Set<String> existingProfiles = userProfiles.get(user.getId());
                if (existingProfiles != null && !existingProfiles.isEmpty()) {
                    skippedWithProfile++;
                    continue;
                }
                
                // Automatically assign profile
                try {
                    String profileName = getDefaultProfileName(user.getUserType());
                    if (profileName != null) {
                        // Check if profile exists, create if not
                        var profileManager = accessControlSystem.getProfileManager();
                        Profile profile = profileManager.getProfile(profileName);
                        if (profile == null) {
                            createDefaultProfile(profileName, user.getUserType());
                        }
                        
                        // Assign profile
                        dbManager.linkBadgeToProfile(user.getBadgeId(), profileName);
                        assignedCount++;
                        details.append("  ✓ ").append(user.getFullName())
                               .append(" (").append(user.getUserType())
                               .append(") -> ").append(profileName).append("\n");
                    }
                } catch (Exception e) {
                    errorCount++;
                    details.append("  ✗ ").append(user.getFullName())
                           .append(" -> Failed: ").append(e.getMessage()).append("\n");
                }
            }
            
            // Reload in-memory data
            accessControlSystem.getAccessRequestProcessor().reloadData();
            
            // Display results
            String message = String.format(
                "Auto-assign profiles completed!\n\n" +
                "Statistics:\n" +
                "• Successfully assigned: %d users\n" +
                "• Skipped (already has profile): %d users\n" +
                "• Skipped (no badge): %d users\n" +
                "• Failed: %d users\n\n" +
                "Details:\n%s",
                assignedCount, skippedWithProfile, skippedNoBadge, errorCount, 
                details.length() > 0 ? details.toString() : "None"
            );
            
            JOptionPane.showMessageDialog(this, message, 
                "Auto-assign Complete", 
                assignedCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            
            // Refresh user list
            loadUsers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to auto-assign profiles: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}

