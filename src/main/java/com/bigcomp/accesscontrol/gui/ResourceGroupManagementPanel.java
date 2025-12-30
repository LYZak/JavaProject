package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.profile.ResourceGroup;
import com.bigcomp.accesscontrol.profile.GroupManager;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.Resource;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.sql.PreparedStatement;

/**
 * 资源组管理面板
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
        // 资源组列表
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedGroup();
                refreshAvailableResourceTable(); // 刷新可用资源列表
            }
        });
        
        // 资源表格
        String[] columnNames = {"资源ID", "资源名称", "类型", "位置"};
        resourceTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resourceTable = new JTable(resourceTableModel);
        resourceTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // 输入字段
        groupNameField = new JTextField(20);
        securityLevelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        groupInfoArea = new JTextArea(5, 30);
        groupInfoArea.setEditable(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 左侧：资源组列表
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("资源组列表"));
        leftPanel.add(new JScrollPane(groupList), BorderLayout.CENTER);
        
        JPanel leftButtonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        leftButtonPanel.add(new JButton("新建资源组") {{
            addActionListener(e -> createNewGroup());
        }});
        leftButtonPanel.add(new JButton("自动创建资源组") {{
            addActionListener(e -> autoCreateGroups());
        }});
        JButton deleteButton = new JButton("删除资源组");
        deleteButton.setForeground(Color.RED);
        deleteButton.addActionListener(e -> deleteGroup());
        leftButtonPanel.add(deleteButton);
        leftButtonPanel.add(new JButton("刷新列表") {{
            addActionListener(e -> {
                loadGroups();
                if (groupList.getSelectedValue() != null) {
                    loadSelectedGroup();
                    refreshAvailableResourceTable();
                }
            });
        }});
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // 中间：资源列表
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("资源列表"));
        
        // 顶部：资源组信息
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        infoPanel.add(new JLabel("资源组名称:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(groupNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        infoPanel.add(new JLabel("安全级别:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(securityLevelSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("添加资源") {{
            addActionListener(e -> addResourceToGroup());
        }});
        buttonPanel.add(new JButton("移除资源") {{
            addActionListener(e -> removeResourceFromGroup());
        }});
        buttonPanel.add(new JButton("保存资源组") {{
            addActionListener(e -> saveGroup());
        }});
        infoPanel.add(buttonPanel, gbc);
        
        centerPanel.add(infoPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(resourceTable), BorderLayout.CENTER);
        centerPanel.add(new JScrollPane(groupInfoArea), BorderLayout.SOUTH);
        
        // 右侧：可用资源列表
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("可用资源（双击或使用按钮添加到资源组）"));
        
        JTable availableResourceTable = createAvailableResourceTable();
        
        // 添加双击事件：双击资源行直接添加到资源组
        availableResourceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = availableResourceTable.rowAtPoint(evt.getPoint());
                    if (row >= 0) {
                        String resourceId = (String) availableResourceTableModel.getValueAt(row, 0);
                        String status = (String) availableResourceTableModel.getValueAt(row, 4);
                        if ("未添加".equals(status)) {
                            addResourceToGroupById(resourceId);
                        } else {
                            JOptionPane.showMessageDialog(ResourceGroupManagementPanel.this,
                                "该资源已添加到当前资源组", "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        });
        
        rightPanel.add(new JScrollPane(availableResourceTable), BorderLayout.CENTER);
        
        // 添加按钮面板
        JPanel rightButtonPanel = new JPanel(new FlowLayout());
        rightButtonPanel.add(new JButton("添加到资源组") {{
            addActionListener(e -> addSelectedResourcesFromTable());
        }});
        rightButtonPanel.add(new JButton("刷新列表") {{
            addActionListener(e -> refreshAvailableResourceTable());
        }});
        rightPanel.add(rightButtonPanel, BorderLayout.SOUTH);
        
        // 主布局
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
        String[] columnNames = {"资源ID", "资源名称", "类型", "位置", "状态"};
        availableResourceTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        refreshAvailableResourceTable();
        
        availableResourceTable = new JTable(availableResourceTableModel);
        availableResourceTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return availableResourceTable;
    }
    
    /**
     * 刷新可用资源表格（显示未添加到当前选中资源组的资源）
     */
    private void refreshAvailableResourceTable() {
        if (availableResourceTableModel == null) {
            return;
        }
        
        availableResourceTableModel.setRowCount(0);
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> allResources = dbManager.loadAllResources();
        
        // 获取当前选中资源组已包含的资源ID
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
                inGroup ? "已添加" : "未添加"
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
            
            // 加载资源列表
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
            
            // 显示资源组信息
            groupInfoArea.setText(String.format(
                "资源组: %s\n安全级别: %d\n资源数量: %d\n文件路径: %s",
                group.getName(),
                group.getSecurityLevel(),
                group.getResourceIds().size(),
                group.getFilePath() != null ? group.getFilePath() : "未保存"
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
        String name = JOptionPane.showInputDialog(this, "请输入资源组名称:", "新建资源组", 
            JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            int securityLevel = (Integer) securityLevelSpinner.getValue();
            ResourceGroup group = new ResourceGroup(name.trim(), securityLevel);
            GroupManager groupManager = new GroupManager();
            try {
                groupManager.saveGroup(group);
                loadGroups();
                groupList.setSelectedValue(name.trim(), true);
                JOptionPane.showMessageDialog(this, "资源组创建成功", "成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "创建资源组失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, 
                "请先在左侧列表中选择要删除的资源组", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 获取资源组信息
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        int resourceCount = group != null ? group.getResourceIds().size() : 0;
        
        String message = String.format(
            "确定要删除资源组 \"%s\" 吗？\n\n" +
            "资源组信息：\n" +
            "• 名称: %s\n" +
            "• 安全级别: %d\n" +
            "• 包含资源数: %d\n\n" +
            "警告：此操作将：\n" +
            "• 删除资源组JSON文件\n" +
            "• 删除数据库中的关联记录\n" +
            "• 此操作不可恢复！",
            selected, 
            selected,
            group != null ? group.getSecurityLevel() : 0,
            resourceCount
        );
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            message, 
            "确认删除资源组", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // 在删除资源组之前，先从所有配置文件中移除对该资源组的引用
                ProfileManager profileManager = accessControlSystem.getProfileManager();
                Map<String, Profile> allProfiles = profileManager.getAllProfiles();
                int updatedProfileCount = 0;
                
                for (Profile profile : allProfiles.values()) {
                    if (profile.getAccessRights().containsKey(selected)) {
                        // 从配置文件中移除该资源组的引用
                        profile.removeAccessRight(selected);
                        try {
                            profileManager.saveProfile(profile);
                            updatedProfileCount++;
                        } catch (Exception e) {
                            System.err.println("更新配置文件失败: " + profile.getName() + " - " + e.getMessage());
                        }
                    }
                }
                
                // 删除资源组
                groupManager.deleteGroup(selected);
                
                // 从数据库中删除关联记录
                try {
                    String sql = "DELETE FROM resource_group_members WHERE group_name = ?";
                    try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
                        pstmt.setString(1, selected);
                        pstmt.executeUpdate();
                    }
                } catch (Exception e) {
                    // 忽略数据库删除错误，可能表不存在或已删除
                }
                
                // 重新加载数据
                accessControlSystem.getAccessRequestProcessor().reloadData();
                loadGroups();
                clearGroupInfo();
                refreshAvailableResourceTable();
                
                String successMessage = "资源组 \"" + selected + "\" 已成功删除";
                if (updatedProfileCount > 0) {
                    successMessage += "\n\n已从 " + updatedProfileCount + " 个配置文件中移除了对该资源组的引用";
                }
                
                JOptionPane.showMessageDialog(this, 
                    successMessage, "删除成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "删除资源组失败: " + e.getMessage() + "\n\n请检查文件权限和数据库连接。", 
                    "删除失败", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void addResourceToGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择或创建一个资源组", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 显示可用资源选择对话框
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> allResources = dbManager.loadAllResources();
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        
        if (group == null) {
            return;
        }
        
        // 获取未添加到该组的资源
        List<String> availableResourceIds = new ArrayList<>();
        List<String> availableResourceNames = new ArrayList<>();
        
        for (Resource resource : allResources.values()) {
            if (!group.getResourceIds().contains(resource.getId())) {
                availableResourceIds.add(resource.getId());
                availableResourceNames.add(resource.getName() + " (" + resource.getId().substring(0, 8) + "...)");
            }
        }
        
        if (availableResourceIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可添加的资源", "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String selectedResourceName = (String) JOptionPane.showInputDialog(this,
            "选择要添加到资源组的资源:",
            "添加资源",
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
                refreshAvailableResourceTable(); // 刷新可用资源列表
                JOptionPane.showMessageDialog(this, "资源已添加到资源组", "成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "添加资源失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * 通过资源ID直接添加资源到资源组
     */
    private void addResourceToGroupById(String resourceId) {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择或创建一个资源组", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        
        if (group == null) {
            return;
        }
        
        // 检查资源是否已在组中
        if (group.getResourceIds().contains(resourceId)) {
            JOptionPane.showMessageDialog(this, "该资源已在此资源组中", "提示", 
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
            JOptionPane.showMessageDialog(this, "资源已添加到资源组", "成功", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "添加资源失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 从右侧表格添加选中的资源到资源组
     */
    private void addSelectedResourcesFromTable() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择或创建一个资源组", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int[] selectedRows = availableResourceTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "请先选择要添加的资源", "提示", 
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
            
            if ("已添加".equals(status)) {
                skippedCount++;
                continue;
            }
            
            if (!group.getResourceIds().contains(resourceId)) {
                group.addResource(resourceId);
                try {
                    dbManager.linkResourceToGroup(resourceId, selected);
                    addedCount++;
                } catch (Exception e) {
                    // 忽略单个资源添加错误
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
                    String.format("成功添加 %d 个资源到资源组\n跳过 %d 个已存在的资源", 
                        addedCount, skippedCount), 
                    "成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "保存资源组失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        } else if (skippedCount > 0) {
            JOptionPane.showMessageDialog(this, 
                "所选资源已全部添加到资源组中", "提示", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void removeResourceFromGroup() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择要移除的资源", "提示", 
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
            "确定要从资源组中移除资源 \"" + resourceName + "\" 吗？", 
            "确认移除", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            GroupManager groupManager = new GroupManager();
            ResourceGroup group = groupManager.getGroup(selected);
            if (group != null) {
                group.removeResource(resourceId);
                try {
                    groupManager.saveGroup(group);
                    // 从数据库中删除关联
                    String sql = "DELETE FROM resource_group_members WHERE resource_id = ? AND group_name = ?";
                    try (PreparedStatement pstmt = accessControlSystem.getDatabaseManager().getConnection().prepareStatement(sql)) {
                        pstmt.setString(1, resourceId);
                        pstmt.setString(2, selected);
                        pstmt.executeUpdate();
                    }
                    accessControlSystem.getAccessRequestProcessor().reloadData();
                    loadSelectedGroup();
                    refreshAvailableResourceTable(); // 刷新可用资源列表
                    JOptionPane.showMessageDialog(this, "资源已从资源组中移除", "成功", 
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "移除资源失败: " + e.getMessage(), 
                        "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void saveGroup() {
        String selected = groupList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请选择要保存的资源组", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String newName = groupNameField.getText().trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "资源组名称不能为空", "错误", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int securityLevel = (Integer) securityLevelSpinner.getValue();
        
        GroupManager groupManager = new GroupManager();
        ResourceGroup group = groupManager.getGroup(selected);
        if (group != null) {
            // 如果名称改变，需要重命名
            if (!selected.equals(newName)) {
                group.setName(newName);
                try {
                    groupManager.deleteGroup(selected);
                } catch (Exception e) {
                    // 忽略删除错误
                }
            }
            
            group.setSecurityLevel(securityLevel);
            
            try {
                groupManager.saveGroup(group);
                loadGroups();
                groupList.setSelectedValue(newName, true);
                JOptionPane.showMessageDialog(this, "资源组保存成功", "成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "保存资源组失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * 根据现有资源自动创建资源组
     * 根据建筑、楼层、类型等属性自动分组
     */
    private void autoCreateGroups() {
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> allResources = dbManager.loadAllResources();
        
        if (allResources.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "没有可用的资源，请先在资源管理中创建资源", 
                "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 显示分组策略选择对话框
        String[] strategies = {
            "按建筑分组",
            "按楼层分组", 
            "按资源类型分组",
            "按建筑+楼层分组",
            "按建筑+类型分组",
            "按楼层+类型分组",
            "创建所有可能的组合"
        };
        
        String selectedStrategy = (String) JOptionPane.showInputDialog(this,
            "选择资源组创建策略:",
            "自动创建资源组",
            JOptionPane.QUESTION_MESSAGE,
            null,
            strategies,
            strategies[6]); // 默认选择"创建所有可能的组合"
        
        if (selectedStrategy == null) {
            return;
        }
        
        GroupManager groupManager = new GroupManager();
        int createdCount = 0;
        int skippedCount = 0;
        
        try {
            if ("创建所有可能的组合".equals(selectedStrategy)) {
                // 创建所有可能的组合
                createdCount = createAllPossibleGroups(allResources, groupManager);
            } else {
                // 根据选择的策略创建
                createdCount = createGroupsByStrategy(allResources, groupManager, selectedStrategy);
            }
            
            // 刷新列表
            loadGroups();
            
            JOptionPane.showMessageDialog(this, 
                String.format("成功创建 %d 个资源组\n跳过 %d 个已存在的资源组", 
                    createdCount, skippedCount), 
                "完成", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "创建资源组时出错: " + e.getMessage(), 
                "错误", 
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
            
            // 检查是否已存在
            ResourceGroup existingGroup = groupManager.getGroup(groupName);
            if (existingGroup != null) {
                // 如果已存在，检查是否需要添加新资源
                boolean hasNewResources = false;
                for (String resourceId : resourceIds) {
                    if (!existingGroup.getResourceIds().contains(resourceId)) {
                        existingGroup.addResource(resourceId);
                        hasNewResources = true;
                        try {
                            dbManager.linkResourceToGroup(resourceId, groupName);
                        } catch (Exception e) {
                            // 忽略关联错误
                        }
                    }
                }
                if (hasNewResources) {
                    groupManager.saveGroup(existingGroup);
                }
                continue;
            }
            
            // 创建新资源组
            ResourceGroup group = new ResourceGroup(groupName, calculateSecurityLevel(resourceIds, resources));
            for (String resourceId : resourceIds) {
                group.addResource(resourceId);
            }
            
            groupManager.saveGroup(group);
            
            // 关联到数据库
            for (String resourceId : resourceIds) {
                try {
                    dbManager.linkResourceToGroup(resourceId, groupName);
                } catch (Exception e) {
                    // 忽略关联错误
                }
            }
            
            created++;
        }
        
        return created;
    }
    
    private int createAllPossibleGroups(Map<String, Resource> resources, 
                                       GroupManager groupManager) throws Exception {
        int created = 0;
        
        // 1. 按建筑分组
        created += createGroupsByStrategy(resources, groupManager, "按建筑分组");
        
        // 2. 按楼层分组
        created += createGroupsByStrategy(resources, groupManager, "按楼层分组");
        
        // 3. 按类型分组
        created += createGroupsByStrategy(resources, groupManager, "按资源类型分组");
        
        // 4. 按建筑+楼层分组
        created += createGroupsByStrategy(resources, groupManager, "按建筑+楼层分组");
        
        // 5. 按建筑+类型分组
        created += createGroupsByStrategy(resources, groupManager, "按建筑+类型分组");
        
        // 6. 按楼层+类型分组
        created += createGroupsByStrategy(resources, groupManager, "按楼层+类型分组");
        
        return created;
    }
    
    private String getGroupKey(Resource resource, String strategy) {
        String building = resource.getBuilding() != null ? resource.getBuilding() : "未指定建筑";
        String floor = resource.getFloor() != null ? resource.getFloor() : "未指定楼层";
        String type = resource.getType() != null ? resource.getType().toString() : "未指定类型";
        
        switch (strategy) {
            case "按建筑分组":
                return building + "区域";
            case "按楼层分组":
                return floor + "楼层";
            case "按资源类型分组":
                return type + "资源组";
            case "按建筑+楼层分组":
                return building + "-" + floor;
            case "按建筑+类型分组":
                return building + "-" + type;
            case "按楼层+类型分组":
                return floor + "-" + type;
            default:
                return null;
        }
    }
    
    /**
     * 根据资源列表计算安全级别
     * 简单策略：根据资源类型和数量计算
     */
    private int calculateSecurityLevel(List<String> resourceIds, Map<String, Resource> allResources) {
        if (resourceIds.isEmpty()) {
            return 1;
        }
        
        // 检查是否有高安全级别的资源类型
        int maxLevel = 1;
        for (String resourceId : resourceIds) {
            Resource resource = allResources.get(resourceId);
            if (resource != null) {
                Resource.ResourceType type = resource.getType();
                // 根据类型设置安全级别
                if (type == Resource.ResourceType.GATE || 
                    type == Resource.ResourceType.PARKING) {
                    maxLevel = Math.max(maxLevel, 1); // 公共区域
                } else if (type == Resource.ResourceType.DOOR || 
                          type == Resource.ResourceType.STAIRWAY) {
                    maxLevel = Math.max(maxLevel, 2); // 一般区域
                } else if (type == Resource.ResourceType.ELEVATOR) {
                    maxLevel = Math.max(maxLevel, 3); // 重要区域
                } else {
                    maxLevel = Math.max(maxLevel, 2);
                }
            }
        }
        
        return maxLevel;
    }
}

