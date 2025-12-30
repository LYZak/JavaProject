// Group 2 ChenGong ZhangZhao LiangYiKuo
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
 * Log Viewer Panel
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
        String[] columnNames = {"Time", "Badge Code", "Badge Reader ID", "Resource ID", "User ID", "User Name", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(tableModel);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Search fields
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = LocalDateTime.now().format(formatter);
        
        startDateField = new JTextField(today, 12);
        endDateField = new JTextField(today, 12);
        badgeCodeField = new JTextField(15);
        resourceIdField = new JTextField(15);
        userIdField = new JTextField(15);
        grantedCombo = new JComboBox<>(new String[]{"All", "Granted", "Denied"});
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Search panel
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Criteria"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // First row: Date range
        gbc.gridx = 0; gbc.gridy = 0;
        searchPanel.add(new JLabel("Start Date (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        searchPanel.add(startDateField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("End Date (yyyy-MM-dd):"), gbc);
        gbc.gridx = 3;
        searchPanel.add(endDateField, gbc);
        
        // Second row: Badge code and resource ID
        gbc.gridx = 0; gbc.gridy = 1;
        searchPanel.add(new JLabel("Badge Code:"), gbc);
        gbc.gridx = 1;
        searchPanel.add(badgeCodeField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("Resource ID:"), gbc);
        gbc.gridx = 3;
        searchPanel.add(resourceIdField, gbc);
        
        // Third row: User ID and status
        gbc.gridx = 0; gbc.gridy = 2;
        searchPanel.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        searchPanel.add(userIdField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 3;
        searchPanel.add(grantedCombo, gbc);
        
        // Buttons
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("Search") {{
            addActionListener(e -> searchLogs());
        }});
        buttonPanel.add(new JButton("Clear Criteria") {{
            addActionListener(e -> clearSearchFields());
        }});
        buttonPanel.add(new JButton("Export Logs") {{
            addActionListener(e -> exportLogs());
        }});
        buttonPanel.add(new JButton("Clear Logs") {{
            addActionListener(e -> clearLogs());
        }});
        searchPanel.add(buttonPanel, gbc);
        
        // Table
        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log Records"));
        
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Default load today's logs (silent mode, no prompts)
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
            // Parse dates
            LocalDateTime startDate = LocalDateTime.parse(startDateField.getText().trim() + "T00:00:00", 
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            LocalDateTime endDate = LocalDateTime.parse(endDateField.getText().trim() + "T23:59:59", 
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            
            // Create search criteria
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
            if ("Granted".equals(status)) {
                criteria.setGranted(true);
            } else if ("Denied".equals(status)) {
                criteria.setGranted(false);
            }
            
            // Execute search
            List<LogManager.LogEntry> results = logManager.searchLogs(criteria);
            
            // Update table
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
                    entry.isGranted() ? "Granted" : "Denied"
                });
            }
            
            // Only show message when manually searching
            if (showMessage) {
                if (results.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "No matching log records found", 
                        "Search Results", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Found " + results.size() + " log records", 
                        "Search Complete", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            
        } catch (DateTimeParseException e) {
            if (showMessage) {
                JOptionPane.showMessageDialog(this, 
                    "Date format error, please use yyyy-MM-dd format", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            if (showMessage) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to search logs: " + e.getMessage(), 
                    "Error", 
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
        // First execute search to get currently displayed logs
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
            if ("Granted".equals(status)) {
                criteria.setGranted(true);
            } else if ("Denied".equals(status)) {
                criteria.setGranted(false);
            }
            
            entries = logManager.searchLogs(criteria);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to get log data: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No log data to export", 
                "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Logs");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new java.io.File("access_logs_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                // Ensure file extension is .csv
                String filePath = file.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                    file = new java.io.File(filePath);
                }
                
                // Write CSV file
                try (java.io.FileWriter writer = new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                    // Write BOM to support Excel displaying correctly
                    writer.write('\ufeff');
                    
                    // Write header
                    writer.write("Time,Badge Code,Badge Reader ID,Resource ID,User ID,User Name,Status\n");
                    
                    // Write data
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    for (LogManager.LogEntry entry : entries) {
                        writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            entry.getTimestamp().format(timeFormatter),
                            entry.getBadgeCode(),
                            entry.getBadgeReaderId(),
                            entry.getResourceId(),
                            entry.getUserId(),
                            entry.getUserName(),
                            entry.isGranted() ? "Granted" : "Denied"
                        ));
                    }
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Successfully exported " + entries.size() + " log records to:\n" + file.getAbsolutePath(), 
                    "Export Successful", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to export logs: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Diagnose selected log record
     */
    private void diagnoseSelectedRecord() {
        int selectedRow = logTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select a log record first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get selected row data
        String badgeCode = (String) tableModel.getValueAt(selectedRow, 1);
        String resourceId = (String) tableModel.getValueAt(selectedRow, 3);
        String status = (String) tableModel.getValueAt(selectedRow, 6);
        
        // Execute diagnosis
        AccessDiagnostic diagnostic = new AccessDiagnostic(accessControlSystem);
        String report = diagnostic.diagnoseAccessIssue(badgeCode, resourceId);
        
        // Display diagnosis results
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, 
            "Access Control Diagnosis - " + status, true);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(this);
        
        JTextArea textArea = new JTextArea(report);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("Close") {{
            addActionListener(e -> dialog.dispose());
        }});
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    /**
     * Show system status report
     */
    private void showSystemStatusReport() {
        AccessDiagnostic diagnostic = new AccessDiagnostic(accessControlSystem);
        String report = diagnostic.generateSystemStatusReport();
        
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, 
            "System Status Report", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        
        JTextArea textArea = new JTextArea(report);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("Close") {{
            addActionListener(e -> dialog.dispose());
        }});
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    /**
     * Clear log files
     */
    private void clearLogs() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear all log files?\n\n" +
            "This operation will:\n" +
            "• Delete all log files in data/logs/ directory\n" +
            "• This operation cannot be undone!\n\n" +
            "Continue?",
            "Confirm Clear Logs",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        try {
            java.nio.file.Path logsDir = java.nio.file.Paths.get("data/logs");
            if (!java.nio.file.Files.exists(logsDir)) {
                JOptionPane.showMessageDialog(this, 
                    "Log directory does not exist, no need to clear", "Info", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Count files to delete
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
                        // Ignore counting errors
                    }
                });
            
            fileCount = filesToDelete.size();
            
            if (fileCount == 0) {
                JOptionPane.showMessageDialog(this, 
                    "No log files found", "Info", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Confirm again
            long totalSizeValue = totalSize[0];
            String sizeStr = totalSizeValue > 1024 * 1024 ? 
                String.format("%.2f MB", totalSizeValue / (1024.0 * 1024.0)) :
                String.format("%.2f KB", totalSizeValue / 1024.0);
            
            int finalConfirm = JOptionPane.showConfirmDialog(this,
                "About to delete " + fileCount + " log files (Total size: " + sizeStr + ")\n\n" +
                "Are you sure you want to continue?",
                "Final Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (finalConfirm != JOptionPane.YES_OPTION) {
                return;
            }
            
            // Delete all log files
            int deletedCount = 0;
            int errorCount = 0;
            
            for (java.nio.file.Path file : filesToDelete) {
                try {
                    java.nio.file.Files.delete(file);
                    deletedCount++;
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("Failed to delete log file: " + file + " - " + e.getMessage());
                }
            }
            
            // Clear table
            tableModel.setRowCount(0);
            
            // Display results
            String message = String.format(
                "Log clearing completed!\n\n" +
                "Statistics:\n" +
                "• Successfully deleted: %d files\n" +
                "• Failed: %d files",
                deletedCount, errorCount
            );
            
            JOptionPane.showMessageDialog(this, message, 
                "Clear Complete", 
                deletedCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to clear logs: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
