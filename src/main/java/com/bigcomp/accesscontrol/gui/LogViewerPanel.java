package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.logging.LogManager;
import com.bigcomp.accesscontrol.util.AccessDiagnostic;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;

/**
 * 日志查看面板
 */
public class LogViewerPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JTable logTable;
    private DefaultTableModel tableModel;
    private LogManager logManager;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField badgeCodeField;
    private JTextField resourceIdField;
    private JTextField userIdField;
    private JComboBox<String> grantedCombo;
    
    public LogViewerPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.logManager = accessControlSystem.getLogManager();
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        String[] columnNames = {"时间", "徽章代码", "读卡器ID", "资源ID", "用户ID", "用户名", "状态"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(tableModel);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // 搜索字段
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = LocalDateTime.now().format(formatter);
        
        startDateField = new JTextField(today, 12);
        endDateField = new JTextField(today, 12);
        badgeCodeField = new JTextField(15);
        resourceIdField = new JTextField(15);
        userIdField = new JTextField(15);
        grantedCombo = new JComboBox<>(new String[]{"全部", "授权", "拒绝"});
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 搜索面板
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("搜索条件"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 第一行：日期范围
        gbc.gridx = 0; gbc.gridy = 0;
        searchPanel.add(new JLabel("开始日期 (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        searchPanel.add(startDateField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("结束日期 (yyyy-MM-dd):"), gbc);
        gbc.gridx = 3;
        searchPanel.add(endDateField, gbc);
        
        // 第二行：徽章代码和资源ID
        gbc.gridx = 0; gbc.gridy = 1;
        searchPanel.add(new JLabel("徽章代码:"), gbc);
        gbc.gridx = 1;
        searchPanel.add(badgeCodeField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("资源ID:"), gbc);
        gbc.gridx = 3;
        searchPanel.add(resourceIdField, gbc);
        
        // 第三行：用户ID和状态
        gbc.gridx = 0; gbc.gridy = 2;
        searchPanel.add(new JLabel("用户ID:"), gbc);
        gbc.gridx = 1;
        searchPanel.add(userIdField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("状态:"), gbc);
        gbc.gridx = 3;
        searchPanel.add(grantedCombo, gbc);
        
        // 按钮
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("搜索") {{
            addActionListener(e -> searchLogs());
        }});
        buttonPanel.add(new JButton("清空条件") {{
            addActionListener(e -> clearSearchFields());
        }});
        buttonPanel.add(new JButton("导出日志") {{
            addActionListener(e -> exportLogs());
        }});
        buttonPanel.add(new JButton("清空日志") {{
            addActionListener(e -> clearLogs());
        }});
        searchPanel.add(buttonPanel, gbc);
        
        // 表格
        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("日志记录"));
        
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // 默认加载今天的日志（静默模式，不显示提示）
        SwingUtilities.invokeLater(() -> {
            searchLogsSilent();
        });
    }
    
    private void searchLogs() {
        searchLogsInternal(true);
    }
    
    private void searchLogsSilent() {
        searchLogsInternal(false);
    }
    
    private void searchLogsInternal(boolean showMessage) {
        try {
            // 解析日期
            LocalDateTime startDate = LocalDateTime.parse(startDateField.getText().trim() + "T00:00:00", 
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            LocalDateTime endDate = LocalDateTime.parse(endDateField.getText().trim() + "T23:59:59", 
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            
            // 创建搜索条件
            LogManager.LogSearchCriteria criteria = new LogManager.LogSearchCriteria();
            criteria.setStartDate(startDate);
            criteria.setEndDate(endDate);
            
            String badgeCode = badgeCodeField.getText().trim();
            if (!badgeCode.isEmpty()) {
                criteria.setBadgeCode(badgeCode);
            }
            
            String resourceId = resourceIdField.getText().trim();
            if (!resourceId.isEmpty()) {
                criteria.setResourceId(resourceId);
            }
            
            String userId = userIdField.getText().trim();
            if (!userId.isEmpty()) {
                criteria.setUserId(userId);
            }
            
            String status = (String) grantedCombo.getSelectedItem();
            if ("授权".equals(status)) {
                criteria.setGranted(true);
            } else if ("拒绝".equals(status)) {
                criteria.setGranted(false);
            }
            
            // 执行搜索
            List<LogManager.LogEntry> results = logManager.searchLogs(criteria);
            
            // 更新表格
            tableModel.setRowCount(0);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (LogManager.LogEntry entry : results) {
                tableModel.addRow(new Object[]{
                    entry.getTimestamp().format(timeFormatter),
                    entry.getBadgeCode(),
                    entry.getBadgeReaderId(),
                    entry.getResourceId(),
                    entry.getUserId(),
                    entry.getUserName(),
                    entry.isGranted() ? "授权" : "拒绝"
                });
            }
            
            // 只在手动搜索时显示消息
            if (showMessage) {
                if (results.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "未找到匹配的日志记录", 
                        "搜索结果", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "找到 " + results.size() + " 条日志记录", 
                        "搜索完成", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            
        } catch (DateTimeParseException e) {
            if (showMessage) {
                JOptionPane.showMessageDialog(this, 
                    "日期格式错误，请使用 yyyy-MM-dd 格式", 
                    "错误", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            if (showMessage) {
                JOptionPane.showMessageDialog(this, 
                    "搜索日志失败: " + e.getMessage(), 
                    "错误", 
                    JOptionPane.ERROR_MESSAGE);
            }
            e.printStackTrace();
        }
    }
    
    private void clearSearchFields() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = LocalDateTime.now().format(formatter);
        startDateField.setText(today);
        endDateField.setText(today);
        badgeCodeField.setText("");
        resourceIdField.setText("");
        userIdField.setText("");
        grantedCombo.setSelectedIndex(0);
    }
    
    private void exportLogs() {
        // 先执行搜索获取当前显示的日志
        List<LogManager.LogEntry> entries = new ArrayList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime startDate = LocalDateTime.parse(startDateField.getText().trim() + "T00:00:00", 
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            LocalDateTime endDate = LocalDateTime.parse(endDateField.getText().trim() + "T23:59:59", 
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            
            LogManager.LogSearchCriteria criteria = new LogManager.LogSearchCriteria();
            criteria.setStartDate(startDate);
            criteria.setEndDate(endDate);
            
            String badgeCode = badgeCodeField.getText().trim();
            if (!badgeCode.isEmpty()) {
                criteria.setBadgeCode(badgeCode);
            }
            
            String resourceId = resourceIdField.getText().trim();
            if (!resourceId.isEmpty()) {
                criteria.setResourceId(resourceId);
            }
            
            String userId = userIdField.getText().trim();
            if (!userId.isEmpty()) {
                criteria.setUserId(userId);
            }
            
            String status = (String) grantedCombo.getSelectedItem();
            if ("授权".equals(status)) {
                criteria.setGranted(true);
            } else if ("拒绝".equals(status)) {
                criteria.setGranted(false);
            }
            
            entries = logManager.searchLogs(criteria);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "获取日志数据失败: " + e.getMessage(), 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "没有可导出的日志数据", 
                "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出日志");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new java.io.File("access_logs_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                // 确保文件扩展名是.csv
                String filePath = file.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                    file = new java.io.File(filePath);
                }
                
                // 写入CSV文件
                try (java.io.FileWriter writer = new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                    // 写入BOM以支持Excel正确显示中文
                    writer.write('\ufeff');
                    
                    // 写入表头
                    writer.write("时间,徽章代码,读卡器ID,资源ID,用户ID,用户名,状态\n");
                    
                    // 写入数据
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    for (LogManager.LogEntry entry : entries) {
                        writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            entry.getTimestamp().format(timeFormatter),
                            entry.getBadgeCode(),
                            entry.getBadgeReaderId(),
                            entry.getResourceId(),
                            entry.getUserId(),
                            entry.getUserName(),
                            entry.isGranted() ? "授权" : "拒绝"
                        ));
                    }
                }
                
                JOptionPane.showMessageDialog(this, 
                    "成功导出 " + entries.size() + " 条日志记录到:\n" + file.getAbsolutePath(), 
                    "导出成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "导出日志失败: " + e.getMessage(), 
                    "错误", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 诊断选中的日志记录
     */
    private void diagnoseSelectedRecord() {
        int selectedRow = logTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, 
                "请先选择一条日志记录", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 获取选中行的数据
        String badgeCode = (String) tableModel.getValueAt(selectedRow, 1);
        String resourceId = (String) tableModel.getValueAt(selectedRow, 3);
        String status = (String) tableModel.getValueAt(selectedRow, 6);
        
        // 执行诊断
        AccessDiagnostic diagnostic = new AccessDiagnostic(accessControlSystem);
        String report = diagnostic.diagnoseAccessIssue(badgeCode, resourceId);
        
        // 显示诊断结果
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, 
            "访问控制诊断 - " + status, true);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(this);
        
        JTextArea textArea = new JTextArea(report);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("关闭") {{
            addActionListener(e -> dialog.dispose());
        }});
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    /**
     * 显示系统状态报告
     */
    private void showSystemStatusReport() {
        AccessDiagnostic diagnostic = new AccessDiagnostic(accessControlSystem);
        String report = diagnostic.generateSystemStatusReport();
        
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, 
            "系统状态报告", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        
        JTextArea textArea = new JTextArea(report);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("关闭") {{
            addActionListener(e -> dialog.dispose());
        }});
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    /**
     * 清空日志文件
     */
    private void clearLogs() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要清空所有日志文件吗？\n\n" +
            "此操作将：\n" +
            "• 删除 data/logs/ 目录下的所有日志文件\n" +
            "• 此操作不可恢复！\n\n" +
            "是否继续？",
            "确认清空日志",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        try {
            java.nio.file.Path logsDir = java.nio.file.Paths.get("data/logs");
            if (!java.nio.file.Files.exists(logsDir)) {
                JOptionPane.showMessageDialog(this, 
                    "日志目录不存在，无需清空", "提示", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // 统计要删除的文件
            int fileCount = 0;
            final long[] totalSize = {0};
            java.util.List<java.nio.file.Path> filesToDelete = new java.util.ArrayList<>();
            
            java.nio.file.Files.walk(logsDir)
                .filter(java.nio.file.Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".csv"))
                .forEach(path -> {
                    try {
                        totalSize[0] += java.nio.file.Files.size(path);
                        filesToDelete.add(path);
                    } catch (Exception e) {
                        // 忽略统计错误
                    }
                });
            
            fileCount = filesToDelete.size();
            
            if (fileCount == 0) {
                JOptionPane.showMessageDialog(this, 
                    "没有找到日志文件", "提示", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // 再次确认
            long totalSizeValue = totalSize[0];
            String sizeStr = totalSizeValue > 1024 * 1024 ? 
                String.format("%.2f MB", totalSizeValue / (1024.0 * 1024.0)) :
                String.format("%.2f KB", totalSizeValue / 1024.0);
            
            int finalConfirm = JOptionPane.showConfirmDialog(this,
                "即将删除 " + fileCount + " 个日志文件（总大小: " + sizeStr + "）\n\n" +
                "确定要继续吗？",
                "最终确认",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (finalConfirm != JOptionPane.YES_OPTION) {
                return;
            }
            
            // 删除所有日志文件
            int deletedCount = 0;
            int errorCount = 0;
            
            for (java.nio.file.Path file : filesToDelete) {
                try {
                    java.nio.file.Files.delete(file);
                    deletedCount++;
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("删除日志文件失败: " + file + " - " + e.getMessage());
                }
            }
            
            // 清空表格
            tableModel.setRowCount(0);
            
            // 显示结果
            String message = String.format(
                "日志清空完成！\n\n" +
                "统计信息：\n" +
                "• 成功删除: %d 个文件\n" +
                "• 失败: %d 个文件",
                deletedCount, errorCount
            );
            
            JOptionPane.showMessageDialog(this, message, 
                "清空完成", 
                deletedCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "清空日志失败: " + e.getMessage(), 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
