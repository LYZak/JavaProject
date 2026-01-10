// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.profile.ResourceGroup;
import com.bigcomp.accesscontrol.profile.GroupManager;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.Resource;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.sql.PreparedStatement;

/**
 * Resource Group Management Panel
 */
public class ResourceGroupManagementPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JList<String> groupList;
    private DefaultListModel<String> groupListModel;
    private JTable resourceTable;
    private DefaultTableModel resourceTableModel;
    private JTextField groupNameField;
    private JSpinner securityLevelSpinner;
    private JTextArea groupInfoArea;
    private DatabaseManager dbManager;
    
    public ResourceGroupManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.dbManager = accessControlSystem.getDatabaseManager();
        initializeComponents();
        setupLayout();
        loadGroups();
    }
    
    private void initializeComponents() {
        // Resource group list
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedGroup();
                refreshAvailableResourceTable(); // Refresh available resource list
            }
        });
        
        // Resource table
        String[] columnNames = {"Resource ID", "Resource Name", "Type", "Location"};
        resourceTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resourceTable = new JTable(resourceTableModel);
        resourceTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resourceTable.setAutoCreateRowSorter(true);
        resourceTable.setFillsViewportHeight(true);
        styleTable(resourceTable);
        
        // Input fields
        groupNameField = new JTextField(20);
        securityLevelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        groupInfoArea = new JTextArea(5, 30);
        groupInfoArea.setEditable(false);
        groupInfoArea.setLineWrap(true);
        groupInfoArea.setWrapStyleWord(true);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        
        // Left: Resource group list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("Resource Groups"));
        leftPanel.add(new JScrollPane(groupList), BorderLayout.CENTER);
        
        JPanel leftButtonPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        leftButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JButton newGroupButton = new JButton("New Group");
        newGroupButton.addActionListener(e -> createNewGroup());
        JButton autoCreateButton = new JButton("Auto-create");
        autoCreateButton.addActionListener(e -> autoCreateGroups());
        JButton deleteButton = new JButton("Delete Resource Group");
        deleteButton.setForeground(Color.RED);
        deleteButton.addActionListener(e -> deleteGroup());
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
                loadGroups();
                if (groupList.getSelectedValue() != null) {
                    loadSelectedGroup();
                    refreshAvailableResourceTable();
                }
        });
        leftButtonPanel.add(newGroupButton);
        leftButtonPanel.add(autoCreateButton);
        leftButtonPanel.add(deleteButton);
        leftButtonPanel.add(refreshButton);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // Center: Resource list
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new TitledBorder("Group Details"));
        
        // Top: Resource group information
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        infoPanel.add(new JLabel("Resource Group Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        infoPanel.add(groupNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        infoPanel.add(new JLabel("Security Level:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        infoPanel.add(securityLevelSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton addResourceButton = new JButton("Add");
        addResourceButton.addActionListener(e -> addResourceToGroup());
        JButton removeResourceButton = new JButton("Remove");
        removeResourceButton.addActionListener(e -> removeResourceFromGroup());
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveGroup());
        JButton uncontrolledButton = new JButton("Set UNCONTROLLED");
        uncontrolledButton.setForeground(Color.RED);
        uncontrolledButton.setToolTipText("Set all resources in this group to UNCONTROLLED state (Emergency Open)");
        uncontrolledButton.addActionListener(e -> setGroupResourcesState(Resource.ResourceState.UNCONTROLLED));
        JButton controlledButton = new JButton("Set CONTROLLED");
        controlledButton.setToolTipText("Set all resources in this group to CONTROLLED state (Normal)");
        controlledButton.addActionListener(e -> setGroupResourcesState(Resource.ResourceState.CONTROLLED));
        buttonPanel.add(addResourceButton);
        buttonPanel.add(removeResourceButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(uncontrolledButton);
        buttonPanel.add(controlledButton);
        infoPanel.add(buttonPanel, gbc);
        
        centerPanel.add(infoPanel, BorderLayout.NORTH);
        JScrollPane groupResourcesScroll = new JScrollPane(resourceTable);
        groupResourcesScroll.setBorder(new EmptyBorder(8, 0, 8, 0));
        centerPanel.add(groupResourcesScroll, BorderLayout.CENTER);
        JScrollPane groupInfoScroll = new JScrollPane(groupInfoArea);
        groupInfoScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        centerPanel.add(groupInfoScroll, BorderLayout.SOUTH);
        
        // Right: Available resource list
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("Available Resources"));
        
        JTable availableResourceTable = createAvailableResourceTable();
        
        // Add double-click event: double-click resource row to directly add to resource group
        availableResourceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = availableResourceTable.rowAtPoint(evt.getPoint());
                    if (row >= 0) {
                        String resourceId = (String) availableResourceTableModel.getValueAt(row, 0);
                        String status = (String) availableResourceTableModel.getValueAt(row, 4);
                        if ("Not Added".equals(status)) {
                            addResourceToGroupById(resourceId);
                        } else {
                            JOptionPane.showMessageDialog(ResourceGroupManagementPanel.this,
                                "This resource has already been added to the current resource group", "Info",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        });
        
        rightPanel.add(new JScrollPane(availableResourceTable), BorderLayout.CENTER);
        
        // Add button panel
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rightButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JButton addToGroupButton = new JButton("Add Selected");
        addToGroupButton.addActionListener(e -> addSelectedResourcesFromTable());
        JButton refreshAvailableButton = new JButton("Refresh");
        refreshAvailableButton.addActionListener(e -> refreshAvailableResourceTable());
        rightButtonPanel.add(addToGroupButton);
        rightButtonPanel.add(refreshAvailableButton);
        rightPanel.add(rightButtonPanel, BorderLayout.SOUTH);
        
        // Main layout
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        leftSplit.setDividerLocation(200);
        leftSplit.setResizeWeight(0.2);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setDividerLocation(700);
        mainSplit.setResizeWeight(0.7);
        
        add(mainSplit, BorderLayout.CENTER);
    }
    
    private JTable availableResourceTable;
    private DefaultTableModel availableResourceTableModel;
    
    private JTable createAvailableResourceTable() {
        String[] columnNames = {"Resource ID", "Resource Name", "Type", "Location", "Status"};
        availableResourceTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        refreshAvailableResourceTable();
        
        availableResourceTable = new JTable(availableResourceTableModel);
        availableResourceTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availableResourceTable.setAutoCreateRowSorter(true);
        availableResourceTable.setFillsViewportHeight(true);
        styleTable(availableResourceTable);
        return availableResourceTable;
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
    
    /**
     * Refresh available resource table (show resources not added to currently selected resource group)
     */
    private void refreshAvailableResourceTable() {
        if (availableResourceTableModel == null) {
            return;
        }
        
        availableResourceTableModel.setRowCount(0);
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> allResources = dbManager.loadAllResources();
        
        // Get resource IDs already included in currently selected resource group
        Set<String> groupResourceIds = new HashSet<>();
        String selected = groupList.getSelectedValue();
        if (selected != null) {
            GroupManager groupManager = new GroupManager();
            ResourceGroup group = groupManager.getGroup(selected);
            if (group != null) {
                groupResourceIds.addAll(group.getResourceIds());
            }
        }
        
        for (Resource resource : allResources.values()) {
            boolean inGroup = groupResourceIds.contains(resource.getId());
            availableResourceTableModel.addRow(new Object[]{
                resource.getId(),
                resource.getName(),
                resource.getType().toString(),
                resource.getLocation(),
                inGroup ? "Added" : "Not Added"
            });
        }
    }
    
    private void loadGroups() {
        groupListModel.clear();
        GroupManager groupManager = new GroupManager();
        Map<String, ResourceGroup> groups = groupManager.getAllGroups();
        for (String name : groups.keySet()) {
            groupListModel.addElement(name);
        }
    }
    
    private void loadSelectedGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            clearGroupInfo();
            return;
        }
        
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        if (group != null) {
            groupNameField.setText(group.getName());
            securityLevelSpinner.setValue(group.getSecurityLevel());
            
            // Load resource list
            resourceTableModel.setRowCount(0);
            DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
            Map<String, Resource> allResources = dbManager.loadAllResources();
            
            for (String resourceId : group.getResourceIds()) {
                Resource resource = allResources.get(resourceId);
                if (resource != null) {
                    resourceTableModel.addRow(new Object[]{
                        resource.getId(),
                        resource.getName(),
                        resource.getType().toString(),
                        resource.getLocation()
                    });
                }
            }
            
            // Display resource group information
            groupInfoArea.setText(String.format(
                "Resource Group: %s\nSecurity Level: %d\nResource Count: %d\nFile Path: %s",
                group.getName(),
                group.getSecurityLevel(),
                group.getResourceIds().size(),
                group.getFilePath() != null ? group.getFilePath() : "Not saved"
            ));
        }
    }
    
    private void clearGroupInfo() {
        groupNameField.setText("");
        securityLevelSpinner.setValue(1);
        resourceTableModel.setRowCount(0);
        groupInfoArea.setText("");
    }
    
    private void createNewGroup() {
        String name = JOptionPane.showInputDialog(this, "Please enter resource group name:", "New Resource Group", 
            JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            int securityLevel = (Integer) securityLevelSpinner.getValue();
            ResourceGroup group = new ResourceGroup(name.trim(), securityLevel);
            GroupManager groupManager = new GroupManager();
            try {
                groupManager.saveGroup(group);
                loadGroups();
                groupList.setSelectedValue(name.trim(), true);
                JOptionPane.showMessageDialog(this, "Resource group created successfully", "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to create resource group: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select a resource group from the left list to delete", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get resource group information
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        int resourceCount = group != null ? group.getResourceIds().size() : 0;
        
        String message = String.format(
            "Are you sure you want to delete resource group \"%s\"?\n\n" +
            "Resource Group Information:\n" +
            "• Name: %s\n" +
            "• Security Level: %d\n" +
            "• Resource Count: %d\n\n" +
            "Warning: This operation will:\n" +
            "• Delete resource group JSON file\n" +
            "• Delete associated records in database\n" +
            "• This operation cannot be undone!",
            selected, 
            selected,
            group != null ? group.getSecurityLevel() : 0,
            resourceCount
        );
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            message, 
            "Confirm Delete Resource Group", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Before deleting resource group, remove references to this resource group from all profiles
                ProfileManager profileManager = accessControlSystem.getProfileManager();
                Map<String, Profile> allProfiles = profileManager.getAllProfiles();
                int updatedProfileCount = 0;
                
                for (Profile profile : allProfiles.values()) {
                    if (profile.getAccessRights().containsKey(selected)) {
                        // Remove reference to this resource group from profile
                        profile.removeAccessRight(selected);
                        try {
                            profileManager.saveProfile(profile);
                            updatedProfileCount++;
                        } catch (Exception e) {
                            System.err.println("Failed to update profile: " + profile.getName() + " - " + e.getMessage());
                        }
                    }
                }
                
                // Delete resource group
                groupManager.deleteGroup(selected);
                
                // Delete associated records from database
                try {
                    String sql = "DELETE FROM resource_group_members WHERE group_name = ?";
                    try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
                        pstmt.setString(1, selected);
                        pstmt.executeUpdate();
                    }
                } catch (Exception e) {
                    // Ignore database delete errors, table may not exist or already deleted
                }
                
                // Reload data
                accessControlSystem.getAccessRequestProcessor().reloadData();
                loadGroups();
                clearGroupInfo();
                refreshAvailableResourceTable();
                
                String successMessage = "Resource group \"" + selected + "\" has been successfully deleted";
                if (updatedProfileCount > 0) {
                    successMessage += "\n\nRemoved references to this resource group from " + updatedProfileCount + " profiles";
                }
                
                JOptionPane.showMessageDialog(this, 
                    successMessage, "Delete Successful", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to delete resource group: " + e.getMessage() + "\n\nPlease check file permissions and database connection.", 
                    "Delete Failed", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void addResourceToGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select or create a resource group first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Show available resource selection dialog
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> allResources = dbManager.loadAllResources();
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        
        if (group == null) {
            return;
        }
        
        // Get resources not added to this group
        List<String> availableResourceIds = new ArrayList<>();
        List<String> availableResourceNames = new ArrayList<>();
        
        for (Resource resource : allResources.values()) {
            if (!group.getResourceIds().contains(resource.getId())) {
                availableResourceIds.add(resource.getId());
                availableResourceNames.add(resource.getName() + " (" + resource.getId().substring(0, 8) + "...)");
            }
        }
        
        if (availableResourceIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No resources available to add", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String selectedResourceName = (String) JOptionPane.showInputDialog(this,
            "Select resource to add to resource group:",
            "Add Resource",
            JOptionPane.QUESTION_MESSAGE,
            null,
            availableResourceNames.toArray(),
            availableResourceNames.get(0));
        
        if (selectedResourceName != null) {
            int index = availableResourceNames.indexOf(selectedResourceName);
            String resourceId = availableResourceIds.get(index);
            
            group.addResource(resourceId);
            try {
                groupManager.saveGroup(group);
                dbManager.linkResourceToGroup(resourceId, selected);
                accessControlSystem.getAccessRequestProcessor().reloadData();
                loadSelectedGroup();
                refreshAvailableResourceTable(); // Refresh available resource list
                JOptionPane.showMessageDialog(this, "Resource added to resource group", "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to add resource: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Add resource to resource group directly by resource ID
     */
    private void addResourceToGroupById(String resourceId) {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select or create a resource group first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        
        if (group == null) {
            return;
        }
        
        // Check if resource is already in group
        if (group.getResourceIds().contains(resourceId)) {
            JOptionPane.showMessageDialog(this, "This resource is already in this resource group", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        group.addResource(resourceId);
        try {
            groupManager.saveGroup(group);
            dbManager.linkResourceToGroup(resourceId, selected);
            accessControlSystem.getAccessRequestProcessor().reloadData();
            loadSelectedGroup();
            refreshAvailableResourceTable();
            JOptionPane.showMessageDialog(this, "Resource added to resource group", "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to add resource: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Add selected resources from right table to resource group
     */
    private void addSelectedResourcesFromTable() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select or create a resource group first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int[] selectedRows = availableResourceTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select resources to add first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        
        if (group == null) {
            return;
        }
        
        int addedCount = 0;
        int skippedCount = 0;
        
        for (int row : selectedRows) {
            String resourceId = (String) availableResourceTableModel.getValueAt(row, 0);
            String status = (String) availableResourceTableModel.getValueAt(row, 4);
            
            if ("Added".equals(status)) {
                skippedCount++;
                continue;
            }
            
            if (!group.getResourceIds().contains(resourceId)) {
                group.addResource(resourceId);
                try {
                    dbManager.linkResourceToGroup(resourceId, selected);
                    addedCount++;
                } catch (Exception e) {
                    // Ignore single resource add errors
                }
            }
        }
        
        if (addedCount > 0) {
            try {
                groupManager.saveGroup(group);
                accessControlSystem.getAccessRequestProcessor().reloadData();
                loadSelectedGroup();
                refreshAvailableResourceTable();
                JOptionPane.showMessageDialog(this, 
                    String.format("Successfully added %d resources to resource group\nSkipped %d existing resources", 
                        addedCount, skippedCount), 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to save resource group: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (skippedCount > 0) {
            JOptionPane.showMessageDialog(this, 
                "All selected resources have already been added to the resource group", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void removeResourceFromGroup() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resource to remove", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            return;
        }
        
        String resourceId = (String) resourceTableModel.getValueAt(selectedRow, 0);
        String resourceName = (String) resourceTableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to remove resource \"" + resourceName + "\" from the resource group?", 
            "Confirm Remove", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            GroupManager groupManager = new GroupManager();
            ResourceGroup group = groupManager.getGroup(selected);
            if (group != null) {
                group.removeResource(resourceId);
                try {
                    groupManager.saveGroup(group);
                    // Delete association from database
                    String sql = "DELETE FROM resource_group_members WHERE resource_id = ? AND group_name = ?";
                    try (PreparedStatement pstmt = accessControlSystem.getDatabaseManager().getConnection().prepareStatement(sql)) {
                        pstmt.setString(1, resourceId);
                        pstmt.setString(2, selected);
                        pstmt.executeUpdate();
                    }
                    accessControlSystem.getAccessRequestProcessor().reloadData();
                    loadSelectedGroup();
                    refreshAvailableResourceTable(); // Refresh available resource list
                    JOptionPane.showMessageDialog(this, "Resource removed from resource group", "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Failed to remove resource: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void saveGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a resource group to save", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String newName = groupNameField.getText().trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Resource group name cannot be empty", "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int securityLevel = (Integer) securityLevelSpinner.getValue();
        
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        if (group != null) {
            // If name changed, need to rename
            if (!selected.equals(newName)) {
                group.setName(newName);
                try {
                    groupManager.deleteGroup(selected);
                } catch (Exception e) {
                    // Ignore delete error
                }
            }
            
            group.setSecurityLevel(securityLevel);
            
            try {
                groupManager.saveGroup(group);
                loadGroups();
                groupList.setSelectedValue(newName, true);
                JOptionPane.showMessageDialog(this, "Resource group saved successfully", "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to save resource group: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Batch set state for all resources in the selected group
     */
    private void setGroupResourcesState(Resource.ResourceState state) {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a resource group", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        if (group == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Set all resources in group \"" + selected + "\" to " + state + "?", 
            "Confirm State Change", 
            JOptionPane.YES_NO_OPTION);
            
        if (confirm != JOptionPane.YES_OPTION) return;
        
        try {
            DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
            Map<String, Resource> allResources = dbManager.loadAllResources();
            
            int count = 0;
            for (String resourceId : group.getResourceIds()) {
                Resource resource = allResources.get(resourceId);
                if (resource != null) {
                    resource.setState(state);
                    dbManager.addResource(resource); // Save to DB
                    count++;
                }
            }
            
            // Reload in-memory data
            accessControlSystem.getAccessRequestProcessor().reloadData();
            loadSelectedGroup(); // Refresh table display
            
            JOptionPane.showMessageDialog(this, 
                "Updated " + count + " resources to " + state, 
                "Success", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
             JOptionPane.showMessageDialog(this, "Failed to update resources: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Automatically create resource groups based on existing resources
     * Automatically group by building, floor, type and other attributes
     */
    private void autoCreateGroups() {
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> allResources = dbManager.loadAllResources();
        
        if (allResources.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No available resources, please create resources in Resource Management first", 
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Show grouping strategy selection dialog
        String[] strategies = {
            "Group by Building",
            "Group by Floor", 
            "Group by Resource Type",
            "Group by Building + Floor",
            "Group by Building + Type",
            "Group by Floor + Type",
            "Create All Possible Combinations"
        };
        
        String selectedStrategy = (String) JOptionPane.showInputDialog(this,
            "Select resource group creation strategy:",
            "Auto-create Resource Groups",
            JOptionPane.QUESTION_MESSAGE,
            null,
            strategies,
            strategies[6]); // Default to "Create All Possible Combinations"
        
        if (selectedStrategy == null) {
            return;
        }
        
        GroupManager groupManager = new GroupManager();
        int createdCount = 0;
        int skippedCount = 0;
        
        try {
            if ("Create All Possible Combinations".equals(selectedStrategy)) {
                // Create all possible combinations
                createdCount = createAllPossibleGroups(allResources, groupManager);
            } else {
                // Create based on selected strategy
                createdCount = createGroupsByStrategy(allResources, groupManager, selectedStrategy);
            }
            
            // Refresh list
            loadGroups();
            
            JOptionPane.showMessageDialog(this, 
                String.format("Successfully created %d resource groups\nSkipped %d existing resource groups", 
                    createdCount, skippedCount), 
                "Complete", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error creating resource groups: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private int createGroupsByStrategy(Map<String, Resource> resources, 
                                      GroupManager groupManager, 
                                      String strategy) throws Exception {
        Map<String, List<String>> groupMap = new HashMap<>();
        
        for (Resource resource : resources.values()) {
            String groupKey = getGroupKey(resource, strategy);
            if (groupKey != null && !groupKey.isEmpty()) {
                groupMap.computeIfAbsent(groupKey, k -> new ArrayList<>())
                        .add(resource.getId());
            }
        }
        
        int created = 0;
        for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
            String groupName = entry.getKey();
            List<String> resourceIds = entry.getValue();
            
            // Check if already exists
            ResourceGroup existingGroup = groupManager.getGroup(groupName);
            if (existingGroup != null) {
                // If exists, check if new resources need to be added
                boolean hasNewResources = false;
                for (String resourceId : resourceIds) {
                    if (!existingGroup.getResourceIds().contains(resourceId)) {
                        existingGroup.addResource(resourceId);
                        hasNewResources = true;
                        try {
                            dbManager.linkResourceToGroup(resourceId, groupName);
                        } catch (Exception e) {
                            // Ignore association errors
                        }
                    }
                }
                if (hasNewResources) {
                    groupManager.saveGroup(existingGroup);
                }
                continue;
            }
            
            // Create new resource group
            ResourceGroup group = new ResourceGroup(groupName, calculateSecurityLevel(resourceIds, resources));
            for (String resourceId : resourceIds) {
                group.addResource(resourceId);
            }
            
            groupManager.saveGroup(group);
            
            // Link to database
            for (String resourceId : resourceIds) {
                try {
                    dbManager.linkResourceToGroup(resourceId, groupName);
                } catch (Exception e) {
                    // Ignore association errors
                }
            }
            
            created++;
        }
        
        return created;
    }
    
    private int createAllPossibleGroups(Map<String, Resource> resources, 
                                       GroupManager groupManager) throws Exception {
        int created = 0;
        
        // 1. Group by building
        created += createGroupsByStrategy(resources, groupManager, "Group by Building");
        
        // 2. Group by floor
        created += createGroupsByStrategy(resources, groupManager, "Group by Floor");
        
        // 3. Group by type
        created += createGroupsByStrategy(resources, groupManager, "Group by Resource Type");
        
        // 4. Group by building + floor
        created += createGroupsByStrategy(resources, groupManager, "Group by Building + Floor");
        
        // 5. Group by building + type
        created += createGroupsByStrategy(resources, groupManager, "Group by Building + Type");
        
        // 6. Group by floor + type
        created += createGroupsByStrategy(resources, groupManager, "Group by Floor + Type");
        
        return created;
    }
    
    private String getGroupKey(Resource resource, String strategy) {
        String building = resource.getBuilding() != null ? resource.getBuilding() : "Unspecified Building";
        String floor = resource.getFloor() != null ? resource.getFloor() : "Unspecified Floor";
        String type = resource.getType() != null ? resource.getType().toString() : "Unspecified Type";
        
        switch (strategy) {
            case "Group by Building":
                return building + " Area";
            case "Group by Floor":
                return floor + " Floor";
            case "Group by Resource Type":
                return type + " Resource Group";
            case "Group by Building + Floor":
                return building + "-" + floor;
            case "Group by Building + Type":
                return building + "-" + type;
            case "Group by Floor + Type":
                return floor + "-" + type;
            default:
                return null;
        }
    }
    
    /**
     * Calculate security level based on resource list
     * Simple strategy: calculate based on resource type and count
     */
    private int calculateSecurityLevel(List<String> resourceIds, Map<String, Resource> allResources) {
        if (resourceIds.isEmpty()) {
            return 1;
        }
        
        // Check if there are high security level resource types
        int maxLevel = 1;
        for (String resourceId : resourceIds) {
            Resource resource = allResources.get(resourceId);
            if (resource != null) {
                Resource.ResourceType type = resource.getType();
                // Set security level based on type
                if (type == Resource.ResourceType.GATE || 
                    type == Resource.ResourceType.PARKING) {
                    maxLevel = Math.max(maxLevel, 1); // Public area
                } else if (type == Resource.ResourceType.DOOR || 
                          type == Resource.ResourceType.STAIRWAY) {
                    maxLevel = Math.max(maxLevel, 2); // General area
                } else if (type == Resource.ResourceType.ELEVATOR) {
                    maxLevel = Math.max(maxLevel, 3); // Important area
                } else {
                    maxLevel = Math.max(maxLevel, 2);
                }
            }
        }
        
        return maxLevel;
    }
}

