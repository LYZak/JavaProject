package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.UUID;

/**
 * 资源管理面板
 */
public class ResourceManagementPanel extends JPanel {
    private JTable resourceTable;
    private DefaultTableModel tableModel;
    private DatabaseManager dbManager;

    public ResourceManagementPanel() {
        this.dbManager = new DatabaseManager();
        initializeComponents();
        setupLayout();
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
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(new JScrollPane(resourceTable), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("添加资源"));
        buttonPanel.add(new JButton("删除资源"));
        buttonPanel.add(new JButton("批量设置状态"));
        add(buttonPanel, BorderLayout.SOUTH);
    }
}

