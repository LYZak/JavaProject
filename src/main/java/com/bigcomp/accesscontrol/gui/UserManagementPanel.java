// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
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
    private TitledBorder formBorder;
    private JLabel firstNameLabel;
    private JLabel lastNameLabel;
    private JLabel genderLabel;
    private JLabel typeLabel;
    private JButton addButton;
    private JButton deleteButton;
    private JButton createBadgeButton;
    private JButton assignProfileButton;
    private JButton autoAssignButton;

    public UserManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.dbManager = accessControlSystem.getDatabaseManager();
        initializeComponents();
        setupLayout();
        applyLanguage();
        loadUsers();
    }

    private void initializeComponents() {
        // Table
        tableModel = new DefaultTableModel(getColumnNames(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setAutoCreateRowSorter(true);
        userTable.setFillsViewportHeight(true);
        styleTable(userTable);

        // Input fields
        firstNameField = new JTextField(15);
        lastNameField = new JTextField(15);
        genderCombo = new JComboBox<>(User.Gender.values());
        genderCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User.Gender) {
                    setText(I18n.t("user.gender." + ((User.Gender) value).name()));
                }
                return this;
            }
        });
        
        userTypeCombo = new JComboBox<>(User.UserType.values());
        userTypeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User.UserType) {
                    setText(I18n.t("user.type." + ((User.UserType) value).name()));
                }
                return this;
            }
        });
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top: Input form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formBorder = new TitledBorder("");
        formPanel.setBorder(formBorder);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        firstNameLabel = new JLabel();
        formPanel.add(firstNameLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        lastNameLabel = new JLabel();
        formPanel.add(lastNameLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0;
        genderLabel = new JLabel();
        formPanel.add(genderLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(genderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.weightx = 0;
        typeLabel = new JLabel();
        formPanel.add(typeLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(userTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        addButton = new JButton();
        addButton.addActionListener(e -> addUser());
        deleteButton = new JButton();
        deleteButton.addActionListener(e -> deleteUser());
        createBadgeButton = new JButton();
        createBadgeButton.addActionListener(e -> createBadge());
        assignProfileButton = new JButton();
        assignProfileButton.addActionListener(e -> assignProfile());
        autoAssignButton = new JButton();
        autoAssignButton.addActionListener(e -> autoAssignProfilesForAll());
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(createBadgeButton);
        buttonPanel.add(assignProfileButton);
        buttonPanel.add(autoAssignButton);
        formPanel.add(buttonPanel, gbc);

        // Center: Table
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(new EmptyBorder(8, 0, 0, 0));

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(Math.max(table.getRowHeight(), 28));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
    }

    private static class StripedTableCellRenderer extends DefaultTableCellRenderer {
        private final Color stripe = new Color(247, 248, 250);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground((row % 2 == 0) ? table.getBackground() : stripe);
            }
            return c;
        }
    }

    private void addUser() {
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            
            if (firstName.isEmpty() || lastName.isEmpty()) {
                JOptionPane.showMessageDialog(this, I18n.t("user.msg.enterName"), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
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
            
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.added"), I18n.t("common.success"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("user.msg.addFailed", e.getMessage()),
                I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.selectDelete"), I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        String userName = (String) tableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            I18n.f("user.msg.confirmDelete", userName), 
            I18n.t("common.confirmDelete.title"), 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteUser(userId);
                loadUsers();
                JOptionPane.showMessageDialog(this, I18n.t("user.msg.deleted"), I18n.t("common.success"), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("user.msg.deleteFailed", e.getMessage()),
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void createBadge() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.selectUser"), I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        try {
            // Get user information
            Map<String, User> allUsers = dbManager.loadAllUsers();
            User user = allUsers.get(userId);
            if (user == null) {
                JOptionPane.showMessageDialog(this, I18n.t("user.msg.notExist"), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.badgeCreatedAutoProfile"), I18n.t("common.success"), 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("user.msg.badgeCreateFailed", e.getMessage()),
                I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
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
                return "profile.default.employee";
            case CONTRACTOR:
                return "profile.default.contractor";
            case INTERN:
                return "profile.default.intern";
            case VISITOR:
                return "profile.default.visitor";
            case PROJECT_MANAGER:
                return "profile.default.project_manager";
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
                    profile.addAccessRight("access.right.public_area", filter);
                    profile.addAccessRight("access.right.office_area", filter);
                    profile.addAccessRight("access.right.equipment_resources", filter);
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
                    profile.addAccessRight("access.right.public_area", filter);
                    profile.addAccessRight("access.right.office_area", filter);
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
                    profile.addAccessRight("access.right.public_area", filter);
                    profile.addAccessRight("access.right.office_area", filter);
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
                    profile.addAccessRight("access.right.public_area", filter);
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
                    profile.addAccessRight("access.right.public_area", filter);
                    profile.addAccessRight("access.right.office_area", filter);
                    profile.addAccessRight("access.right.equipment_resources", filter);
                    profile.addAccessRight("access.right.high_security_area", filter);
                    break;
                    
                default:
                    // Default: Full-time access to public area
                    profile.addAccessRight("access.right.public_area", filter);
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
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.selectUser"), I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        User user = dbManager.loadAllUsers().get(userId);
        
        if (user == null) {
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.notExist"), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (user.getBadgeId() == null) {
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.noBadge"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get all available profiles
        var profileManager = accessControlSystem.getProfileManager();
        Map<String, Profile> profiles = profileManager.getAllProfiles();
        
        if (profiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.noProfiles"),
                I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String[] profileNames = profiles.keySet().toArray(new String[0]);
        
        // Show selection dialog
        String selectedProfile = (String) JOptionPane.showInputDialog(this,
            I18n.t("user.msg.selectProfile"),
            I18n.t("user.action.assignProfile"),
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
                        I18n.f("user.msg.profileAlready", selectedProfile),
                        I18n.t("common.info"), JOptionPane.INFORMATION_MESSAGE);
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
                        profileList.append(I18n.t(profile));
                    }
                }
                
                JOptionPane.showMessageDialog(this, 
                    I18n.f("user.msg.profileAssigned", I18n.t(selectedProfile), profileList.toString()),
                    I18n.t("common.success"), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("user.msg.assignProfileFailed", e.getMessage()),
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
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
                    I18n.t("user.gender." + user.getGender().name()),
                    I18n.t("user.type." + user.getUserType().name()),
                    user.getBadgeId() != null ? user.getBadgeId() : I18n.t("common.none")
                });
            }
            
            // Reload in-memory data (for access control, only users with badges are needed)
            accessControlSystem.getAccessRequestProcessor().reloadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("user.msg.loadFailed", e.getMessage()),
                I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
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
                        details.append(I18n.f("user.msg.autoAssignDetailSuccess", 
                            user.getFullName(), 
                            I18n.t("user.type." + user.getUserType().name()), 
                            I18n.t(profileName))).append("\n");
                    }
                } catch (Exception e) {
                    errorCount++;
                    details.append(I18n.f("user.msg.autoAssignDetailFail", 
                        user.getFullName(), 
                        e.getMessage())).append("\n");
                }
            }
            
            // Reload in-memory data
            accessControlSystem.getAccessRequestProcessor().reloadData();
            
            // Display results
            String message = I18n.f(
                "user.msg.autoAssignSummary",
                assignedCount, skippedWithProfile, skippedNoBadge, errorCount,
                details.length() > 0 ? details.toString() : I18n.t("common.none")
            );
            
            JOptionPane.showMessageDialog(this, message, 
                I18n.t("user.action.autoAssign"), 
                assignedCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            
            // Refresh user list
            loadUsers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                I18n.f("user.msg.autoAssignFailed", e.getMessage()),
                I18n.t("common.error"), 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void applyLanguage() {
        formBorder.setTitle(I18n.t("user.title"));
        firstNameLabel.setText(I18n.t("user.firstName"));
        lastNameLabel.setText(I18n.t("user.lastName"));
        genderLabel.setText(I18n.t("user.gender"));
        typeLabel.setText(I18n.t("user.type"));

        addButton.setText(I18n.t("user.action.add"));
        deleteButton.setText(I18n.t("user.action.delete"));
        createBadgeButton.setText(I18n.t("user.action.createBadge"));
        assignProfileButton.setText(I18n.t("user.action.assignProfile"));
        autoAssignButton.setText(I18n.t("user.action.autoAssign"));

        tableModel.setColumnIdentifiers(getColumnNames());
        userTable.getTableHeader().repaint();
        revalidate();
        repaint();
    }

    private String[] getColumnNames() {
        return new String[]{
            I18n.t("user.col.id"),
            I18n.t("user.col.name"),
            I18n.t("user.col.gender"),
            I18n.t("user.col.type"),
            I18n.t("user.col.badgeId")
        };
    }
}

