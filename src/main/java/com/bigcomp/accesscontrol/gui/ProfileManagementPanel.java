// Group 2 ChenGong ZhangZhao LiangYizhuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.profile.GroupManager;
import com.bigcomp.accesscontrol.profile.ResourceGroup;
import com.bigcomp.accesscontrol.profile.PriorityPolicy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private JTextArea profileDetailsArea;
    private JTextField limitPerUserPerDayField;
    private JTextField limitPerUserPerWeekField;
    private JTextField limitPerUserPerMonthField;
    private JTextField limitGlobalPerDayField;
    private JTextField limitGlobalPerWeekField;
    private JTextField limitGlobalPerMonthField;
    private JCheckBox limitPerUserPerDayPerResourceCheck;
    private JButton saveUsageLimitButton;
    private JButton clearUsageLimitButton;
    private JCheckBox priorityEnabledCheck;
    private JSpinner priorityMinutesSpinner;
    private DefaultListModel<String> priorityBuildingsModel;
    private JList<String> priorityBuildingsList;
    private JLabel priorityHintLabel;
    private JButton savePriorityButton;
    private JButton clearPriorityButton;
    private JTabbedPane bottomTabs;
    private TitledBorder usageLimitBorder;
    private TitledBorder priorityPolicyBorder;
    private TitledBorder profilesBorder;
    private TitledBorder rightsBorder;
    private TitledBorder timeFilterBorder;
    private TitledBorder profileBorder;
    private JLabel groupLabel;
    private JLabel profileNameLabel;
    private JButton newButton;
    private JButton modifyButton;
    private JButton deleteButton;
    private JButton restoreButton;
    private JButton refreshGroupsButton;
    private JButton addRightButton;
    private JButton removeRightButton;
    private JButton editTimeFilterButton;
    private JButton saveButton;
    
    public ProfileManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        initializeComponents();
        setupLayout();
        applyLanguage();
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
        accessRightsModel = new DefaultTableModel() {
             @Override
             public boolean isCellEditable(int row, int column) {
                 return false;
             }
         };
        accessRightsModel.setColumnIdentifiers(new String[]{I18n.t("profile.col.group"), I18n.t("profile.col.timeFilter"), I18n.t("profile.col.usageLimit")});
        accessRightsTable = new JTable(accessRightsModel);
        accessRightsTable.setAutoCreateRowSorter(true);
        accessRightsTable.setFillsViewportHeight(true);
        styleTable(accessRightsTable);
        accessRightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accessRightsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedAccessRightDetails();
            }
        });
        
        // Input fields
        profileNameField = new JTextField(20);
        groupCombo = new JComboBox<>();
        profileInfoArea = new JTextArea(5, 30);
        profileInfoArea.setEditable(false);
        profileInfoArea.setLineWrap(true);
        profileInfoArea.setWrapStyleWord(true);

        profileDetailsArea = new JTextArea();
        profileDetailsArea.setEditable(false);
        profileDetailsArea.setLineWrap(true);
        profileDetailsArea.setWrapStyleWord(true);
        profileDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        limitPerUserPerDayField = new JTextField(8);
        limitPerUserPerWeekField = new JTextField(8);
        limitPerUserPerMonthField = new JTextField(8);
        limitGlobalPerDayField = new JTextField(8);
        limitGlobalPerWeekField = new JTextField(8);
        limitGlobalPerMonthField = new JTextField(8);
        limitPerUserPerDayPerResourceCheck = new JCheckBox();
        saveUsageLimitButton = new JButton();
        saveUsageLimitButton.addActionListener(e -> saveSelectedGroupUsageLimit());
        clearUsageLimitButton = new JButton();
        clearUsageLimitButton.addActionListener(e -> clearSelectedGroupUsageLimit());
        setUsageLimitEditorEnabled(false);

        priorityEnabledCheck = new JCheckBox();
        priorityMinutesSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 1440, 1));
        priorityBuildingsModel = new DefaultListModel<>();
        priorityBuildingsList = new JList<>(priorityBuildingsModel);
        priorityBuildingsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        priorityHintLabel = new JLabel();
        savePriorityButton = new JButton();
        savePriorityButton.addActionListener(e -> savePriorityPolicy());
        clearPriorityButton = new JButton();
        clearPriorityButton.addActionListener(e -> clearPriorityPolicy());
        setPriorityPolicyEditorEnabled(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        
        // Left: Profile list and operations
        JPanel leftPanel = new JPanel(new BorderLayout());
        profilesBorder = new TitledBorder("");
        leftPanel.setBorder(profilesBorder);
        leftPanel.add(new JScrollPane(profileList), BorderLayout.CENTER);
        
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        newButton = new JButton();
        newButton.addActionListener(e -> createNewProfile());
        modifyButton = new JButton();
        modifyButton.addActionListener(e -> modifyProfile());
        deleteButton = new JButton();
        deleteButton.setForeground(Color.RED);
        deleteButton.addActionListener(e -> deleteProfile());
        restoreButton = new JButton();
        restoreButton.addActionListener(e -> restoreProfile());
        leftButtonPanel.add(newButton);
        leftButtonPanel.add(modifyButton);
        leftButtonPanel.add(deleteButton);
        leftButtonPanel.add(restoreButton);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // Center: Access rights management
        JPanel centerPanel = new JPanel(new BorderLayout());
        rightsBorder = new TitledBorder("");
        centerPanel.setBorder(rightsBorder);
        
        // Top: Add access rights
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        addPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        groupLabel = new JLabel();
        addPanel.add(groupLabel);
        addPanel.add(groupCombo);
        refreshGroupsButton = new JButton();
        refreshGroupsButton.addActionListener(e -> {
                loadGroups();
                JOptionPane.showMessageDialog(ProfileManagementPanel.this, 
                    I18n.t("profile.msg.refreshed"), I18n.t("common.info"), JOptionPane.INFORMATION_MESSAGE);
        });
        addRightButton = new JButton();
        addRightButton.addActionListener(e -> addAccessRight());
        removeRightButton = new JButton();
        removeRightButton.addActionListener(e -> removeAccessRight());
        addPanel.add(refreshGroupsButton);
        addPanel.add(addRightButton);
        addPanel.add(removeRightButton);
        centerPanel.add(addPanel, BorderLayout.NORTH);
        
        // Center: Access rights table
        JScrollPane rightsScroll = new JScrollPane(accessRightsTable);
        rightsScroll.setBorder(new EmptyBorder(0, 8, 8, 8));
        centerPanel.add(rightsScroll, BorderLayout.CENTER);
        
        // Bottom: Time filter editing
        JPanel timeFilterPanel = new JPanel(new BorderLayout());
        timeFilterBorder = new TitledBorder("");
        timeFilterPanel.setBorder(timeFilterBorder);
        JScrollPane timeFilterScroll = new JScrollPane(profileInfoArea);
        timeFilterScroll.setBorder(new EmptyBorder(8, 8, 8, 8));
        timeFilterPanel.add(timeFilterScroll, BorderLayout.CENTER);
        JPanel timeFilterButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        timeFilterButtonPanel.setBorder(new EmptyBorder(0, 8, 8, 8));
        editTimeFilterButton = new JButton();
        editTimeFilterButton.addActionListener(e -> editTimeFilter());
        timeFilterButtonPanel.add(editTimeFilterButton);
        timeFilterPanel.add(timeFilterButtonPanel, BorderLayout.SOUTH);

        JPanel usageLimitPanel = new JPanel(new BorderLayout());
        usageLimitBorder = new TitledBorder("");
        usageLimitPanel.setBorder(usageLimitBorder);
        JPanel limitForm = new JPanel(new GridBagLayout());
        GridBagConstraints lg = new GridBagConstraints();
        lg.insets = new Insets(4, 8, 4, 8);
        lg.anchor = GridBagConstraints.WEST;
        lg.fill = GridBagConstraints.HORIZONTAL;
        lg.weightx = 1;

        int row = 0;
        lg.gridx = 0; lg.gridy = row; lg.weightx = 0;
        limitForm.add(new JLabel(I18n.t("profile.limit.perUserPerDay")), lg);
        lg.gridx = 1; lg.weightx = 1;
        limitForm.add(limitPerUserPerDayField, lg);
        lg.gridx = 2; lg.weightx = 0;
        limitForm.add(new JLabel(I18n.t("profile.limit.globalPerDay")), lg);
        lg.gridx = 3; lg.weightx = 1;
        limitForm.add(limitGlobalPerDayField, lg);

        row++;
        lg.gridx = 0; lg.gridy = row; lg.weightx = 0;
        limitForm.add(new JLabel(I18n.t("profile.limit.perUserPerWeek")), lg);
        lg.gridx = 1; lg.weightx = 1;
        limitForm.add(limitPerUserPerWeekField, lg);
        lg.gridx = 2; lg.weightx = 0;
        limitForm.add(new JLabel(I18n.t("profile.limit.globalPerWeek")), lg);
        lg.gridx = 3; lg.weightx = 1;
        limitForm.add(limitGlobalPerWeekField, lg);

        row++;
        lg.gridx = 0; lg.gridy = row; lg.weightx = 0;
        limitForm.add(new JLabel(I18n.t("profile.limit.perUserPerMonth")), lg);
        lg.gridx = 1; lg.weightx = 1;
        limitForm.add(limitPerUserPerMonthField, lg);
        lg.gridx = 2; lg.weightx = 0;
        limitForm.add(new JLabel(I18n.t("profile.limit.globalPerMonth")), lg);
        lg.gridx = 3; lg.weightx = 1;
        limitForm.add(limitGlobalPerMonthField, lg);

        row++;
        lg.gridx = 0; lg.gridy = row; lg.weightx = 0;
        limitForm.add(new JLabel(I18n.t("profile.limit.perUserPerDayPerResource")), lg);
        lg.gridx = 1; lg.weightx = 1; lg.gridwidth = 3;
        limitForm.add(limitPerUserPerDayPerResourceCheck, lg);
        lg.gridwidth = 1;

        JPanel limitButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        limitButtons.setBorder(new EmptyBorder(4, 8, 8, 8));
        limitButtons.add(saveUsageLimitButton);
        limitButtons.add(clearUsageLimitButton);

        usageLimitPanel.add(limitForm, BorderLayout.CENTER);
        usageLimitPanel.add(limitButtons, BorderLayout.SOUTH);

        JPanel priorityPanel = new JPanel(new BorderLayout());
        priorityPolicyBorder = new TitledBorder("");
        priorityPanel.setBorder(priorityPolicyBorder);

        JPanel priorityForm = new JPanel(new GridBagLayout());
        GridBagConstraints pg = new GridBagConstraints();
        pg.insets = new Insets(4, 8, 4, 8);
        pg.anchor = GridBagConstraints.WEST;
        pg.fill = GridBagConstraints.HORIZONTAL;
        pg.weightx = 1;

        int prow = 0;
        pg.gridx = 0; pg.gridy = prow; pg.weightx = 0;
        priorityForm.add(new JLabel(I18n.t("profile.priority.enabled")), pg);
        pg.gridx = 1; pg.weightx = 1; pg.gridwidth = 3;
        priorityForm.add(priorityEnabledCheck, pg);
        pg.gridwidth = 1;

        prow++;
        pg.gridx = 0; pg.gridy = prow; pg.weightx = 0;
        priorityForm.add(new JLabel(I18n.t("profile.priority.minutes")), pg);
        pg.gridx = 1; pg.weightx = 1;
        priorityForm.add(priorityMinutesSpinner, pg);

        prow++;
        pg.gridx = 0; pg.gridy = prow; pg.weightx = 0;
        priorityForm.add(new JLabel(I18n.t("profile.priority.buildings")), pg);
        pg.gridx = 1; pg.weightx = 1; pg.gridwidth = 3;
        JScrollPane buildingScroll = new JScrollPane(priorityBuildingsList);
        buildingScroll.setPreferredSize(new Dimension(260, 120));
        priorityForm.add(buildingScroll, pg);
        pg.gridwidth = 1;

        prow++;
        pg.gridx = 0; pg.gridy = prow; pg.weightx = 1; pg.gridwidth = 4;
        priorityHintLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
        priorityForm.add(priorityHintLabel, pg);
        pg.gridwidth = 1;

        JPanel priorityButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        priorityButtons.setBorder(new EmptyBorder(4, 8, 8, 8));
        priorityButtons.add(savePriorityButton);
        priorityButtons.add(clearPriorityButton);

        priorityPanel.add(priorityForm, BorderLayout.CENTER);
        priorityPanel.add(priorityButtons, BorderLayout.SOUTH);

        bottomTabs = new JTabbedPane();
        bottomTabs.addTab(I18n.t("profile.title.timeFilter"), timeFilterPanel);
        bottomTabs.addTab(I18n.t("profile.title.usageLimits"), usageLimitPanel);
        bottomTabs.addTab(I18n.t("profile.title.priorityPolicy"), priorityPanel);
        centerPanel.add(bottomTabs, BorderLayout.SOUTH);
        
        // Right: Profile information
        JPanel rightPanel = new JPanel(new BorderLayout());
        profileBorder = new TitledBorder("");
        rightPanel.setBorder(profileBorder);
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        profileNameLabel = new JLabel();
        infoPanel.add(profileNameLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        infoPanel.add(profileNameField, gbc);
        
        rightPanel.add(infoPanel, BorderLayout.NORTH);
        JScrollPane detailsScroll = new JScrollPane(profileDetailsArea);
        detailsScroll.setBorder(new EmptyBorder(8, 8, 8, 8));
        rightPanel.add(detailsScroll, BorderLayout.CENTER);
        saveButton = new JButton();
        saveButton.addActionListener(e -> saveProfile());
        JPanel savePanel = new JPanel(new BorderLayout());
        savePanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        savePanel.add(saveButton, BorderLayout.NORTH);
        rightPanel.add(savePanel, BorderLayout.SOUTH);
        
        // Main layout
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        leftSplit.setResizeWeight(0.3);
        leftSplit.setDividerSize(8);
        leftSplit.setContinuousLayout(true);
        leftSplit.setBorder(null);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setResizeWeight(0.5);
        mainSplit.setDividerSize(8);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        
        add(mainSplit, BorderLayout.CENTER);
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
            groupCombo.addItem(I18n.t("profile.msg.noGroups"));
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
                com.bigcomp.accesscontrol.profile.UsageLimit limit = profile.getUsageLimitsByGroup() != null ? profile.getUsageLimitsByGroup().get(groupName) : null;
                String limitDesc = describeUsageLimit(limit);
                accessRightsModel.addRow(new Object[]{groupName, filterDesc, limitDesc});
            }
            profileDetailsArea.setText(formatProfileDetails(profile));
            profileDetailsArea.setCaretPosition(0);
            loadPriorityPolicyEditor(profile);
            if (accessRightsModel.getRowCount() > 0) {
                accessRightsTable.setRowSelectionInterval(0, 0);
            } else {
                profileInfoArea.setText(I18n.t("profile.details.noRights"));
            }
        }
    }
    
    private String describeTimeFilter(TimeFilter filter) {
        List<String> parts = new ArrayList<>();
        
        if (filter.getDaysOfWeek() != null && !filter.getDaysOfWeek().isEmpty()) {
            parts.add(I18n.f("profile.msg.days", filter.getDaysOfWeek()));
        }
        if (filter.getTimeRanges() != null && !filter.getTimeRanges().isEmpty()) {
            parts.add(I18n.f("profile.msg.timeRanges", filter.getTimeRanges().size()));
        }
        
        return parts.isEmpty() ? I18n.t("profile.msg.noRestrictions") : String.join(", ", parts);
    }

    private String describeUsageLimit(com.bigcomp.accesscontrol.profile.UsageLimit limit) {
        if (limit == null) {
            return I18n.t("common.none");
        }
        List<String> parts = new ArrayList<>();
        if (limit.getPerUserPerDayMax() != null) parts.add(I18n.f("profile.limit.short.perUserPerDay", limit.getPerUserPerDayMax()));
        if (limit.getPerUserPerWeekMax() != null) parts.add(I18n.f("profile.limit.short.perUserPerWeek", limit.getPerUserPerWeekMax()));
        if (limit.getPerUserPerMonthMax() != null) parts.add(I18n.f("profile.limit.short.perUserPerMonth", limit.getPerUserPerMonthMax()));
        if (limit.getGlobalPerDayMax() != null) parts.add(I18n.f("profile.limit.short.globalPerDay", limit.getGlobalPerDayMax()));
        if (limit.getGlobalPerWeekMax() != null) parts.add(I18n.f("profile.limit.short.globalPerWeek", limit.getGlobalPerWeekMax()));
        if (limit.getGlobalPerMonthMax() != null) parts.add(I18n.f("profile.limit.short.globalPerMonth", limit.getGlobalPerMonthMax()));
        if (parts.isEmpty()) {
            return I18n.t("profile.msg.noRestrictions");
        }
        return String.join(", ", parts);
    }
    
    private void clearProfileInfo() {
        profileNameField.setText("");
        accessRightsModel.setRowCount(0);
        profileInfoArea.setText("");
        if (profileDetailsArea != null) {
            profileDetailsArea.setText("");
        }
        clearUsageLimitEditor();
        clearPriorityPolicyEditor();
    }

    private void showSelectedAccessRightDetails() {
        String selectedProfileName = profileList.getSelectedValue();
        if (selectedProfileName == null) {
            profileInfoArea.setText("");
            clearUsageLimitEditor();
            return;
        }
        Profile profile = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
        if (profile == null) {
            profileInfoArea.setText("");
            clearUsageLimitEditor();
            return;
        }
        int viewRow = accessRightsTable.getSelectedRow();
        if (viewRow < 0) {
            profileInfoArea.setText(I18n.t("profile.details.selectRightHint"));
            clearUsageLimitEditor();
            return;
        }
        int modelRow = accessRightsTable.convertRowIndexToModel(viewRow);
        Object groupKeyObj = accessRightsModel.getValueAt(modelRow, 0);
        if (groupKeyObj == null) {
            profileInfoArea.setText("");
            clearUsageLimitEditor();
            return;
        }
        String groupKey = groupKeyObj.toString();
        TimeFilter filter = profile.getAccessRights().get(groupKey);

        StringBuilder sb = new StringBuilder();
        sb.append(I18n.t("profile.details.group")).append(": ").append(renderGroupName(groupKey)).append('\n');
        ResourceGroup group = new GroupManager().getGroup(groupKey);
        if (group != null) {
            sb.append(I18n.t("profile.details.groupSecurity")).append(": ").append(group.getSecurityLevel()).append('\n');
            sb.append(I18n.t("profile.details.groupResourceCount")).append(": ").append(group.getResourceIds().size()).append('\n');
        }
        sb.append('\n');
        sb.append(I18n.t("profile.details.timeFilter")).append(":\n");
        sb.append(formatTimeFilterDetailed(filter));
        profileInfoArea.setText(sb.toString());
        profileInfoArea.setCaretPosition(0);
        loadUsageLimitEditor(profile, groupKey);
    }

    private void loadUsageLimitEditor(Profile profile, String groupKey) {
        if (profile == null || groupKey == null || groupKey.isBlank()) {
            clearUsageLimitEditor();
            return;
        }
        com.bigcomp.accesscontrol.profile.UsageLimit limit = profile.getUsageLimitsByGroup() != null ? profile.getUsageLimitsByGroup().get(groupKey) : null;
        limitPerUserPerDayField.setText(limit != null && limit.getPerUserPerDayMax() != null ? String.valueOf(limit.getPerUserPerDayMax()) : "");
        limitPerUserPerWeekField.setText(limit != null && limit.getPerUserPerWeekMax() != null ? String.valueOf(limit.getPerUserPerWeekMax()) : "");
        limitPerUserPerMonthField.setText(limit != null && limit.getPerUserPerMonthMax() != null ? String.valueOf(limit.getPerUserPerMonthMax()) : "");
        limitGlobalPerDayField.setText(limit != null && limit.getGlobalPerDayMax() != null ? String.valueOf(limit.getGlobalPerDayMax()) : "");
        limitGlobalPerWeekField.setText(limit != null && limit.getGlobalPerWeekMax() != null ? String.valueOf(limit.getGlobalPerWeekMax()) : "");
        limitGlobalPerMonthField.setText(limit != null && limit.getGlobalPerMonthMax() != null ? String.valueOf(limit.getGlobalPerMonthMax()) : "");
        limitPerUserPerDayPerResourceCheck.setSelected(limit != null && limit.isPerUserPerDayPerResource());
        setUsageLimitEditorEnabled(true);
    }

    private void clearUsageLimitEditor() {
        if (limitPerUserPerDayField == null) {
            return;
        }
        limitPerUserPerDayField.setText("");
        limitPerUserPerWeekField.setText("");
        limitPerUserPerMonthField.setText("");
        limitGlobalPerDayField.setText("");
        limitGlobalPerWeekField.setText("");
        limitGlobalPerMonthField.setText("");
        limitPerUserPerDayPerResourceCheck.setSelected(false);
        setUsageLimitEditorEnabled(false);
    }

    private void setUsageLimitEditorEnabled(boolean enabled) {
        if (limitPerUserPerDayField == null) {
            return;
        }
        limitPerUserPerDayField.setEnabled(enabled);
        limitPerUserPerWeekField.setEnabled(enabled);
        limitPerUserPerMonthField.setEnabled(enabled);
        limitGlobalPerDayField.setEnabled(enabled);
        limitGlobalPerWeekField.setEnabled(enabled);
        limitGlobalPerMonthField.setEnabled(enabled);
        limitPerUserPerDayPerResourceCheck.setEnabled(enabled);
        saveUsageLimitButton.setEnabled(enabled);
        clearUsageLimitButton.setEnabled(enabled);
    }

    private void saveSelectedGroupUsageLimit() {
        String selectedProfileName = profileList.getSelectedValue();
        int viewRow = accessRightsTable.getSelectedRow();
        if (selectedProfileName == null || viewRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectRightToEdit"), I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = accessRightsTable.convertRowIndexToModel(viewRow);
        Object groupKeyObj = accessRightsModel.getValueAt(modelRow, 0);
        if (groupKeyObj == null) {
            return;
        }
        String groupKey = groupKeyObj.toString();
        Profile profile = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
        if (profile == null) {
            return;
        }
        try {
            com.bigcomp.accesscontrol.profile.UsageLimit limit = new com.bigcomp.accesscontrol.profile.UsageLimit();
            limit.setPerUserPerDayMax(parseNullableNonNegativeInt(limitPerUserPerDayField.getText()));
            limit.setPerUserPerWeekMax(parseNullableNonNegativeInt(limitPerUserPerWeekField.getText()));
            limit.setPerUserPerMonthMax(parseNullableNonNegativeInt(limitPerUserPerMonthField.getText()));
            limit.setGlobalPerDayMax(parseNullableNonNegativeInt(limitGlobalPerDayField.getText()));
            limit.setGlobalPerWeekMax(parseNullableNonNegativeInt(limitGlobalPerWeekField.getText()));
            limit.setGlobalPerMonthMax(parseNullableNonNegativeInt(limitGlobalPerMonthField.getText()));
            limit.setPerUserPerDayPerResource(limitPerUserPerDayPerResourceCheck.isSelected());
            profile.getUsageLimitsByGroup().put(groupKey, limit);
            accessControlSystem.getProfileManager().saveProfile(profile);

            Profile refreshed = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
            if (refreshed != null) {
                profileDetailsArea.setText(formatProfileDetails(refreshed));
                profileDetailsArea.setCaretPosition(0);
            }
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.usageSaved"), I18n.t("common.success"), JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, I18n.f("profile.msg.usageInvalid", ex.getMessage()), I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("profile.msg.saveProfileFailed", e.getMessage()), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearSelectedGroupUsageLimit() {
        String selectedProfileName = profileList.getSelectedValue();
        int viewRow = accessRightsTable.getSelectedRow();
        if (selectedProfileName == null || viewRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectRightToEdit"), I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = accessRightsTable.convertRowIndexToModel(viewRow);
        Object groupKeyObj = accessRightsModel.getValueAt(modelRow, 0);
        if (groupKeyObj == null) {
            return;
        }
        String groupKey = groupKeyObj.toString();
        Profile profile = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
        if (profile == null) {
            return;
        }
        try {
            if (profile.getUsageLimitsByGroup() != null) {
                profile.getUsageLimitsByGroup().remove(groupKey);
            }
            accessControlSystem.getProfileManager().saveProfile(profile);

            Profile refreshed = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
            if (refreshed != null) {
                loadUsageLimitEditor(refreshed, groupKey);
                profileDetailsArea.setText(formatProfileDetails(refreshed));
                profileDetailsArea.setCaretPosition(0);
            } else {
                clearUsageLimitEditor();
            }
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.usageCleared"), I18n.t("common.success"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("profile.msg.saveProfileFailed", e.getMessage()), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private Integer parseNullableNonNegativeInt(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(raw);
        }
        if (v < 0) {
            throw new IllegalArgumentException(raw);
        }
        return v;
    }

    private void loadPriorityPolicyEditor(Profile profile) {
        if (profile == null) {
            clearPriorityPolicyEditor();
            return;
        }
        Set<String> buildings = collectBuildingsFromResources();
        priorityBuildingsModel.clear();
        List<String> ordered = new ArrayList<>(buildings);
        ordered.sort(String::compareToIgnoreCase);
        for (String b : ordered) {
            priorityBuildingsModel.addElement(b);
        }

        PriorityPolicy policy = profile.getPriorityPolicy();
        if (policy == null) {
            priorityEnabledCheck.setSelected(false);
            priorityMinutesSpinner.setValue(60);
            priorityBuildingsList.clearSelection();
            setPriorityPolicyEditorEnabled(true);
            return;
        }
        priorityEnabledCheck.setSelected(policy.isRequireGateWithinMinutesEnabled());
        priorityMinutesSpinner.setValue(policy.getRequireGateWithinMinutes());
        priorityBuildingsList.clearSelection();
        if (policy.getBuildings() != null && !policy.getBuildings().isEmpty()) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < priorityBuildingsModel.size(); i++) {
                String v = priorityBuildingsModel.getElementAt(i);
                if (policy.getBuildings().contains(v)) {
                    indices.add(i);
                }
            }
            int[] sel = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                sel[i] = indices.get(i);
            }
            priorityBuildingsList.setSelectedIndices(sel);
        }
        setPriorityPolicyEditorEnabled(true);
    }

    private void clearPriorityPolicyEditor() {
        if (priorityEnabledCheck == null) {
            return;
        }
        priorityEnabledCheck.setSelected(false);
        priorityMinutesSpinner.setValue(60);
        priorityBuildingsModel.clear();
        priorityBuildingsList.clearSelection();
        setPriorityPolicyEditorEnabled(false);
    }

    private void setPriorityPolicyEditorEnabled(boolean enabled) {
        if (priorityEnabledCheck == null) {
            return;
        }
        priorityEnabledCheck.setEnabled(enabled);
        priorityMinutesSpinner.setEnabled(enabled);
        priorityBuildingsList.setEnabled(enabled);
        savePriorityButton.setEnabled(enabled);
        clearPriorityButton.setEnabled(enabled);
        if (priorityHintLabel != null) {
            priorityHintLabel.setEnabled(enabled);
        }
    }

    private void savePriorityPolicy() {
        String selectedProfileName = profileList.getSelectedValue();
        if (selectedProfileName == null) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectProfileFirst"), I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        Profile profile = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
        if (profile == null) {
            return;
        }
        PriorityPolicy policy = profile.getPriorityPolicy();
        if (policy == null) {
            policy = new PriorityPolicy();
            profile.setPriorityPolicy(policy);
        }
        policy.setRequireGateWithinMinutesEnabled(priorityEnabledCheck.isSelected());
        policy.setRequireGateWithinMinutes((Integer) priorityMinutesSpinner.getValue());

        List<String> selectedBuildings = priorityBuildingsList.getSelectedValuesList();
        if (selectedBuildings == null || selectedBuildings.isEmpty()) {
            policy.setBuildings(null);
        } else {
            policy.setBuildings(new ArrayList<>(selectedBuildings));
        }
        try {
            accessControlSystem.getProfileManager().saveProfile(profile);
            Profile refreshed = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
            if (refreshed != null) {
                profileDetailsArea.setText(formatProfileDetails(refreshed));
                profileDetailsArea.setCaretPosition(0);
            }
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.prioritySaved"), I18n.t("common.success"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("profile.msg.saveProfileFailed", e.getMessage()), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearPriorityPolicy() {
        String selectedProfileName = profileList.getSelectedValue();
        if (selectedProfileName == null) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectProfileFirst"), I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        Profile profile = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
        if (profile == null) {
            return;
        }
        try {
            profile.setPriorityPolicy(null);
            accessControlSystem.getProfileManager().saveProfile(profile);
            Profile refreshed = accessControlSystem.getProfileManager().getProfile(selectedProfileName);
            if (refreshed != null) {
                loadPriorityPolicyEditor(refreshed);
                profileDetailsArea.setText(formatProfileDetails(refreshed));
                profileDetailsArea.setCaretPosition(0);
            } else {
                clearPriorityPolicyEditor();
            }
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.priorityCleared"), I18n.t("common.success"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("profile.msg.saveProfileFailed", e.getMessage()), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private Set<String> collectBuildingsFromResources() {
        Set<String> buildings = new HashSet<>();
        Map<String, Resource> resources = accessControlSystem.getDatabaseManager().loadAllResources();
        for (Resource r : resources.values()) {
            if (r != null && r.getBuilding() != null && !r.getBuilding().isBlank()) {
                buildings.add(r.getBuilding());
            }
        }
        return buildings;
    }

    private String formatProfileDetails(Profile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.t("profile.details.title")).append('\n');
        sb.append(I18n.t("profile.details.name")).append(": ").append(profile.getName()).append('\n');
        sb.append('\n');

        Map<String, TimeFilter> rights = profile.getAccessRights();
        sb.append(I18n.f("profile.details.rightCount", rights != null ? rights.size() : 0)).append('\n');
        if (rights == null || rights.isEmpty()) {
            sb.append(I18n.t("profile.details.noRights")).append('\n');
        } else {
            for (Map.Entry<String, TimeFilter> e : rights.entrySet()) {
                sb.append("- ").append(renderGroupName(e.getKey())).append(": ").append(describeTimeFilter(e.getValue())).append('\n');
            }
        }
        sb.append('\n');

        Map<String, com.bigcomp.accesscontrol.profile.UsageLimit> limitsByGroup = profile.getUsageLimitsByGroup();
        sb.append(I18n.t("profile.details.usageByGroup")).append(": ").append(limitsByGroup != null ? limitsByGroup.size() : 0).append('\n');
        if (limitsByGroup != null && !limitsByGroup.isEmpty()) {
            for (Map.Entry<String, com.bigcomp.accesscontrol.profile.UsageLimit> e : limitsByGroup.entrySet()) {
                sb.append("- ").append(renderGroupName(e.getKey())).append(": ").append(formatUsageLimit(e.getValue())).append('\n');
            }
        }
        sb.append('\n');

        Map<String, com.bigcomp.accesscontrol.profile.UsageLimit> limitsByType = profile.getUsageLimitsByResourceType();
        sb.append(I18n.t("profile.details.usageByType")).append(": ").append(limitsByType != null ? limitsByType.size() : 0).append('\n');
        if (limitsByType != null && !limitsByType.isEmpty()) {
            for (Map.Entry<String, com.bigcomp.accesscontrol.profile.UsageLimit> e : limitsByType.entrySet()) {
                String typeLabel = I18n.t("resource.type." + e.getKey());
                if (typeLabel.equals("resource.type." + e.getKey())) {
                    typeLabel = e.getKey();
                }
                sb.append("- ").append(typeLabel).append(": ").append(formatUsageLimit(e.getValue())).append('\n');
            }
        }
        sb.append('\n');

        com.bigcomp.accesscontrol.profile.PriorityPolicy policy = profile.getPriorityPolicy();
        if (policy == null) {
            sb.append(I18n.t("profile.details.priorityPolicy")).append(": ").append(I18n.t("common.none")).append('\n');
        } else {
            sb.append(I18n.t("profile.details.priorityPolicy")).append('\n');
            sb.append(I18n.t("profile.details.priorityEnabled")).append(": ").append(policy.isRequireGateWithinMinutesEnabled()).append('\n');
            sb.append(I18n.t("profile.details.priorityMinutes")).append(": ").append(policy.getRequireGateWithinMinutes()).append('\n');
            sb.append(I18n.t("profile.details.priorityBuildings")).append(": ");
            if (policy.getBuildings() == null || policy.getBuildings().isEmpty()) {
                sb.append(I18n.t("common.none"));
            } else {
                sb.append(String.join(", ", policy.getBuildings()));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String renderGroupName(String groupKey) {
        String translated = I18n.t(groupKey);
        if (translated != null && !translated.equals(groupKey)) {
            return translated + " (" + groupKey + ")";
        }
        return groupKey;
    }

    private String formatTimeFilterDetailed(TimeFilter filter) {
        if (filter == null) {
            return I18n.t("profile.details.noTimeFilter") + "\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.t("profile.details.years")).append(": ").append(formatSet(filter.getYears(), filter.isExcludeYears())).append('\n');
        sb.append(I18n.t("profile.details.months")).append(": ").append(formatSet(filter.getMonths(), filter.isExcludeMonths())).append('\n');
        sb.append(I18n.t("profile.details.daysOfMonth")).append(": ").append(formatSet(filter.getDaysOfMonth(), filter.isExcludeDaysOfMonth())).append('\n');
        sb.append(I18n.t("profile.details.daysOfWeek")).append(": ").append(formatSet(filter.getDaysOfWeek(), filter.isExcludeDaysOfWeek())).append('\n');
        sb.append(I18n.t("profile.details.timeRanges")).append(": ").append(formatTimeRanges(filter.getTimeRanges(), filter.isExcludeTimeRanges())).append('\n');
        return sb.toString();
    }

    private String formatTimeRanges(List<TimeFilter.TimeRange> ranges, boolean exclude) {
        if (ranges == null || ranges.isEmpty()) {
            return I18n.t("common.all");
        }
        List<String> parts = new ArrayList<>();
        for (TimeFilter.TimeRange r : ranges) {
            parts.add(formatMinutes(r.getStartMinutes()) + "-" + formatMinutes(r.getEndMinutes()));
        }
        String joined = String.join(", ", parts);
        return exclude ? (I18n.t("common.exclude") + " " + joined) : joined;
    }

    private String formatMinutes(int minutes) {
        int h = Math.max(0, minutes) / 60;
        int m = Math.max(0, minutes) % 60;
        return String.format("%02d:%02d", h, m);
    }

    private String formatSet(Object setObj, boolean exclude) {
        if (setObj == null) {
            return I18n.t("common.all");
        }
        String s = setObj.toString();
        if (s.isBlank() || "[]".equals(s)) {
            return I18n.t("common.all");
        }
        return exclude ? (I18n.t("common.exclude") + " " + s) : s;
    }

    private String formatUsageLimit(com.bigcomp.accesscontrol.profile.UsageLimit limit) {
        if (limit == null) {
            return I18n.t("common.none");
        }
        List<String> parts = new ArrayList<>();
        if (limit.getPerUserPerDayMax() != null) parts.add("perUserPerDay=" + limit.getPerUserPerDayMax());
        if (limit.getPerUserPerWeekMax() != null) parts.add("perUserPerWeek=" + limit.getPerUserPerWeekMax());
        if (limit.getPerUserPerMonthMax() != null) parts.add("perUserPerMonth=" + limit.getPerUserPerMonthMax());
        if (limit.getGlobalPerDayMax() != null) parts.add("globalPerDay=" + limit.getGlobalPerDayMax());
        if (limit.getGlobalPerWeekMax() != null) parts.add("globalPerWeek=" + limit.getGlobalPerWeekMax());
        if (limit.getGlobalPerMonthMax() != null) parts.add("globalPerMonth=" + limit.getGlobalPerMonthMax());
        parts.add("perUserPerDayPerResource=" + limit.isPerUserPerDayPerResource());
        return String.join(", ", parts);
    }
    
    private void createNewProfile() {
        String name = JOptionPane.showInputDialog(this, I18n.t("profile.msg.enterName"), I18n.t("profile.action.new"),
            JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Profile profile = new Profile(name.trim());
            ProfileManager profileManager = accessControlSystem.getProfileManager();
            try {
                profileManager.saveProfile(profile);
                loadProfiles();
                profileList.setSelectedValue(name.trim(), true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("profile.msg.createFailed", e.getMessage()),
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectDelete"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            I18n.f("profile.msg.confirmDelete", selected), I18n.t("common.confirmDelete.title"), 
            JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ProfileManager profileManager = accessControlSystem.getProfileManager();
            try {
                profileManager.deleteProfile(selected);
                loadProfiles();
                clearProfileInfo();
                JOptionPane.showMessageDialog(this, I18n.t("profile.msg.deleted"), I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("profile.msg.deleteFailed", e.getMessage()),
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void addAccessRight() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectProfileFirst"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String groupName = (String) groupCombo.getSelectedItem();
        if (groupName == null || groupName.startsWith("(") || groupName.contains(I18n.t("profile.msg.noGroups"))) {
            JOptionPane.showMessageDialog(this, 
                I18n.t("profile.msg.noGroupsHint"), 
                I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if access right for this resource group already exists
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        Profile profile = profileManager.getProfile(selected);
        if (profile != null) {
            if (profile.getAccessRights().containsKey(groupName)) {
                int confirm = JOptionPane.showConfirmDialog(this, 
                    I18n.t("profile.msg.overwriteConfirm"), 
                    I18n.t("common.confirmDelete.title"), 
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
                JOptionPane.showMessageDialog(this, I18n.t("profile.msg.rightAdded"), I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("profile.msg.saveProfileFailed", e.getMessage()), 
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void removeAccessRight() {
        int selectedRow = accessRightsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectRightToDelete"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectProfileFirst"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String groupName = (String) accessRightsTable.getValueAt(selectedRow, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            I18n.f("profile.msg.confirmDeleteRight", groupName), 
            I18n.t("common.confirmDelete.title"), 
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
                    JOptionPane.showMessageDialog(this, I18n.t("profile.msg.rightDeleted"), I18n.t("common.success"), 
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, I18n.f("profile.msg.saveProfileFailed", e.getMessage()), 
                        I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void editTimeFilter() {
        int selectedRow = accessRightsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectRightToEdit"), I18n.t("common.warning"), 
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
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectProfileToModify"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        Profile profile = profileManager.getProfile(selected);
        if (profile == null) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.notExist"), I18n.t("common.error"), 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create backup first
        try {
            backupProfile(selected);
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this, I18n.f("profile.msg.backupFailed", e.getMessage()), 
                I18n.t("common.warning"), JOptionPane.WARNING_MESSAGE);
        }
        
        String newName = (String) JOptionPane.showInputDialog(this, 
            I18n.t("profile.msg.enterNewName"), 
            I18n.t("profile.action.modify"), 
            JOptionPane.QUESTION_MESSAGE, 
            null, null, selected);
            
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(selected)) {
            newName = newName.trim();
            try {
                // Rename means delete old and save new
                profileManager.deleteProfile(selected);
                profile.setName(newName);
                profileManager.saveProfile(profile);
                
                loadProfiles();
                profileList.setSelectedValue(newName, true);
                JOptionPane.showMessageDialog(this, I18n.t("profile.msg.modifySuccess"), I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("profile.msg.modifyFailed", e.getMessage()), 
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void restoreProfile() {
        String selected = profileList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectProfileToRestore"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Find backup files
        java.io.File backupDir = new java.io.File("data/profiles/backup");
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.backupDirNotFound"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Find backups for this profile
        java.io.File[] backupFiles = backupDir.listFiles((dir, name) -> 
            name.startsWith(selected + "_") && name.endsWith(".json"));
        
        if (backupFiles == null || backupFiles.length == 0) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.noBackupsFound"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Display backup list for user selection
        String[] backupNames = new String[backupFiles.length];
        for (int i = 0; i < backupFiles.length; i++) {
            String name = backupFiles[i].getName();
            // Extract timestamp
            String timestamp = name.substring(selected.length() + 1, name.length() - 5);
            backupNames[i] = I18n.f("profile.msg.backupTime", timestamp);
        }
        
        String selectedBackup = (String) JOptionPane.showInputDialog(this,
            I18n.t("profile.msg.selectBackup"),
            I18n.t("profile.title.restore"),
            JOptionPane.QUESTION_MESSAGE,
            null,
            backupNames,
            backupNames[0]);
        
        if (selectedBackup != null) {
            int index = java.util.Arrays.asList(backupNames).indexOf(selectedBackup);
            java.io.File backupFile = backupFiles[index];
            
            int confirm = JOptionPane.showConfirmDialog(this, 
                I18n.t("profile.msg.confirmRestore"), 
                I18n.t("profile.msg.confirmRestoreTitle"), 
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
                        JOptionPane.showMessageDialog(this, I18n.t("profile.msg.restoreSuccess"), I18n.t("common.success"), 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, I18n.t("profile.msg.restoreFailedRead"), I18n.t("common.error"), 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, I18n.f("profile.msg.restoreFailed", e.getMessage()), 
                        I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.selectProfileToSave"), I18n.t("common.warning"), 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String newName = profileNameField.getText().trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("profile.msg.nameEmpty"), I18n.t("common.error"), 
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
                JOptionPane.showMessageDialog(this, I18n.t("profile.msg.saveSuccess"), I18n.t("common.success"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, I18n.f("profile.msg.saveFailed", e.getMessage()), 
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Time filter editing dialog (full version)
     */
    private static class TimeFilterDialog extends JDialog {
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
            super(parent, I18n.t("profile.title.editTime"), true);
            this.originalFilter = filter;
            this.timeFilter = new TimeFilter();
            if (filter != null) {
                copyFilter(filter, this.timeFilter);
            }
            initializeComponents();
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

        private void initializeComponents() {
            setSize(600, 500);
            setLayout(new BorderLayout());
            
            JPanel mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            
            // Days of week
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridwidth = 8;
            mainPanel.add(new JLabel(I18n.t("profile.field.days")), gbc);
            
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            String[] dayNames = {
                I18n.t("profile.day.mon"), I18n.t("profile.day.tue"), 
                I18n.t("profile.day.wed"), I18n.t("profile.day.thu"), 
                I18n.t("profile.day.fri"), I18n.t("profile.day.sat"), 
                I18n.t("profile.day.sun")
            };
            dayOfWeekCheckboxes = new JCheckBox[7];
            for (int i = 0; i < 7; i++) {
                gbc.gridx = i;
                dayOfWeekCheckboxes[i] = new JCheckBox(dayNames[i]);
                mainPanel.add(dayOfWeekCheckboxes[i], gbc);
            }
            
            // Load existing days
            if (timeFilter.getDaysOfWeek() != null) {
                java.util.Set<java.time.DayOfWeek> days = timeFilter.getDaysOfWeek();
                dayOfWeekCheckboxes[0].setSelected(days.contains(java.time.DayOfWeek.MONDAY));
                dayOfWeekCheckboxes[1].setSelected(days.contains(java.time.DayOfWeek.TUESDAY));
                dayOfWeekCheckboxes[2].setSelected(days.contains(java.time.DayOfWeek.WEDNESDAY));
                dayOfWeekCheckboxes[3].setSelected(days.contains(java.time.DayOfWeek.THURSDAY));
                dayOfWeekCheckboxes[4].setSelected(days.contains(java.time.DayOfWeek.FRIDAY));
                dayOfWeekCheckboxes[5].setSelected(days.contains(java.time.DayOfWeek.SATURDAY));
                dayOfWeekCheckboxes[6].setSelected(days.contains(java.time.DayOfWeek.SUNDAY));
            }
            
            // Exclude days option
            gbc.gridx = 0; gbc.gridy = 2;
            gbc.gridwidth = 8;
            excludeDaysCheckbox = new JCheckBox(I18n.t("profile.field.excludeDays"));
            excludeDaysCheckbox.setSelected(timeFilter.isExcludeDaysOfWeek());
            mainPanel.add(excludeDaysCheckbox, gbc);
            
            // Time range
            gbc.gridx = 0; gbc.gridy = 3;
            gbc.gridwidth = 1;
            mainPanel.add(new JLabel(I18n.t("profile.field.timeRange")), gbc);
            
            gbc.gridx = 1;
            startHourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
            mainPanel.add(startHourSpinner, gbc);
            
            gbc.gridx = 2;
            mainPanel.add(new JLabel(I18n.t("common.colon")), gbc);
            
            gbc.gridx = 3;
            startMinuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
            mainPanel.add(startMinuteSpinner, gbc);
            
            gbc.gridx = 4;
            mainPanel.add(new JLabel(I18n.t("profile.text.to")), gbc);
            
            gbc.gridx = 5;
            endHourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
            mainPanel.add(endHourSpinner, gbc);
            
            gbc.gridx = 6;
            mainPanel.add(new JLabel(I18n.t("common.colon")), gbc);
            
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
            excludeTimeRangeCheckbox = new JCheckBox(I18n.t("profile.field.excludeTime"));
            excludeTimeRangeCheckbox.setSelected(timeFilter.isExcludeTimeRanges());
            mainPanel.add(excludeTimeRangeCheckbox, gbc);
            
            // Help text
            gbc.gridx = 0; gbc.gridy = 5;
            gbc.gridwidth = 8;
            gbc.fill = GridBagConstraints.BOTH;
            JTextArea infoArea = new JTextArea(3, 40);
            infoArea.setEditable(false);
            infoArea.setText(I18n.t("profile.text.timeHelp"));
            infoArea.setBackground(getBackground());
            mainPanel.add(infoArea, gbc);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton okButton = new JButton(I18n.t("common.ok"));
            okButton.addActionListener(e -> {
                applyChanges();
                confirmed = true;
                dispose();
            });
            buttonPanel.add(okButton);
            
            JButton cancelButton = new JButton(I18n.t("common.cancel"));
            cancelButton.addActionListener(e -> dispose());
            buttonPanel.add(cancelButton);
            
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

    public void applyLanguage() {
        String selectedProfileName = profileList.getSelectedValue();
        int selectedRightViewRow = accessRightsTable.getSelectedRow();
        String selectedGroupKey = null;
        if (selectedRightViewRow >= 0) {
            int modelRow = accessRightsTable.convertRowIndexToModel(selectedRightViewRow);
            if (modelRow >= 0 && modelRow < accessRightsModel.getRowCount()) {
                Object groupKeyObj = accessRightsModel.getValueAt(modelRow, 0);
                if (groupKeyObj != null) {
                    selectedGroupKey = groupKeyObj.toString();
                }
            }
        }

        profilesBorder.setTitle(I18n.t("profile.title.list"));
        rightsBorder.setTitle(I18n.t("profile.title.rights"));
        timeFilterBorder.setTitle(I18n.t("profile.title.timeFilter"));
        profileBorder.setTitle(I18n.t("profile.title.profile"));
        if (usageLimitBorder != null) {
            usageLimitBorder.setTitle(I18n.t("profile.title.usageLimits"));
        }
        if (priorityPolicyBorder != null) {
            priorityPolicyBorder.setTitle(I18n.t("profile.title.priorityPolicy"));
        }

        newButton.setText(I18n.t("profile.action.new"));
        modifyButton.setText(I18n.t("profile.action.modify"));
        deleteButton.setText(I18n.t("profile.action.delete"));
        restoreButton.setText(I18n.t("profile.action.restore"));

        groupLabel.setText(I18n.t("profile.field.group"));
        refreshGroupsButton.setText(I18n.t("profile.action.refreshGroups"));
        addRightButton.setText(I18n.t("profile.action.addRight"));
        removeRightButton.setText(I18n.t("profile.action.removeRight"));
        editTimeFilterButton.setText(I18n.t("profile.action.editTimeFilter"));

        profileNameLabel.setText(I18n.t("profile.field.name"));
        saveButton.setText(I18n.t("profile.action.save"));
        if (saveUsageLimitButton != null) {
            saveUsageLimitButton.setText(I18n.t("profile.action.saveUsageLimit"));
        }
        if (clearUsageLimitButton != null) {
            clearUsageLimitButton.setText(I18n.t("profile.action.clearUsageLimit"));
        }
        if (limitPerUserPerDayPerResourceCheck != null) {
            limitPerUserPerDayPerResourceCheck.setText(I18n.t("profile.limit.perUserPerDayPerResourceHint"));
        }
        if (priorityEnabledCheck != null) {
            priorityEnabledCheck.setText(I18n.t("profile.priority.enabledHint"));
        }
        if (priorityHintLabel != null) {
            priorityHintLabel.setText(I18n.t("profile.priority.hint"));
        }
        if (savePriorityButton != null) {
            savePriorityButton.setText(I18n.t("profile.action.savePriorityPolicy"));
        }
        if (clearPriorityButton != null) {
            clearPriorityButton.setText(I18n.t("profile.action.clearPriorityPolicy"));
        }

        accessRightsModel.setColumnIdentifiers(new String[]{I18n.t("profile.col.group"), I18n.t("profile.col.timeFilter"), I18n.t("profile.col.usageLimit")});
        accessRightsTable.getTableHeader().repaint();

        if (!groupCombo.isEnabled()) {
            groupCombo.removeAllItems();
            groupCombo.addItem(I18n.t("profile.msg.noGroups"));
        }
        if (bottomTabs != null) {
            bottomTabs.setTitleAt(0, I18n.t("profile.title.timeFilter"));
            bottomTabs.setTitleAt(1, I18n.t("profile.title.usageLimits"));
            bottomTabs.setTitleAt(2, I18n.t("profile.title.priorityPolicy"));
        }

        String selectedGroupKeyFinal = selectedGroupKey;
        SwingUtilities.invokeLater(() -> {
            if (selectedProfileName != null) {
                loadSelectedProfile();
                if (selectedGroupKeyFinal != null) {
                    for (int row = 0; row < accessRightsModel.getRowCount(); row++) {
                        Object groupKeyObj = accessRightsModel.getValueAt(row, 0);
                        if (selectedGroupKeyFinal.equals(groupKeyObj)) {
                            int viewRow = accessRightsTable.convertRowIndexToView(row);
                            if (viewRow >= 0) {
                                accessRightsTable.setRowSelectionInterval(viewRow, viewRow);
                            }
                            break;
                        }
                    }
                }
                showSelectedAccessRightDetails();
            }
        });

        revalidate();
        repaint();
    }
}
