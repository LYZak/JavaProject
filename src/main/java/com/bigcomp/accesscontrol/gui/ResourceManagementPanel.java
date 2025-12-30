package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.profile.GroupManager;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.UUID;
import java.util.Map;

/**
 * 资源管理面板
 */
public class ResourceManagementPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JTable resourceTable;
    private DefaultTableModel tableModel;
    private DatabaseManager dbManager;
    private JTextField nameField;
    private JComboBox<Resource.ResourceType> typeCombo;
    private JTextField locationField;
    private JTextField buildingField;
    private JTextField floorField;

    public ResourceManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.dbManager = accessControlSystem.getDatabaseManager();
        initializeComponents();
        setupLayout();
        loadResources();
    }

    private void initializeComponents() {
        String[] columnNames = {"ID", "名称", "类型", "位置", "建筑", "楼层", "状态"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // 只有状态列可编辑
            }
        };
        resourceTable = new JTable(tableModel);
        resourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 输入字段
        nameField = new JTextField(15);
        typeCombo = new JComboBox<>(Resource.ResourceType.values());
        locationField = new JTextField(15);
        buildingField = new JTextField(15);
        floorField = new JTextField(15);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部：输入表单
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("名称:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("类型:"), gbc);
        gbc.gridx = 1;
        formPanel.add(typeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("位置:"), gbc);
        gbc.gridx = 1;
        formPanel.add(locationField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("建筑:"), gbc);
        gbc.gridx = 1;
        formPanel.add(buildingField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("楼层:"), gbc);
        gbc.gridx = 1;
        formPanel.add(floorField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("添加资源") {{
            addActionListener(e -> addResource());
        }});
        buttonPanel.add(new JButton("删除资源") {{
            addActionListener(e -> deleteResource());
        }});
        buttonPanel.add(new JButton("创建读卡器") {{
            addActionListener(e -> createBadgeReader());
        }});
        buttonPanel.add(new JButton("为所有资源创建读卡器") {{
            addActionListener(e -> createBadgeReadersForAll());
        }});
        buttonPanel.add(new JButton("关联到资源组") {{
            addActionListener(e -> linkToResourceGroup());
        }});
        formPanel.add(buttonPanel, gbc);
        
        // 中间：表格
        JScrollPane scrollPane = new JScrollPane(resourceTable);
        
        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void addResource() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入资源名称", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String resourceId = UUID.randomUUID().toString();
            Resource resource = new Resource(
                resourceId,
                name,
                (Resource.ResourceType) typeCombo.getSelectedItem(),
                locationField.getText().trim(),
                buildingField.getText().trim(),
                floorField.getText().trim()
            );
            
            dbManager.addResource(resource);
            loadResources();
            
            // 清空输入字段
            nameField.setText("");
            locationField.setText("");
            buildingField.setText("");
            floorField.setText("");
            
            JOptionPane.showMessageDialog(this, "资源添加成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "添加资源失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteResource() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的资源", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String resourceId = (String) tableModel.getValueAt(selectedRow, 0);
        String resourceName = (String) tableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "确定要删除资源 \"" + resourceName + "\" 吗？\n此操作将同时删除关联的读卡器和资源组关联。", 
            "确认删除", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteResource(resourceId);
                
                // 从路由器中注销读卡器
                var router = accessControlSystem.getRouter();
                Map<String, BadgeReader> readers = router.getBadgeReaders();
                BadgeReader readerToRemove = null;
                for (BadgeReader reader : readers.values()) {
                    if (reader.getResourceId().equals(resourceId)) {
                        readerToRemove = reader;
                        break;
                    }
                }
                if (readerToRemove != null) {
                    router.unregisterBadgeReader(readerToRemove.getId());
                }
                
                loadResources();
                JOptionPane.showMessageDialog(this, "资源删除成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "删除资源失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void createBadgeReader() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择资源", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String resourceId = (String) tableModel.getValueAt(selectedRow, 0);
        Resource resource = dbManager.loadAllResources().get(resourceId);
        
        // 检查资源是否已有读卡器
        if (resource != null && resource.getBadgeReaderId() != null && !resource.getBadgeReaderId().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "资源 \"" + resource.getName() + "\" 已有读卡器: " + resource.getBadgeReaderId() + "\n\n" +
                "是否要创建新的读卡器？这将替换现有的读卡器。",
                "确认",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        try {
            String readerId = UUID.randomUUID().toString();
            BadgeReader reader = new BadgeReader(readerId, resourceId);
            
            dbManager.addBadgeReader(reader);
            accessControlSystem.getRouter().registerBadgeReader(reader);
            
            // 更新资源
            if (resource != null) {
                resource.setBadgeReaderId(readerId);
                dbManager.addResource(resource);
            }
            
            loadResources();
            JOptionPane.showMessageDialog(this, "读卡器创建成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "创建读卡器失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 为所有资源创建读卡器
     */
    private void createBadgeReadersForAll() {
        Map<String, Resource> allResources = dbManager.loadAllResources();
        
        if (allResources.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "没有可用的资源，请先添加资源", "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 统计需要创建读卡器的资源
        int totalResources = allResources.size();
        int resourcesWithReader = 0;
        int resourcesWithoutReader = 0;
        
        for (Resource resource : allResources.values()) {
            if (resource.getBadgeReaderId() != null && !resource.getBadgeReaderId().isEmpty()) {
                resourcesWithReader++;
            } else {
                resourcesWithoutReader++;
            }
        }
        
        if (resourcesWithoutReader == 0) {
            JOptionPane.showMessageDialog(this, 
                "所有资源都已配置读卡器！\n\n" +
                "总资源数: " + totalResources + "\n" +
                "已有读卡器: " + resourcesWithReader,
                "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 确认对话框
        int confirm = JOptionPane.showConfirmDialog(this,
            "将为 " + resourcesWithoutReader + " 个资源创建读卡器\n\n" +
            "统计信息：\n" +
            "• 总资源数: " + totalResources + "\n" +
            "• 已有读卡器: " + resourcesWithReader + "\n" +
            "• 需要创建: " + resourcesWithoutReader + "\n\n" +
            "是否继续？",
            "为所有资源创建读卡器",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // 获取所有已存在的读卡器，用于生成新的ID
        var router = accessControlSystem.getRouter();
        Map<String, BadgeReader> existingReaders = router.getBadgeReaders();
        int maxReaderIndex = 0;
        
        // 查找最大的读卡器编号（格式：BR001, BR002等）
        for (BadgeReader reader : existingReaders.values()) {
            String readerId = reader.getId();
            if (readerId.startsWith("BR") && readerId.length() == 5) {
                try {
                    int index = Integer.parseInt(readerId.substring(2));
                    maxReaderIndex = Math.max(maxReaderIndex, index);
                } catch (NumberFormatException e) {
                    // 忽略非数字格式的ID
                }
            }
        }
        
        int createdCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        StringBuilder details = new StringBuilder();
        
        // 为没有读卡器的资源创建读卡器
        for (Resource resource : allResources.values()) {
            // 跳过已有读卡器的资源
            if (resource.getBadgeReaderId() != null && !resource.getBadgeReaderId().isEmpty()) {
                skippedCount++;
                continue;
            }
            
            try {
                maxReaderIndex++;
                String readerId = "BR" + String.format("%03d", maxReaderIndex);
                
                // 检查ID是否已存在
                while (existingReaders.containsKey(readerId)) {
                    maxReaderIndex++;
                    readerId = "BR" + String.format("%03d", maxReaderIndex);
                }
                
                BadgeReader reader = new BadgeReader(readerId, resource.getId());
                dbManager.addBadgeReader(reader);
                accessControlSystem.getRouter().registerBadgeReader(reader);
                
                // 更新资源
                resource.setBadgeReaderId(readerId);
                dbManager.addResource(resource);
                
                createdCount++;
                details.append("  ✓ ").append(resource.getName())
                       .append(" -> ").append(readerId).append("\n");
                
            } catch (Exception e) {
                errorCount++;
                details.append("  ✗ ").append(resource.getName())
                       .append(" -> 失败: ").append(e.getMessage()).append("\n");
            }
        }
        
        // 刷新资源列表
        loadResources();
        
        // 显示结果
        String message = String.format(
            "读卡器创建完成！\n\n" +
            "统计信息：\n" +
            "• 成功创建: %d 个\n" +
            "• 跳过（已有）: %d 个\n" +
            "• 失败: %d 个\n\n" +
            "详细信息：\n%s",
            createdCount, skippedCount, errorCount, details.toString()
        );
        
        JOptionPane.showMessageDialog(this, message, 
            "批量创建读卡器完成", 
            createdCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }
    
    private void linkToResourceGroup() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择资源", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String resourceId = (String) tableModel.getValueAt(selectedRow, 0);
        String resourceName = (String) tableModel.getValueAt(selectedRow, 1);
        
        // 获取所有可用的资源组
        GroupManager groupManager = new GroupManager();
        Map<String, com.bigcomp.accesscontrol.profile.ResourceGroup> groups = groupManager.getAllGroups();
        
        if (groups.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "没有可用的资源组。\n\n请先在 data/groups/ 目录下创建资源组JSON文件。\n" +
                "示例：{\"name\": \"办公室区域\", \"securityLevel\": 1, \"resources\": []}\n\n" +
                "创建后请重启程序或刷新资源组。", 
                "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String[] groupNames = groups.keySet().toArray(new String[0]);
        
        // 显示选择对话框
        String selectedGroup = (String) JOptionPane.showInputDialog(this,
            "选择要将资源 \"" + resourceName + "\" 关联到的资源组:",
            "关联到资源组",
            JOptionPane.QUESTION_MESSAGE,
            null,
            groupNames,
            groupNames.length > 0 ? groupNames[0] : null);
        
        if (selectedGroup != null) {
            try {
                // 关联到数据库
                dbManager.linkResourceToGroup(resourceId, selectedGroup);
                
                // 更新资源组文件（将资源ID添加到JSON文件）
                com.bigcomp.accesscontrol.profile.ResourceGroup group = groups.get(selectedGroup);
                if (group != null) {
                    group.addResource(resourceId);
                    groupManager.saveGroup(group);
                }
                
                // 重新加载内存数据
                accessControlSystem.getAccessRequestProcessor().reloadData();
                
                JOptionPane.showMessageDialog(this, 
                    "资源已关联到资源组 \"" + selectedGroup + "\"\n\n" +
                    "资源ID: " + resourceId + "\n" +
                    "现在可以在配置文件中为该资源组配置访问权限了。", 
                    "成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "关联资源组失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void loadResources() {
        tableModel.setRowCount(0);
        try {
            Map<String, Resource> resources = dbManager.loadAllResources();
            for (Resource resource : resources.values()) {
                tableModel.addRow(new Object[]{
                    resource.getId(),
                    resource.getName(),
                    resource.getType().toString(),
                    resource.getLocation(),
                    resource.getBuilding(),
                    resource.getFloor(),
                    resource.getState().toString()
                });
            }
            
            // 重新加载内存数据
            accessControlSystem.getAccessRequestProcessor().reloadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载资源失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}

