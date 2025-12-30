// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.profile.GroupManager;
import com.bigcomp.accesscontrol.profile.ResourceGroup;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Profile Management Panel
 */
public class ProfileManagementPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JList<String> profileList;
    private DefaultListModel<String> profileListModel;
    private JTable accessRightsTable;
    private DefaultTableModel accessRightsModel;
    private JTextField profileNameField;
    private JComboBox<String> groupCombo;
    private JTextArea profileInfoArea;
    
    public ProfileManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        initializeComponents();
        setupLayout();
        loadProfiles();
        loadGroups();
    }
    
    private void initializeComponents() {
        // Profile list
        profileListModel = new DefaultListModel<>();
        profileList = new JList<>(profileListModel);
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedProfile();
            }
        });
        
        // Access rights table
        String[] columnNames = {"Resource Group", "Time Filter"};
        accessRightsModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        accessRightsTable = new JTable(accessRightsModel);
        
        // Input fields
        profileNameField = new JTextField(20);
        groupCombo = new JComboBox<>();
        profileInfoArea = new JTextArea(5, 30);
        profileInfoArea.setEditable(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Left: Profile list and operations
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Profile List"));
        leftPanel.add(new JScrollPane(profileList), BorderLayout.CENTER);
        
        JPanel leftButtonPanel = new JPanel(new FlowLayout());
        leftButtonPanel.add(new JButton("New Profile") {{
            addActionListener(e -> createNewProfile());
        }});
        leftButtonPanel.add(new JButton("Modify Profile") {{
            addActionListener(e -> modifyProfile());
        }});
        leftButtonPanel.add(new JButton("Delete Profile") {{
            addActionListener(e -> deleteProfile());
        }});
        leftButtonPanel.add(new JButton("Restore Profile") {{
            addActionListener(e -> restoreProfile());
        }});
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // Center: Access rights management
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Access Rights"));
        
        // Top: Add access rights
        JPanel addPanel = new JPanel(new FlowLayout());
        addPanel.add(new JLabel("Resource Group:"));
        addPanel.add(groupCombo);
        addPanel.add(new JButton("Refresh Groups") {{
            addActionListener(e -> {
                loadGroups();
                JOptionPane.showMessageDialog(ProfileManagementPanel.this, 
                    "Resource group list refreshed", "Info", JOptionPane.INFORMATION_MESSAGE);
            });
        }});
        addPanel.add(new JButton("Add Access Right") {{
            addActionListener(e -> addAccessRight());
        }});
        addPanel.add(new JButton("Remove Access Right") {{
            addActionListener(e -> removeAccessRight());
        }});
        centerPanel.add(addPanel, BorderLayout.NORTH);
        
        // Center: Access rights table
        centerPanel.add(new JScrollPane(accessRightsTable), BorderLayout.CENTER);
        
        // Bottom: Time filter editing
        JPanel timeFilterPanel = new JPanel(new BorderLayout());
        timeFilterPanel.setBorder(BorderFactory.createTitledBorder("Time Filter Editor"));
        timeFilterPanel.add(new JScrollPane(profileInfoArea), BorderLayout.CENTER);
        JPanel timeFilterButtonPanel = new JPanel(new FlowLayout());
        timeFilterButtonPanel.add(new JButton("Edit Time Filter") {{
            addActionListener(e -> editTimeFilter());
        }});
        timeFilterPanel.add(timeFilterButtonPanel, BorderLayout.SOUTH);
        centerPanel.add(timeFilterPanel, BorderLayout.SOUTH);
        
        // Right: Profile information
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Profile Information"));
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        infoPanel.add(new JLabel("Profile Name:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(profileNameField, gbc);
        
        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(new JButton("Save Profile") {{
            addActionListener(e -> saveProfile());
        }}, BorderLayout.SOUTH);
        
        // Main layout
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        leftSplit.setDividerLocation(200);
        leftSplit.setResizeWeight(0.2);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setDividerLocation(800);
        mainSplit.setResizeWeight(0.7);
        
        add(mainSplit, BorderLayout.CENTER);
    }
    
    private void loadProfiles() {
        profileListModel.clear();
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        Map<String, Profile> profiles = profileManager.getAllProfiles();
        for (String name : profiles.keySet()) {
            profileListModel.addElement(name);
        }
    }
    
    private void loadGroups() {
        groupCombo.removeAllItems();
        GroupManager groupManager = new GroupManager();
        Map<String, ResourceGroup> groups = groupManager.getAllGroups();
        
        if (groups.isEmpty()) {
            // If no resource groups, prompt user
            groupCombo.addItem("(No resource groups, please create in data/groups/ directory)");
            groupCombo.setEnabled(false);
        } else {
            for (String name : groups.keySet()) {
                groupCombo.addItem(name);
            }
            groupCombo.setEnabled(true);
        }
    }
    
    private void loadSelectedProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            clearProfileInfo();
            return;
        }
        
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        Profile profile = profileManager.getProfile(selected);
        if (profile != null) {
            profileNameField.setText(profile.getName());
            
            // Load access rights
            accessRightsModel.setRowCount(0);
            Map<String, TimeFilter> accessRights = profile.getAccessRights();
            for (Map.Entry<String, TimeFilter> entry : accessRights.entrySet()) {
                String groupName = entry.getKey();
                TimeFilter filter = entry.getValue();
                String filterDesc = describeTimeFilter(filter);
                accessRightsModel.addRow(new Object[]{groupName, filterDesc});
            }
        }
    }
    
    private String describeTimeFilter(TimeFilter filter) {
        List<String> parts = new ArrayList<>();
        
        if (filter.getDaysOfWeek() != null && !filter.getDaysOfWeek().isEmpty()) {
            parts.add("Days: " + filter.getDaysOfWeek());
        }
        if (filter.getTimeRanges() != null && !filter.getTimeRanges().isEmpty()) {
            parts.add("Time Ranges: " + filter.getTimeRanges().size() + " range(s)");
        }
        
        return parts.isEmpty() ? "No restrictions" : String.join(", ", parts);
    }
    
    private void clearProfileInfo() {
        profileNameField.setText("");
        accessRightsModel.setRowCount(0);
        profileInfoArea.setText("");
    }
    
    private void createNewProfile() {
        String name = JOptionPane.showInputDialog(this, "Please enter profile name:", "New Profile", 
            JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Profile profile = new Profile(name.trim());
            ProfileManager profileManager = accessControlSystem.getProfileManager();
            try {
                profileManager.saveProfile(profile);
                loadProfiles();
                profileList.setSelectedValue(name.trim(), true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to create profile: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to delete", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete profile \"" + selected + "\"?", "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ProfileManager profileManager = accessControlSystem.getProfileManager();
            try {
                profileManager.deleteProfile(selected);
                loadProfiles();
                clearProfileInfo();
                JOptionPane.showMessageDialog(this, "Profile deleted successfully", "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to delete profile: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void addAccessRight() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select or create a profile first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String groupName = (String) groupCombo.getSelectedItem();
        if (groupName == null || groupName.startsWith("(No resource groups")) {
            JOptionPane.showMessageDialog(this, 
                "Please create resource groups first.\n\nResource group files should be placed in data/groups/ directory, in JSON format.\n" +
                "Example: {\"name\": \"Office Area\", \"securityLevel\": 1, \"resources\": [\"resource-id\"]}\n\n" +
                "After creating, please click the \"Refresh\" button to reload.", 
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if access right for this resource group already exists
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        Profile profile = profileManager.getProfile(selected);
        if (profile != null) {
            if (profile.getAccessRights().containsKey(groupName)) {
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Access right for this resource group already exists. Overwrite?", 
                    "Confirm", 
                    JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Create default time filter (allows all times)
            TimeFilter timeFilter = new TimeFilter();
            profile.addAccessRight(groupName, timeFilter);
            
            try {
                profileManager.saveProfile(profile);
                loadSelectedProfile();
                JOptionPane.showMessageDialog(this, "Access right added", "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to save profile: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void removeAccessRight() {
        int selectedRow = accessRightsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an access right to delete", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String groupName = (String) accessRightsTable.getValueAt(selectedRow, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete access right for resource group \"" + groupName + "\"?", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            ProfileManager profileManager = accessControlSystem.getProfileManager();
            Profile profile = profileManager.getProfile(selected);
            if (profile != null) {
                profile.removeAccessRight(groupName);
                try {
                    profileManager.saveProfile(profile);
                    loadSelectedProfile();
                    JOptionPane.showMessageDialog(this, "Access right deleted", "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Failed to save profile: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void editTimeFilter() {
        int selectedRow = accessRightsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an access right to edit", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String selected = profileList.getSelectedValue();
        if (selected == null) return;
        
        String groupName = (String) accessRightsTable.getValueAt(selectedRow, 0);
        
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        Profile profile = profileManager.getProfile(selected);
        if (profile != null) {
            TimeFilter filter = profile.getAccessRights().get(groupName);
            if (filter != null) {
                TimeFilterDialog dialog = new TimeFilterDialog((JFrame) SwingUtilities.getWindowAncestor(this), filter);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    loadSelectedProfile();
                }
            }
        }
    }
    
    private void modifyProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to modify", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        Profile profile = profileManager.getProfile(selected);
        if (profile == null) {
            JOptionPane.showMessageDialog(this, "Profile does not exist", "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create backup
        try {
            backupProfile(selected);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to create backup: " + e.getMessage(), 
                "Warning", JOptionPane.WARNING_MESSAGE);
        }
        
        // Allow user to modify name and access rights
        String newName = JOptionPane.showInputDialog(this, 
            "Please enter new profile name (leave empty to keep original name):", 
            "Modify Profile", 
            JOptionPane.QUESTION_MESSAGE);
        
        if (newName != null) {
            if (!newName.trim().isEmpty() && !newName.trim().equals(selected)) {
                profile.setName(newName.trim());
                try {
                    profileManager.deleteProfile(selected);
                } catch (Exception e) {
                    // Ignore delete error
                }
            }
            
            try {
                profileManager.saveProfile(profile);
                loadProfiles();
                profileList.setSelectedValue(profile.getName(), true);
                JOptionPane.showMessageDialog(this, "Profile modified successfully", "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to modify profile: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void restoreProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to restore", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Find backup files
        java.io.File backupDir = new java.io.File("data/profiles/backup");
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Backup directory not found", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Find backups for this profile
        java.io.File[] backupFiles = backupDir.listFiles((dir, name) -> 
            name.startsWith(selected + "_") && name.endsWith(".json"));
        
        if (backupFiles == null || backupFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No backups found for this profile", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Display backup list for user selection
        String[] backupNames = new String[backupFiles.length];
        for (int i = 0; i < backupFiles.length; i++) {
            String name = backupFiles[i].getName();
            // Extract timestamp
            String timestamp = name.substring(selected.length() + 1, name.length() - 5);
            backupNames[i] = "Backup time: " + timestamp;
        }
        
        String selectedBackup = (String) JOptionPane.showInputDialog(this,
            "Select backup to restore:",
            "Restore Profile",
            JOptionPane.QUESTION_MESSAGE,
            null,
            backupNames,
            backupNames[0]);
        
        if (selectedBackup != null) {
            int index = java.util.Arrays.asList(backupNames).indexOf(selectedBackup);
            java.io.File backupFile = backupFiles[index];
            
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to restore this backup? Current configuration will be overwritten.", 
                "Confirm Restore", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    // Read backup file
                    ProfileManager profileManager = accessControlSystem.getProfileManager();
                    Profile backupProfile = profileManager.loadProfileFromFile(backupFile);
                    
                    if (backupProfile != null) {
                        // Restore profile
                        profileManager.saveProfile(backupProfile);
                        loadProfiles();
                        profileList.setSelectedValue(backupProfile.getName(), true);
                        JOptionPane.showMessageDialog(this, "Profile restored successfully", "Success", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Restore failed: Unable to read backup file", "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Failed to restore profile: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void backupProfile(String profileName) throws java.io.IOException {
        java.io.File backupDir = new java.io.File("data/profiles/backup");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        java.io.File profileFile = new java.io.File("data/profiles", profileName + ".json");
        if (profileFile.exists()) {
            String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            java.io.File backupFile = new java.io.File(backupDir, profileName + "_" + timestamp + ".json");
            java.nio.file.Files.copy(profileFile.toPath(), backupFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    private void saveProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a profile to save", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String newName = profileNameField.getText().trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Profile name cannot be empty", "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create backup
        try {
            backupProfile(selected);
        } catch (Exception e) {
            // Ignore backup error
        }
        
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        Profile profile = profileManager.getProfile(selected);
        if (profile != null) {
            if (!selected.equals(newName)) {
                // Rename profile
                profile.setName(newName);
                try {
                    profileManager.deleteProfile(selected);
                } catch (Exception e) {
                    // Ignore delete error
                }
            }
            
            try {
                profileManager.saveProfile(profile);
                loadProfiles();
                profileList.setSelectedValue(newName, true);
                JOptionPane.showMessageDialog(this, "Profile saved successfully", "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to save profile: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Time filter editing dialog (full version)
     */
    private class TimeFilterDialog extends JDialog {
        private TimeFilter timeFilter;
        private TimeFilter originalFilter;
        private boolean confirmed = false;
        
        // UI components
        private JCheckBox[] dayOfWeekCheckboxes;
        private JSpinner startHourSpinner;
        private JSpinner startMinuteSpinner;
        private JSpinner endHourSpinner;
        private JSpinner endMinuteSpinner;
        private JCheckBox excludeDaysCheckbox;
        private JCheckBox excludeTimeRangeCheckbox;
        
        public TimeFilterDialog(JFrame parent, TimeFilter filter) {
            super(parent, "Edit Time Filter", true);
            this.originalFilter = filter;
            // Create a copy to avoid directly modifying the original object
            this.timeFilter = new TimeFilter();
            if (filter != null) {
                copyFilter(filter, this.timeFilter);
            }
            initializeDialog();
        }
        
        private void copyFilter(TimeFilter source, TimeFilter target) {
            if (source.getDaysOfWeek() != null) {
                target.setDaysOfWeek(new java.util.HashSet<>(source.getDaysOfWeek()));
            }
            if (source.getTimeRanges() != null) {
                target.setTimeRanges(new java.util.ArrayList<>(source.getTimeRanges()));
            }
            target.setExcludeDaysOfWeek(source.isExcludeDaysOfWeek());
            target.setExcludeTimeRanges(source.isExcludeTimeRanges());
        }
        
        private void initializeDialog() {
        setLayout(new BorderLayout());
            
            JPanel mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            
            // Day of week selection
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridwidth = 7;
            mainPanel.add(new JLabel("Allowed Days of Week:"), gbc);
            
            dayOfWeekCheckboxes = new JCheckBox[7];
            String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            java.time.DayOfWeek[] days = {
                java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, 
                java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY, 
                java.time.DayOfWeek.SUNDAY
            };
            
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            for (int i = 0; i < 7; i++) {
                gbc.gridx = i;
                dayOfWeekCheckboxes[i] = new JCheckBox(dayNames[i]);
                if (timeFilter.getDaysOfWeek() != null && 
                    timeFilter.getDaysOfWeek().contains(days[i])) {
                    dayOfWeekCheckboxes[i].setSelected(true);
                }
                mainPanel.add(dayOfWeekCheckboxes[i], gbc);
            }
            
            // Exclude days of week option
            gbc.gridx = 0; gbc.gridy = 2;
            gbc.gridwidth = 7;
            excludeDaysCheckbox = new JCheckBox("Exclude selected days (instead of allowing)");
            excludeDaysCheckbox.setSelected(timeFilter.isExcludeDaysOfWeek());
            mainPanel.add(excludeDaysCheckbox, gbc);
            
            // Time range
            gbc.gridx = 0; gbc.gridy = 3;
            gbc.gridwidth = 1;
            mainPanel.add(new JLabel("Time Range:"), gbc);
            
            gbc.gridx = 1;
            startHourSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
            mainPanel.add(startHourSpinner, gbc);
            
            gbc.gridx = 2;
            mainPanel.add(new JLabel(":"), gbc);
            
            gbc.gridx = 3;
            startMinuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
            mainPanel.add(startMinuteSpinner, gbc);
            
            gbc.gridx = 4;
            mainPanel.add(new JLabel(" to "), gbc);
            
            gbc.gridx = 5;
            endHourSpinner = new JSpinner(new SpinnerNumberModel(18, 0, 23, 1));
            mainPanel.add(endHourSpinner, gbc);
            
            gbc.gridx = 6;
            mainPanel.add(new JLabel(":"), gbc);
            
            gbc.gridx = 7;
            endMinuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
            mainPanel.add(endMinuteSpinner, gbc);
            
            // If there is an existing time range, load it
            if (timeFilter.getTimeRanges() != null && !timeFilter.getTimeRanges().isEmpty()) {
                TimeFilter.TimeRange range = timeFilter.getTimeRanges().get(0);
                int startMinutes = range.getStartMinutes();
                int endMinutes = range.getEndMinutes();
                startHourSpinner.setValue(startMinutes / 60);
                startMinuteSpinner.setValue(startMinutes % 60);
                endHourSpinner.setValue(endMinutes / 60);
                endMinuteSpinner.setValue(endMinutes % 60);
            }
            
            // Exclude time range option
            gbc.gridx = 0; gbc.gridy = 4;
            gbc.gridwidth = 8;
            excludeTimeRangeCheckbox = new JCheckBox("Exclude time range (instead of allowing)");
            excludeTimeRangeCheckbox.setSelected(timeFilter.isExcludeTimeRanges());
            mainPanel.add(excludeTimeRangeCheckbox, gbc);
            
            // Help text
            gbc.gridx = 0; gbc.gridy = 5;
            gbc.gridwidth = 8;
            gbc.fill = GridBagConstraints.BOTH;
            JTextArea infoArea = new JTextArea(3, 40);
            infoArea.setEditable(false);
            infoArea.setText("Note: If no days are selected, all days are allowed. If no time range is set, all times are allowed.");
            infoArea.setBackground(getBackground());
            mainPanel.add(infoArea, gbc);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(new JButton("OK") {{
                addActionListener(e -> {
                    applyChanges();
                    confirmed = true;
                    dispose();
                });
            }});
            buttonPanel.add(new JButton("Cancel") {{
                addActionListener(e -> dispose());
            }});
            
            add(new JScrollPane(mainPanel), BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);
            
            pack();
            setLocationRelativeTo(getParent());
        }
        
        private void applyChanges() {
            // Update days of week
            java.util.Set<java.time.DayOfWeek> selectedDays = new java.util.HashSet<>();
            java.time.DayOfWeek[] days = {
                java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, 
                java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY, 
                java.time.DayOfWeek.SUNDAY
            };
            
            for (int i = 0; i < 7; i++) {
                if (dayOfWeekCheckboxes[i].isSelected()) {
                    selectedDays.add(days[i]);
                }
            }
            
            if (!selectedDays.isEmpty()) {
                timeFilter.setDaysOfWeek(selectedDays);
            } else {
                timeFilter.setDaysOfWeek(null);
            }
            timeFilter.setExcludeDaysOfWeek(excludeDaysCheckbox.isSelected());
            
            // Update time range
            int startHour = (Integer) startHourSpinner.getValue();
            int startMinute = (Integer) startMinuteSpinner.getValue();
            int endHour = (Integer) endHourSpinner.getValue();
            int endMinute = (Integer) endMinuteSpinner.getValue();
            
            TimeFilter.TimeRange range = new TimeFilter.TimeRange(startHour, startMinute, endHour, endMinute);
            java.util.List<TimeFilter.TimeRange> ranges = new java.util.ArrayList<>();
            ranges.add(range);
            timeFilter.setTimeRanges(ranges);
            timeFilter.setExcludeTimeRanges(excludeTimeRangeCheckbox.isSelected());
            
            // Apply changes to original filter
            copyFilter(timeFilter, originalFilter);
        }
        
        public boolean isConfirmed() {
            return confirmed;
        }
    }
}
