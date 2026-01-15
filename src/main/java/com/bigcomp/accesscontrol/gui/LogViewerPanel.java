// Group 2 ChenGong ZhangZhao LiangYizhuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.logging.LogManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
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
    private JTable logTable;
    private DefaultTableModel tableModel;
    private LogManager logManager;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField badgeCodeField;
    private JTextField resourceIdField;
    private JTextField userIdField;
    private JComboBox<String> grantedCombo;
    private TitledBorder searchBorder;
    private TitledBorder recordsBorder;
    private JLabel startDateLabel;
    private JLabel endDateLabel;
    private JLabel badgeCodeLabel;
    private JLabel resourceIdLabel;
    private JLabel userIdLabel;
    private JLabel statusLabel;
    private JButton searchButton;
    private JButton clearCriteriaButton;
    private JButton exportButton;
    private JButton clearLogsButton;
    
    public LogViewerPanel(AccessControlSystem accessControlSystem) {
        this.logManager = accessControlSystem.getLogManager();
        initializeComponents();
        setupLayout();
        applyLanguage();
    }
    
    private void initializeComponents() {
        tableModel = new DefaultTableModel(getColumnNames(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(tableModel);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        logTable.setAutoCreateRowSorter(true);
        logTable.setFillsViewportHeight(true);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(logTable);
        
        // Search fields
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = LocalDateTime.now().format(formatter);
        
        startDateField = new JTextField(today, 12);
        endDateField = new JTextField(today, 12);
        badgeCodeField = new JTextField(15);
        resourceIdField = new JTextField(15);
        userIdField = new JTextField(15);
        grantedCombo = new JComboBox<>(new String[]{I18n.t("log.status.all"), I18n.t("log.status.granted"), I18n.t("log.status.denied")});
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        
        // Search panel
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchBorder = new TitledBorder("");
        searchPanel.setBorder(searchBorder);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        
        // First row: Date range
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        startDateLabel = new JLabel();
        searchPanel.add(startDateLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        searchPanel.add(startDateField, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0;
        endDateLabel = new JLabel();
        searchPanel.add(endDateLabel, gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        searchPanel.add(endDateField, gbc);
        
        // Second row: Badge code and resource ID
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        badgeCodeLabel = new JLabel();
        searchPanel.add(badgeCodeLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        searchPanel.add(badgeCodeField, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0;
        resourceIdLabel = new JLabel();
        searchPanel.add(resourceIdLabel, gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        searchPanel.add(resourceIdField, gbc);
        
        // Third row: User ID and status
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0;
        userIdLabel = new JLabel();
        searchPanel.add(userIdLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        searchPanel.add(userIdField, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0;
        statusLabel = new JLabel();
        searchPanel.add(statusLabel, gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        searchPanel.add(grantedCombo, gbc);
        
        // Buttons
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchButton = new JButton();
        searchButton.addActionListener(e -> searchLogs());
        clearCriteriaButton = new JButton();
        clearCriteriaButton.addActionListener(e -> clearSearchFields());
        exportButton = new JButton();
        exportButton.addActionListener(e -> exportLogs());
        clearLogsButton = new JButton();
        clearLogsButton.setForeground(Color.RED);
        clearLogsButton.addActionListener(e -> clearLogs());
        buttonPanel.add(searchButton);
        buttonPanel.add(clearCriteriaButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(clearLogsButton);
        searchPanel.add(buttonPanel, gbc);
        
        // Table
        JScrollPane scrollPane = new JScrollPane(logTable);
        recordsBorder = new TitledBorder("");
        scrollPane.setBorder(recordsBorder);
        
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Default load today's logs (silent mode, no prompts)
        SwingUtilities.invokeLater(() -> {
            searchLogsSilent();
        });
    }

    public void applyLanguage() {
        searchBorder.setTitle(I18n.t("log.title.search"));
        recordsBorder.setTitle(I18n.t("log.title.records"));

        startDateLabel.setText(I18n.t("log.field.startDate"));
        endDateLabel.setText(I18n.t("log.field.endDate"));
        badgeCodeLabel.setText(I18n.t("log.field.badgeCode"));
        resourceIdLabel.setText(I18n.t("log.field.resourceId"));
        userIdLabel.setText(I18n.t("log.field.userId"));
        statusLabel.setText(I18n.t("log.field.status"));

        searchButton.setText(I18n.t("log.action.search"));
        clearCriteriaButton.setText(I18n.t("log.action.clearCriteria"));
        exportButton.setText(I18n.t("log.action.export"));
        clearLogsButton.setText(I18n.t("log.action.clearLogs"));

        int sel = grantedCombo.getSelectedIndex();
        grantedCombo.removeAllItems();
        grantedCombo.addItem(I18n.t("log.status.all"));
        grantedCombo.addItem(I18n.t("log.status.granted"));
        grantedCombo.addItem(I18n.t("log.status.denied"));
        grantedCombo.setSelectedIndex(Math.max(0, Math.min(sel, 2)));

        tableModel.setColumnIdentifiers(getColumnNames());
        logTable.getTableHeader().repaint();
        SwingUtilities.invokeLater(this::searchLogsSilent);
        revalidate();
        repaint();
    }

    private String[] getColumnNames() {
        return new String[]{
            I18n.t("log.col.time"),
            I18n.t("log.col.badgeCode"),
            I18n.t("log.col.readerId"),
            I18n.t("log.col.resourceId"),
            I18n.t("log.col.userId"),
            I18n.t("log.col.userName"),
            I18n.t("log.col.status")
        };
    }

    private void styleTable(JTable table) {
        table.setRowHeight(Math.max(table.getRowHeight(), 28));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(true);
        table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
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

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        private final Color granted = new Color(25, 135, 84);
        private final Color denied = new Color(220, 53, 69);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value instanceof String) {
                String v = (String) value;
                if (I18n.t("log.status.granted").equalsIgnoreCase(v)) {
                    c.setForeground(granted);
                } else if (I18n.t("log.status.denied").equalsIgnoreCase(v)) {
                    c.setForeground(denied);
                } else {
                    c.setForeground(table.getForeground());
                }
            } else if (!isSelected) {
                c.setForeground(table.getForeground());
            }
            setHorizontalAlignment(CENTER);
            return c;
        }
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
            
            int statusIndex = grantedCombo.getSelectedIndex();
            if (statusIndex == 1) {
                criteria.setGranted(true);
            } else if (statusIndex == 2) {
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
                    entry.isGranted() ? I18n.t("log.status.granted") : I18n.t("log.status.denied")
                });
            }
            
            // Only show message when manually searching
            if (showMessage) {
                if (results.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        I18n.t("log.msg.noMatches"), 
                        I18n.t("log.msg.searchResultTitle"), 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        I18n.f("log.msg.searchComplete", results.size()), 
                        I18n.t("log.msg.searchCompleteTitle"), 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            
        } catch (DateTimeParseException e) {
            if (showMessage) {
                JOptionPane.showMessageDialog(this, 
                    I18n.t("log.msg.dateFormatError"), 
                    I18n.t("common.error"), 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            if (showMessage) {
                JOptionPane.showMessageDialog(this, 
                    I18n.f("log.msg.searchError", e.getMessage()), 
                    I18n.t("log.msg.searchErrorTitle"), 
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
            
            int statusIndex = grantedCombo.getSelectedIndex();
            if (statusIndex == 1) {
                criteria.setGranted(true);
            } else if (statusIndex == 2) {
                criteria.setGranted(false);
            }
            
            entries = logManager.searchLogs(criteria);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                I18n.f("log.msg.exportDataError", e.getMessage()), 
                I18n.t("common.error"), 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                I18n.t("log.msg.noExportData"), 
                I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18n.t("log.msg.exportTitle"));
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
                    writer.write(I18n.t("log.csv.header") + "\n");
                    
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
                            entry.isGranted() ? I18n.t("log.csv.granted") : I18n.t("log.csv.denied")
                        ));
                    }
                }
                
                JOptionPane.showMessageDialog(this, 
                    I18n.f("log.msg.exportSuccess", entries.size(), file.getAbsolutePath()), 
                    I18n.t("log.msg.exportSuccessTitle"), 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    I18n.f("log.msg.exportError", e.getMessage()), 
                    I18n.t("common.error"), 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    
    /**
     * Clear log files
     */
    private void clearLogs() {
        int confirm = JOptionPane.showConfirmDialog(this,
            I18n.t("log.msg.clearLogs.confirmInitial"),
            I18n.t("log.msg.clearLogs.confirmInitialTitle"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        try {
            java.nio.file.Path logsDir = java.nio.file.Paths.get("data/logs");
            if (!java.nio.file.Files.exists(logsDir)) {
                JOptionPane.showMessageDialog(this, 
                    I18n.t("log.msg.noLogsDir"), I18n.t("common.info"), 
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
                    I18n.t("log.msg.noLogs"), I18n.t("common.info"), 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Confirm again
            long totalSizeValue = totalSize[0];
            String sizeStr = totalSizeValue > 1024 * 1024 ? 
                String.format("%.2f %s", totalSizeValue / (1024.0 * 1024.0), I18n.t("common.unit.mb")) :
                String.format("%.2f %s", totalSizeValue / 1024.0, I18n.t("common.unit.kb"));
            
            int finalConfirm = JOptionPane.showConfirmDialog(this,
                I18n.f("log.msg.clearLogs.confirm", fileCount, sizeStr),
                I18n.t("log.msg.clearLogs.confirmTitle"),
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
            String message = I18n.f("log.msg.clearLogs.done", deletedCount, errorCount);
            
            JOptionPane.showMessageDialog(this, message, 
                I18n.t("log.msg.clearLogs.doneTitle"), 
                deletedCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                I18n.f("log.msg.clearLogs.error", e.getMessage()), 
                I18n.t("common.error"), 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
