package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.logging.LogManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;

/**
 * 日志查看面板
 */
public class LogViewerPanel extends JPanel {
    private JTable logTable;
    private DefaultTableModel tableModel;
    private LogManager logManager;

    public LogViewerPanel() {
        this.logManager = new LogManager();
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        String[] columnNames = {"时间", "徽章代码", "读卡器ID", "资源ID", "用户", "状态"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(tableModel);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 搜索面板
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("开始日期:"));
        searchPanel.add(new JTextField(10));
        searchPanel.add(new JLabel("结束日期:"));
        searchPanel.add(new JTextField(10));
        searchPanel.add(new JButton("搜索"));
        
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(logTable), BorderLayout.CENTER);
    }
}

