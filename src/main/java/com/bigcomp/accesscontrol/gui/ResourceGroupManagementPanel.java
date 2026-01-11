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
    private TitledBorder leftBorder;
    private TitledBorder centerBorder;
    private TitledBorder rightBorder;
    private JLabel groupNameLabel;
    private JLabel securityLevelLabel;
    private JButton newGroupButton;
    private JButton autoCreateButton;
    private JButton deleteGroupButton;
    private JButton refreshGroupsButton;
    private JButton addResourceButton;
    private JButton removeResourceButton;
    private JButton saveButton;
    private JButton uncontrolledButton;
    private JButton controlledButton;
    private JButton addSelectedButton;
    private JButton refreshAvailableButton;
    
    public ResourceGroupManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.dbManager = accessControlSystem.getDatabaseManager();
        initializeComponents();
        setupLayout();
        applyLanguage();
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
        resourceTableModel = new DefaultTableModel(getGroupResourceColumnNames(), 0) {
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
        leftBorder = new TitledBorder("");
        leftPanel.setBorder(leftBorder);
        JScrollPane groupListScroll = new JScrollPane(groupList);
        groupListScroll.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.add(groupListScroll, BorderLayout.CENTER);
        
        JPanel leftButtonPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        leftButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        newGroupButton = new JButton();
        newGroupButton.addActionListener(e -> createNewGroup());
        autoCreateButton = new JButton();
        autoCreateButton.addActionListener(e -> autoCreateGroups());
        deleteGroupButton = new JButton();
        deleteGroupButton.setForeground(Color.RED);
        deleteGroupButton.addActionListener(e -> deleteGroup());
        refreshGroupsButton = new JButton();
        refreshGroupsButton.addActionListener(e -> {
                loadGroups();
                if (groupList.getSelectedValue() != null) {
                    loadSelectedGroup();
                    refreshAvailableResourceTable();
                }
        });
        leftButtonPanel.add(newGroupButton);
        leftButtonPanel.add(autoCreateButton);
        leftButtonPanel.add(deleteGroupButton);
        leftButtonPanel.add(refreshGroupsButton);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // Center: Resource list
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerBorder = new TitledBorder("");
        centerPanel.setBorder(centerBorder);
        
        // Top: Resource group information
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        groupNameLabel = new JLabel();
        infoPanel.add(groupNameLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        infoPanel.add(groupNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        securityLevelLabel = new JLabel();
        infoPanel.add(securityLevelLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        infoPanel.add(securityLevelSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        addResourceButton = new JButton();
        addResourceButton.addActionListener(e -> addResourceToGroup());
        removeResourceButton = new JButton();
        removeResourceButton.addActionListener(e -> removeResourceFromGroup());
        saveButton = new JButton();
        saveButton.addActionListener(e -> saveGroup());
        uncontrolledButton = new JButton();
        uncontrolledButton.setForeground(Color.RED);
        uncontrolledButton.addActionListener(e -> setGroupResourcesState(Resource.ResourceState.UNCONTROLLED));
        controlledButton = new JButton();
        controlledButton.addActionListener(e -> setGroupResourcesState(Resource.ResourceState.CONTROLLED));
        buttonPanel.add(addResourceButton);
        buttonPanel.add(removeResourceButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(uncontrolledButton);
        buttonPanel.add(controlledButton);
        infoPanel.add(buttonPanel, gbc);
        
        centerPanel.add(infoPanel, BorderLayout.NORTH);
        JScrollPane groupResourcesScroll = new JScrollPane(resourceTable);
        groupResourcesScroll.setBorder(BorderFactory.createEmptyBorder());
        centerPanel.add(groupResourcesScroll, BorderLayout.CENTER);
        JScrollPane groupInfoScroll = new JScrollPane(groupInfoArea);
        groupInfoScroll.setBorder(BorderFactory.createEmptyBorder());
        centerPanel.add(groupInfoScroll, BorderLayout.SOUTH);
        
        // Right: Available resource list
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightBorder = new TitledBorder("");
        rightPanel.setBorder(rightBorder);
        
        JTable availableResourceTable = createAvailableResourceTable();
        
        // Add double-click event: double-click resource row to directly add to resource group
        availableResourceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = availableResourceTable.rowAtPoint(evt.getPoint());
                    if (row >= 0) {
                        int modelRow = availableResourceTable.convertRowIndexToModel(row);
                        String resourceId = (String) availableResourceTableModel.getValueAt(modelRow, 0);
                        addResourceToGroupById(resourceId);
                    }
                }
            }
        });
        
        JScrollPane availableResourceScroll = new JScrollPane(availableResourceTable);
        availableResourceScroll.setBorder(BorderFactory.createEmptyBorder());
        rightPanel.add(availableResourceScroll, BorderLayout.CENTER);
        
        // Add button panel
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rightButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        addSelectedButton = new JButton();
        addSelectedButton.addActionListener(e -> addSelectedResourcesFromTable());
        refreshAvailableButton = new JButton();
        refreshAvailableButton.addActionListener(e -> refreshAvailableResourceTable());
        rightButtonPanel.add(addSelectedButton);
        rightButtonPanel.add(refreshAvailableButton);
        rightPanel.add(rightButtonPanel, BorderLayout.SOUTH);
        
        // Main layout
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        leftSplit.setResizeWeight(0.5);
        leftSplit.setDividerSize(8);
        leftSplit.setContinuousLayout(true);
        leftSplit.setBorder(null);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setResizeWeight(0.66);
        mainSplit.setDividerSize(8);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        
        add(mainSplit, BorderLayout.CENTER);
    }
    
    private JTable availableResourceTable;
    private DefaultTableModel availableResourceTableModel;
    
    private JTable createAvailableResourceTable() {
        availableResourceTableModel = new DefaultTableModel(getAvailableResourceColumnNames(), 0) {
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

    public void applyLanguage() {
        leftBorder.setTitle(I18n.t("group.title.list"));
        centerBorder.setTitle(I18n.t("group.title.details"));
        rightBorder.setTitle(I18n.t("group.title.available"));

        newGroupButton.setText(I18n.t("group.action.new"));
        autoCreateButton.setText(I18n.t("group.action.autoCreate"));
        deleteGroupButton.setText(I18n.t("group.action.delete"));
        refreshGroupsButton.setText(I18n.t("common.refresh"));

        groupNameLabel.setText(I18n.t("group.field.name"));
        securityLevelLabel.setText(I18n.t("group.field.security"));

        addResourceButton.setText(I18n.t("common.add"));
        removeResourceButton.setText(I18n.t("common.remove"));
        saveButton.setText(I18n.t("common.save"));
        uncontrolledButton.setText(I18n.t("group.action.uncontrolled"));
        uncontrolledButton.setToolTipText(I18n.t("group.tip.uncontrolled"));
        controlledButton.setText(I18n.t("group.action.controlled"));
        controlledButton.setToolTipText(I18n.t("group.tip.controlled"));

        addSelectedButton.setText(I18n.t("group.action.addSelected"));
        refreshAvailableButton.setText(I18n.t("common.refresh"));

        resourceTableModel.setColumnIdentifiers(getGroupResourceColumnNames());
        availableResourceTableModel.setColumnIdentifiers(getAvailableResourceColumnNames());
        resourceTable.getTableHeader().repaint();
        if (availableResourceTable != null) {
            availableResourceTable.getTableHeader().repaint();
        }
        revalidate();
        repaint();
    }

    private String[] getGroupResourceColumnNames() {
        return new String[]{
            I18n.t("group.col.resourceId"),
            I18n.t("group.col.resourceName"),
            I18n.t("group.col.type"),
            I18n.t("group.col.location")
        };
    }

    private String[] getAvailableResourceColumnNames() {
        return new String[]{
            I18n.t("group.col.resourceId"),
            I18n.t("group.col.resourceName"),
            I18n.t("group.col.type"),
            I18n.t("group.col.location")
        };
    }

    private void styleTable(JTable table) {
        table.setRowHeight(Math.max(table.getRowHeight(), 28));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(true);
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

        Set<String> groupedResourceIds = new HashSet<>();
        try {
            groupedResourceIds.addAll(dbManager.loadResourceGroups().keySet());
        } catch (Exception e) {
        }
        try {
            GroupManager groupManager = new GroupManager();
            for (ResourceGroup group : groupManager.getAllGroups().values()) {
                if (group != null) {
                    groupedResourceIds.addAll(group.getResourceIds());
                }
            }
        } catch (Exception e) {
        }
        
        for (Resource resource : allResources.values()) {
            if (groupedResourceIds.contains(resource.getId())) {
                continue;
            }
            availableResourceTableModel.addRow(new Object[]{
                resource.getId(),
                resource.getName(),
                I18n.t("resource.type." + resource.getType().name()),
                resource.getLocation()
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
                        I18n.t("resource.type." + resource.getType().name()),
                        resource.getLocation()
                    });
                }
            }
            
            // Display resource group information
            groupInfoArea.setText(I18n.f("group.info.details",
                group.getName(),
                group.getSecurityLevel(),
                group.getResourceIds().size(),
                group.getFilePath() != null ? group.getFilePath() : I18n.t("group.info.notSaved")
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
        String name = JOptionPane.showInputDialog(this, I18n.t("group.msg.enterName"), I18n.t("group.msg.newTitle"), 
            JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            int securityLevel = (Integer) securityLevelSpinner.getValue();
            ResourceGroup group = new ResourceGroup(name.trim(), securityLevel);
            GroupManager groupManager = new GroupManager();
            try {
                groupManager.saveGroup(group);
                loadGroups();
                groupList.setSelectedValue(name.trim(), true);
                JOptionPane.showMessageDialog(this, I18n.t("group.msg.created"), I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("group.msg.createFailed", e.getMessage()), 
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, 
                I18n.t("group.msg.selectDelete"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get resource group information
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        int resourceCount = group != null ? group.getResourceIds().size() : 0;
        
        String message = I18n.f("group.msg.confirmDelete",
            selected, 
            selected,
            group != null ? group.getSecurityLevel() : 0,
            resourceCount
        );
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            message, 
            I18n.t("group.msg.confirmDeleteTitle"), 
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
                
                String successMessage = I18n.t("group.msg.deleted");
                
                JOptionPane.showMessageDialog(this, 
                    successMessage, I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    I18n.f("group.msg.deleteFailed", e.getMessage()), 
                    I18n.t("common.error"), 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void addResourceToGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.noGroupSelected"), I18n.t("common.warning"), 
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
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.noResources"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String selectedResourceName = (String) JOptionPane.showInputDialog(this,
            I18n.t("group.msg.selectToAdd"),
            I18n.t("group.action.add"),
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
                JOptionPane.showMessageDialog(this, I18n.t("group.msg.resourceAdded"), I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("group.msg.addResourceFailed", e.getMessage()), 
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Add resource to resource group directly by resource ID
     */
    private void addResourceToGroupById(String resourceId) {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.noGroupSelected"), I18n.t("common.warning"), 
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
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.resourceAlreadyIn"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String existingGroupName = dbManager.loadResourceGroups().get(resourceId);
        if (existingGroupName != null && !existingGroupName.isBlank() && !existingGroupName.equals(selected)) {
            JOptionPane.showMessageDialog(this, I18n.f("group.msg.resourceInOtherGroup", I18n.t(existingGroupName)), I18n.t("common.warning"),
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        group.addResource(resourceId);
        try {
            groupManager.saveGroup(group);
            dbManager.linkResourceToGroup(resourceId, selected);
            accessControlSystem.getAccessRequestProcessor().reloadData();
            loadSelectedGroup();
            refreshAvailableResourceTable();
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.resourceAdded"), I18n.t("common.success"), 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("group.msg.addResourceFailed", e.getMessage()), 
                I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Add selected resources from right table to resource group
     */
    private void addSelectedResourcesFromTable() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.noGroupSelected"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int[] selectedRows = availableResourceTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.selectToAdd"), I18n.t("common.warning"), 
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
            int modelRow = availableResourceTable.convertRowIndexToModel(row);
            String resourceId = (String) availableResourceTableModel.getValueAt(modelRow, 0);

            String existingGroupName = dbManager.loadResourceGroups().get(resourceId);
            if (existingGroupName != null && !existingGroupName.isBlank() && !existingGroupName.equals(selected)) {
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
                    I18n.f("group.msg.addResources.done", addedCount, skippedCount), 
                    I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("group.msg.saveFailed", e.getMessage()), 
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        } else if (skippedCount > 0) {
            JOptionPane.showMessageDialog(this, 
                I18n.t("group.msg.resourceAlreadyIn"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void removeResourceFromGroup() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.selectToAdd"), I18n.t("common.warning"), 
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
            I18n.f("group.msg.confirmRemove", resourceName), 
            I18n.t("group.msg.confirmRemoveTitle"), 
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
                    JOptionPane.showMessageDialog(this, I18n.t("group.msg.resourceRemoved"), I18n.t("common.success"), 
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, I18n.f("group.msg.addResourceFailed", e.getMessage()), 
                        I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void saveGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.noGroupSelected"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String newName = groupNameField.getText().trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("user.msg.enterName"), I18n.t("common.error"), 
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
                JOptionPane.showMessageDialog(this, I18n.t("group.msg.saveSuccess"), I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("group.msg.saveFailed", e.getMessage()), 
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Batch set state for all resources in the selected group
     */
    private void setGroupResourcesState(Resource.ResourceState state) {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("group.msg.noGroupSelected"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        if (group == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            I18n.f("group.msg.confirmStateChange", selected, state), 
            I18n.t("group.msg.confirmStateChangeTitle"), 
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
                I18n.f("group.msg.updateStateSuccess", count, state), 
                I18n.t("common.success"), JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
             JOptionPane.showMessageDialog(this, I18n.f("group.msg.updateResourcesFailed", e.getMessage()), 
                I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
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
                I18n.t("group.msg.noResources"), 
                I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Show grouping strategy selection dialog
        String[] strategyKeys = {
            "group.strategy.building",
            "group.strategy.floor", 
            "group.strategy.type",
            "group.strategy.buildingFloor",
            "group.strategy.buildingType",
            "group.strategy.floorType",
            "group.strategy.all"
        };
        
        String[] strategyLabels = new String[strategyKeys.length];
        for (int i = 0; i < strategyKeys.length; i++) {
            strategyLabels[i] = I18n.t(strategyKeys[i]);
        }
        
        String selectedStrategyLabel = (String) JOptionPane.showInputDialog(this,
            I18n.t("group.msg.selectStrategy"),
            I18n.t("group.msg.autoCreateTitle"),
            JOptionPane.QUESTION_MESSAGE,
            null,
            strategyLabels,
            strategyLabels[strategyLabels.length - 1]); // Default to "Create All Possible Combinations"
        
        if (selectedStrategyLabel == null) {
            return;
        }
        
        // Find back the key from label
        String selectedStrategyKey = null;
        for (int i = 0; i < strategyLabels.length; i++) {
            if (strategyLabels[i].equals(selectedStrategyLabel)) {
                selectedStrategyKey = strategyKeys[i];
                break;
            }
        }
        
        GroupManager groupManager = new GroupManager();
        
        try {
            int[] results;
            if ("group.strategy.all".equals(selectedStrategyKey)) {
                // Create all possible combinations
                results = createAllPossibleGroups(allResources, groupManager);
            } else {
                // Create based on selected strategy
                results = createGroupsByStrategy(allResources, groupManager, selectedStrategyKey);
            }
            
            int createdCount = results[0];
            int skippedCount = results[1];
            
            // Refresh list
            loadGroups();
            
            JOptionPane.showMessageDialog(this, 
                I18n.f("group.msg.autoCreate.done", createdCount, skippedCount), 
                I18n.t("common.success"), 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                I18n.f("group.msg.autoCreateError", e.getMessage()), 
                I18n.t("common.error"), 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private int[] createGroupsByStrategy(Map<String, Resource> resources, 
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
        int skipped = 0;
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
                    created++; // Technically updated, but we count it as "processed/created" in this context
                } else {
                    skipped++;
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
        
        return new int[]{created, skipped};
    }
    
    private int[] createAllPossibleGroups(Map<String, Resource> resources, 
                                       GroupManager groupManager) throws Exception {
        Map<String, List<String>> groupMap = new HashMap<>();
        for (Resource resource : resources.values()) {
            String groupKey = getAllStrategyGroupKey(resource);
            if (groupKey != null && !groupKey.isEmpty()) {
                groupMap.computeIfAbsent(groupKey, k -> new ArrayList<>())
                    .add(resource.getId());
            }
        }
        return createGroupsByGroupMap(resources, groupManager, groupMap);
    }

    private int[] createGroupsByGroupMap(Map<String, Resource> resources,
                                        GroupManager groupManager,
                                        Map<String, List<String>> groupMap) throws Exception {
        int created = 0;
        int skipped = 0;
        for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
            String groupName = entry.getKey();
            List<String> resourceIds = entry.getValue();

            ResourceGroup existingGroup = groupManager.getGroup(groupName);
            if (existingGroup != null) {
                boolean hasNewResources = false;
                for (String resourceId : resourceIds) {
                    if (!existingGroup.getResourceIds().contains(resourceId)) {
                        existingGroup.addResource(resourceId);
                        hasNewResources = true;
                        try {
                            dbManager.linkResourceToGroup(resourceId, groupName);
                        } catch (Exception e) {
                        }
                    }
                }
                if (hasNewResources) {
                    groupManager.saveGroup(existingGroup);
                    created++;
                } else {
                    skipped++;
                }
                continue;
            }

            ResourceGroup group = new ResourceGroup(groupName, calculateSecurityLevel(resourceIds, resources));
            for (String resourceId : resourceIds) {
                group.addResource(resourceId);
            }

            groupManager.saveGroup(group);

            for (String resourceId : resourceIds) {
                try {
                    dbManager.linkResourceToGroup(resourceId, groupName);
                } catch (Exception e) {
                }
            }
            created++;
        }
        return new int[]{created, skipped};
    }

    private String getAllStrategyGroupKey(Resource resource) {
        if (resource == null) {
            return null;
        }
        String building = resource.getBuilding();
        String floor = resource.getFloor();
        String type = resource.getType() != null ? resource.getType().toString() : null;
        if (building == null || building.isBlank() || floor == null || floor.isBlank() || type == null || type.isBlank()) {
            return null;
        }
        return building + " - " + floor + " - " + type;
    }
    
    private String getGroupKey(Resource resource, String strategyKey) {
        if ("group.strategy.building".equals(strategyKey)) {
            return resource.getBuilding();
        } else if ("group.strategy.floor".equals(strategyKey)) {
            return I18n.f("group.strategy.floor.pattern", resource.getFloor());
        } else if ("group.strategy.type".equals(strategyKey)) {
            return resource.getType().toString();
        } else if ("group.strategy.buildingFloor".equals(strategyKey)) {
            return I18n.f("group.strategy.buildingFloor.pattern", resource.getBuilding(), resource.getFloor());
        } else if ("group.strategy.buildingType".equals(strategyKey)) {
            return I18n.f("group.strategy.buildingType.pattern", resource.getBuilding(), resource.getType());
        } else if ("group.strategy.floorType".equals(strategyKey)) {
            return I18n.f("group.strategy.floorType.pattern", resource.getFloor(), resource.getType());
        }
        return null;
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

