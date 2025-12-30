// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.simulation.EventSimulator;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.util.SystemClock;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Event Simulation Panel
 */
public class EventSimulationPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private EventSimulator simulator;
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JTable readerTable;
    private DefaultTableModel readerTableModel;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea statusArea;
    private JSpinner intervalSpinner;
    private JLabel statsLabel;
    private JLabel timeLabel;
    private JButton setTimeButton;
    private JButton resetTimeButton;
    private Map<String, User> simulatedUsers;
    private Map<String, Badge> userBadges;
    private int totalEvents = 0;
    private int grantedEvents = 0;
    private int deniedEvents = 0;
    
    public EventSimulationPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.simulatedUsers = new HashMap<>();
        this.userBadges = new HashMap<>();
        initializeComponents();
        setupLayout();
        loadData();
    }
    
    private void initializeComponents() {
        // User table
        String[] userColumns = {"User ID", "Name", "Type", "Badge Code"};
        userTableModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Badge reader table (status column editable, add selection column)
        String[] readerColumns = {"Participate", "Badge Reader ID", "Resource ID", "Resource Name", "Status"};
        readerTableModel = new DefaultTableModel(readerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 4; // Participate column and status column are editable
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0 || column == 4) {
                    return Boolean.class; // Checkbox column
                }
                return String.class;
            }
        };
        readerTable = new JTable(readerTableModel);
        readerTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Set column widths
        readerTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        readerTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        readerTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        readerTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        readerTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        
        // Add checkbox renderer and editor for status column
        readerTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JCheckBox checkBox = new JCheckBox();
                if (value instanceof String) {
                    checkBox.setSelected("Active".equals(value));
                } else if (value instanceof Boolean) {
                    checkBox.setSelected((Boolean) value);
                }
                checkBox.setHorizontalAlignment(JCheckBox.CENTER);
                if (isSelected) {
                    checkBox.setBackground(table.getSelectionBackground());
                } else {
                    checkBox.setBackground(table.getBackground());
                }
                return checkBox;
            }
        });
        
        readerTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            @Override
            public Object getCellEditorValue() {
                JCheckBox checkBox = (JCheckBox) getComponent();
                return checkBox.isSelected() ? "Active" : "Inactive";
            }
        });
        
        // Add checkbox renderer and editor for "Participate" column
        readerTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JCheckBox checkBox = new JCheckBox();
                if (value instanceof Boolean) {
                    checkBox.setSelected((Boolean) value);
                }
                checkBox.setHorizontalAlignment(JCheckBox.CENTER);
                if (isSelected) {
                    checkBox.setBackground(table.getSelectionBackground());
                } else {
                    checkBox.setBackground(table.getBackground());
                }
                return checkBox;
            }
        });
        
        readerTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        
        // Add checkbox renderer and editor for status column
        readerTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JCheckBox checkBox = new JCheckBox();
                if (value instanceof Boolean) {
                    checkBox.setSelected((Boolean) value);
                } else if (value instanceof String) {
                    checkBox.setSelected("Active".equals(value));
                }
                checkBox.setHorizontalAlignment(JCheckBox.CENTER);
                if (isSelected) {
                    checkBox.setBackground(table.getSelectionBackground());
                } else {
                    checkBox.setBackground(table.getBackground());
                }
                return checkBox;
            }
        });
        
        readerTable.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            @Override
            public Object getCellEditorValue() {
                JCheckBox checkBox = (JCheckBox) getComponent();
                return checkBox.isSelected();
            }
        });
        
        // Add table change listener
        readerTable.getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            
            if (column == 0) {
                // Participate column changed
                String readerId = (String) readerTableModel.getValueAt(row, 1);
                Boolean selected = (Boolean) readerTableModel.getValueAt(row, 0);
                statusArea.append("Badge reader " + readerId + " " + (selected ? "added to" : "removed from") + " simulation\n");
            } else if (column == 4) {
                // Status column changed
                String readerId = (String) readerTableModel.getValueAt(row, 1);
                Object statusValue = readerTableModel.getValueAt(row, 4);
                boolean active = statusValue instanceof Boolean ? (Boolean) statusValue : 
                                "Active".equals(statusValue);
                
                updateReaderStatus(readerId, active);
            }
        });
        
        // Control buttons
        startButton = new JButton("Start Simulation");
        startButton.addActionListener(e -> startSimulation());
        
        stopButton = new JButton("Stop Simulation");
        stopButton.addActionListener(e -> stopSimulation());
        stopButton.setEnabled(false);
        
        // Status area
        statusArea = new JTextArea(5, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Left: User list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Simulated User List"));
        leftPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        JPanel leftButtonPanel = new JPanel(new FlowLayout());
        leftButtonPanel.add(new JButton("Select All") {{
            addActionListener(e -> selectAllUsers());
        }});
        leftButtonPanel.add(new JButton("Deselect All") {{
            addActionListener(e -> deselectAllUsers());
        }});
        leftButtonPanel.add(new JButton("Add User") {{
            addActionListener(e -> addSimulatedUser());
        }});
        leftButtonPanel.add(new JButton("Add All Users") {{
            addActionListener(e -> addAllUsers());
        }});
        leftButtonPanel.add(new JButton("Remove User") {{
            addActionListener(e -> removeSimulatedUser());
        }});
        leftButtonPanel.add(new JButton("Clear All Users") {{
            addActionListener(e -> clearAllSimulatedUsers());
        }});
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // Center: Badge reader list
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Badge Reader List (Available for Simulation)"));
        centerPanel.add(new JScrollPane(readerTable), BorderLayout.CENTER);
        
        // Add control buttons and info below badge reader list
        JPanel readerControlPanel = new JPanel(new BorderLayout());
        
        JPanel readerButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        readerButtonPanel.add(new JButton("Select All") {{
            addActionListener(e -> selectAllReaders());
        }});
        readerButtonPanel.add(new JButton("Deselect All") {{
            addActionListener(e -> deselectAllReaders());
        }});
        readerButtonPanel.add(new JButton("Select All Readers") {{
            addActionListener(e -> setAllReadersSelected(true));
        }});
        readerButtonPanel.add(new JButton("Deselect All Readers") {{
            addActionListener(e -> setAllReadersSelected(false));
        }});
        readerButtonPanel.add(new JButton("Enable All") {{
            addActionListener(e -> setAllReadersStatus(true));
        }});
        readerButtonPanel.add(new JButton("Disable All") {{
            addActionListener(e -> setAllReadersStatus(false));
        }});
        readerButtonPanel.add(new JButton("Refresh List") {{
            addActionListener(e -> loadReaders());
        }});
        readerControlPanel.add(readerButtonPanel, BorderLayout.NORTH);
        
        JPanel readerInfoPanel = new JPanel(new BorderLayout());
        JTextArea infoText = new JTextArea(
            "Instructions:\n" +
            "• 'Participate' column: Check to include this badge reader in event simulation\n" +
            "• 'Status' column: Enable/disable badge reader, disabled readers won't respond to any badge operations\n" +
            "• Only badge readers with both 'Participate' checked and 'Status' enabled will generate events in simulation"
        );
        infoText.setEditable(false);
        infoText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        infoText.setBackground(readerControlPanel.getBackground());
        infoText.setForeground(Color.DARK_GRAY);
        readerInfoPanel.add(infoText, BorderLayout.CENTER);
        readerControlPanel.add(readerInfoPanel, BorderLayout.SOUTH);
        
        centerPanel.add(readerControlPanel, BorderLayout.SOUTH);
        
        // Right: Control panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Simulation Control"));
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        controlPanel.add(startButton, gbc);
        
        gbc.gridy = 1;
        controlPanel.add(stopButton, gbc);
        
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Event Interval (seconds):"), gbc);
        gbc.gridx = 1;
        intervalSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 60, 1));
        controlPanel.add(intervalSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        statsLabel = new JLabel("Statistics: Total Events: 0 | Granted: 0 | Denied: 0");
        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        controlPanel.add(statsLabel, gbc);
        
        // System time control
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("System Time:"), gbc);
        gbc.gridx = 1;
        timeLabel = new JLabel(getCurrentTimeString());
        timeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        controlPanel.add(timeLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 1;
        setTimeButton = new JButton("Set Time");
        setTimeButton.addActionListener(e -> setSystemTime());
        controlPanel.add(setTimeButton, gbc);
        
        gbc.gridx = 1;
        resetTimeButton = new JButton("Reset Time");
        resetTimeButton.addActionListener(e -> resetSystemTime());
        resetTimeButton.setEnabled(SystemClock.isUsingCustomTime());
        controlPanel.add(resetTimeButton, gbc);
        
        // Update time display
        javax.swing.Timer timeUpdateTimer = new javax.swing.Timer(1000, e -> updateTimeDisplay());
        timeUpdateTimer.start();
        
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        controlPanel.add(new JButton("Reset Statistics") {{
            addActionListener(e -> resetStatistics());
        }}, gbc);
        
        gbc.gridy = 7;
        controlPanel.add(new JButton("Refresh Data") {{
            addActionListener(e -> {
                loadData();
                JOptionPane.showMessageDialog(EventSimulationPanel.this, 
                    "Data refreshed", "Info", JOptionPane.INFORMATION_MESSAGE);
            });
        }}, gbc);
        
        rightPanel.add(controlPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        
        // Main layout
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        leftSplit.setDividerLocation(300);
        leftSplit.setResizeWeight(0.3);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setDividerLocation(700);
        mainSplit.setResizeWeight(0.7);
        
        add(mainSplit, BorderLayout.CENTER);
    }
    
    private void loadData() {
        loadUsers();
        loadReaders();
    }
    
    private void loadUsers() {
        userTableModel.setRowCount(0);
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        // Use loadAllUsers() to load all users, including those without badges
        Map<String, User> users = dbManager.loadAllUsers();
        // Load all badges
        Map<String, Badge> allBadges = dbManager.loadAllBadges();
        
        for (User user : users.values()) {
            // Load badge from database if user has badge ID
            Badge badge = null;
            if (user.getBadgeId() != null) {
                badge = dbManager.loadBadgeById(user.getBadgeId());
            } else if (allBadges.containsKey(user.getId())) {
                badge = allBadges.get(user.getId());
            }
            
            // If badge found, save to userBadges
            if (badge != null) {
                userBadges.put(user.getId(), badge);
            }
            
            String badgeCode = badge != null ? badge.getCode() : "None";
            userTableModel.addRow(new Object[]{
                user.getId(),
                user.getFullName(),
                user.getUserType().toString(),
                badgeCode
            });
        }
    }
    
    private void loadReaders() {
        readerTableModel.setRowCount(0);
        var router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        
        if (readers.isEmpty()) {
            // If no badge readers, show prompt
            readerTableModel.addRow(new Object[]{
                false, "None", "None", "No available badge readers", false
            });
            statusArea.append("Note: No available badge readers, please create resources and badge readers in Resource Management first\n");
            return;
        }
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> resources = dbManager.loadAllResources();
        
        for (BadgeReader reader : readers.values()) {
            String resourceId = reader.getResourceId();
            Resource resource = resources.get(resourceId);
            String resourceName = resource != null ? resource.getName() : "Unknown";
            String resourceType = resource != null ? resource.getType().toString() : "Unknown";
            Boolean status = reader.isActive(); // Use Boolean type for checkbox display
            Boolean selected = true; // Default selected to participate in simulation
            
            readerTableModel.addRow(new Object[]{
                selected,  // Participate in simulation
                reader.getId(),
                resourceId,
                resourceName + " (" + resourceType + ")",
                status  // Enable status
            });
        }
        
        // Display badge reader count in status area
        statusArea.append("Loaded " + readers.size() + " badge readers (all participate in simulation by default)\n");
    }
    
    private void addSimulatedUser() {
        int[] selectedRows = userTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select users to add", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, User> allUsers = dbManager.loadAllUsers();
        
        for (int row : selectedRows) {
            String userId = (String) userTableModel.getValueAt(row, 0);
            User user = allUsers.get(userId);
            
            if (user != null && !simulatedUsers.containsKey(userId)) {
                // Load badge from database if user has badge
                Badge badge = userBadges.get(userId);
                if (badge == null) {
                    if (user.getBadgeId() != null) {
                        badge = dbManager.loadBadgeById(user.getBadgeId());
                    } else {
                        // Try to load by user ID
                        badge = dbManager.loadBadgeByUserId(userId);
                    }
                    
                    // If still not found, create a new badge (but won't save to database)
                    if (badge == null) {
                        JOptionPane.showMessageDialog(this, 
                            "User \"" + user.getFullName() + "\" has no badge, cannot add to simulation. Please create a badge for the user first.", 
                            "Warning", 
                            JOptionPane.WARNING_MESSAGE);
                        continue;
                    }
                    
                    userBadges.put(userId, badge);
                }
                
                simulatedUsers.put(userId, user);
                statusArea.append("Added simulated user: " + user.getFullName() + " (Badge: " + badge.getCode() + ")\n");
            } else if (simulatedUsers.containsKey(userId)) {
                statusArea.append("User \"" + (user != null ? user.getFullName() : userId) + "\" is already in simulation list\n");
            }
        }
    }
    
    private void removeSimulatedUser() {
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No simulated users to remove", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Show selection dialog
        String[] userNames = simulatedUsers.values().stream()
            .map(User::getFullName)
            .toArray(String[]::new);
        
        String selected = (String) JOptionPane.showInputDialog(this,
            "Select simulated user to remove:",
            "Remove Simulated User",
            JOptionPane.QUESTION_MESSAGE,
            null,
            userNames,
            userNames[0]);
        
        if (selected != null) {
            // Find and remove selected user
            String userIdToRemove = null;
            for (Map.Entry<String, User> entry : simulatedUsers.entrySet()) {
                if (entry.getValue().getFullName().equals(selected)) {
                    userIdToRemove = entry.getKey();
                    break;
                }
            }
            
            if (userIdToRemove != null) {
                simulatedUsers.remove(userIdToRemove);
                userBadges.remove(userIdToRemove);
                statusArea.append("Removed simulated user: " + selected + "\n");
                
                // If simulator is running, need to update simulator
                if (simulator != null && simulator.isRunning()) {
                    // Recreate simulator to update user list
                    var router = accessControlSystem.getRouter();
                    Map<String, BadgeReader> readers = router.getBadgeReaders();
                    List<BadgeReader> readerList = new ArrayList<>(readers.values());
                    
                    simulator.stop();
                    simulator = new EventSimulator(readerList);
                    
                    for (User user : simulatedUsers.values()) {
                        Badge badge = userBadges.get(user.getId());
                        if (badge != null) {
                            simulator.addSimulatedUser(user, badge);
                        }
                    }
                    
                    simulator.start();
                    statusArea.append("Simulator updated\n");
                }
            }
        }
    }
    
    private void startSimulation() {
        if (simulator != null && simulator.isRunning()) {
            JOptionPane.showMessageDialog(this, "Simulation is already running", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please add simulated users first", "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        var router = accessControlSystem.getRouter();
        Map<String, BadgeReader> allReaders = router.getBadgeReaders();
        
        // Only select badge readers that are checked "Participate" and status is enabled
        List<BadgeReader> readerList = new ArrayList<>();
        int disabledCount = 0;
        
        for (int i = 0; i < readerTableModel.getRowCount(); i++) {
            Boolean selected = (Boolean) readerTableModel.getValueAt(i, 0);
            String readerId = (String) readerTableModel.getValueAt(i, 1);
            Boolean active = (Boolean) readerTableModel.getValueAt(i, 4);
            
            if (selected != null && selected && active != null && active) {
                BadgeReader reader = allReaders.get(readerId);
                if (reader != null && reader.isActive()) {
                    readerList.add(reader);
                }
            } else if (selected != null && selected) {
                disabledCount++;
            }
        }
        
        if (readerList.isEmpty()) {
            String message = "No available badge readers to participate in simulation.\n\n";
            if (disabledCount > 0) {
                message += "Note: " + disabledCount + " badge readers are checked but not enabled.\n";
                message += "Please enable these badge readers first, or check other enabled badge readers.";
            } else {
                message += "Please:\n";
                message += "1. Check badge readers in the 'Participate' column\n";
                message += "2. Ensure these badge readers' 'Status' is enabled";
            }
            JOptionPane.showMessageDialog(this, message, "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get event interval
        int interval = (Integer) intervalSpinner.getValue();
        
        // Create simulator (only use selected badge readers)
        simulator = new EventSimulator(readerList);
        simulator.setInterval(interval);
        
        // Add simulated users
        for (User user : simulatedUsers.values()) {
            Badge badge = userBadges.get(user.getId());
            if (badge != null) {
                simulator.addSimulatedUser(user, badge);
            }
        }
        
        // Start simulation
        simulator.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        intervalSpinner.setEnabled(false);
        statusArea.append("Simulation started (interval: " + interval + " seconds, using " + readerList.size() + " badge readers)\n");
        
        // Start statistics update thread
        startStatisticsUpdate();
    }
    
    private void stopSimulation() {
        if (simulator != null) {
            simulator.stop();
            simulator = null;
            startButton.setEnabled(true);
            stopButton.setEnabled(true);
            intervalSpinner.setEnabled(true);
            statusArea.append("Simulation stopped\n");
        }
    }
    
    private void resetStatistics() {
        totalEvents = 0;
        grantedEvents = 0;
        deniedEvents = 0;
        updateStatistics();
        statusArea.append("Statistics reset\n");
    }
    
    private void updateStatistics() {
        statsLabel.setText(String.format("Statistics: Total Events: %d | Granted: %d | Denied: %d", 
            totalEvents, grantedEvents, deniedEvents));
    }
    
    private void startStatisticsUpdate() {
        // Create a thread to periodically update statistics
        Thread statsThread = new Thread(() -> {
            while (simulator != null && simulator.isRunning()) {
                try {
                    // Read statistics from logs (simplified implementation)
                    // Should actually get from LogManager or AccessControlSystem
                    Thread.sleep(2000); // Update every 2 seconds
                    SwingUtilities.invokeLater(() -> updateStatistics());
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
    }
    
    /**
     * Set system time (for testing)
     */
    private void setSystemTime() {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
            "Set System Time", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        LocalDateTime currentTime = SystemClock.now();
        
        // Year
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getYear(), 2020, 2030, 1));
        panel.add(yearSpinner, gbc);
        
        // Month
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Month:"), gbc);
        gbc.gridx = 1;
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getMonthValue(), 1, 12, 1));
        panel.add(monthSpinner, gbc);
        
        // Day
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Day:"), gbc);
        gbc.gridx = 1;
        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(currentTime.getDayOfMonth(), 1, 31, 1));
        panel.add(daySpinner, gbc);
        
        // Hour
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Hour:"), gbc);
        gbc.gridx = 1;
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getHour(), 0, 23, 1));
        panel.add(hourSpinner, gbc);
        
        // Minute
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Minute:"), gbc);
        gbc.gridx = 1;
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getMinute(), 0, 59, 1));
        panel.add(minuteSpinner, gbc);
        
        // Preset time buttons
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel presetPanel = new JPanel(new FlowLayout());
        presetPanel.add(new JButton("Weekday 8:00") {{
            addActionListener(e -> {
                LocalDateTime preset = LocalDateTime.now()
                    .withHour(8).withMinute(0).withSecond(0);
                yearSpinner.setValue(preset.getYear());
                monthSpinner.setValue(preset.getMonthValue());
                daySpinner.setValue(preset.getDayOfMonth());
                hourSpinner.setValue(8);
                minuteSpinner.setValue(0);
            });
        }});
        presetPanel.add(new JButton("Weekend 10:00") {{
            addActionListener(e -> {
                LocalDateTime preset = LocalDateTime.now()
                    .withHour(10).withMinute(0).withSecond(0);
                // Set to nearest Saturday
                int daysUntilSaturday = (java.time.DayOfWeek.SATURDAY.getValue() - 
                    preset.getDayOfWeek().getValue() + 7) % 7;
                if (daysUntilSaturday == 0) daysUntilSaturday = 7;
                preset = preset.plusDays(daysUntilSaturday);
                yearSpinner.setValue(preset.getYear());
                monthSpinner.setValue(preset.getMonthValue());
                daySpinner.setValue(preset.getDayOfMonth());
                hourSpinner.setValue(10);
                minuteSpinner.setValue(0);
            });
        }});
        panel.add(presetPanel, gbc);
        
        // Buttons
        gbc.gridy = 6;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("OK") {{
            addActionListener(e -> {
                try {
                    int year = (Integer) yearSpinner.getValue();
                    int month = (Integer) monthSpinner.getValue();
                    int day = (Integer) daySpinner.getValue();
                    int hour = (Integer) hourSpinner.getValue();
                    int minute = (Integer) minuteSpinner.getValue();
                    
                    LocalDateTime customTime = LocalDateTime.of(year, month, day, hour, minute, 0);
                    SystemClock.setCustomTime(customTime);
                    updateTimeDisplay();
                    resetTimeButton.setEnabled(true);
                    statusArea.append("System time set to: " + customTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n");
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Failed to set time: " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }});
        buttonPanel.add(new JButton("Cancel") {{
            addActionListener(e -> dialog.dispose());
        }});
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * Reset system time
     */
    private void resetSystemTime() {
        SystemClock.clearCustomTime();
        updateTimeDisplay();
        resetTimeButton.setEnabled(false);
        statusArea.append("System time reset to current time\n");
    }
    
    /**
     * Update time display
     */
    private void updateTimeDisplay() {
        if (timeLabel != null) {
            LocalDateTime now = SystemClock.now();
            String timeStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (SystemClock.isUsingCustomTime()) {
                timeStr += " (Custom)";
                timeLabel.setForeground(Color.RED);
            } else {
                timeLabel.setForeground(Color.BLACK);
            }
            timeLabel.setText(timeStr);
        }
    }
    
    /**
     * Get current time string
     */
    private String getCurrentTimeString() {
        LocalDateTime now = SystemClock.now();
        String timeStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (SystemClock.isUsingCustomTime()) {
            timeStr += " (Custom)";
        }
        return timeStr;
    }
    
    /**
     * Update badge reader status
     */
    private void updateReaderStatus(String readerId, boolean active) {
        var router = accessControlSystem.getRouter();
        BadgeReader reader = router.getBadgeReaders().get(readerId);
        if (reader != null) {
            reader.setActive(active);
            statusArea.append("Badge reader " + readerId + " " + (active ? "enabled" : "disabled") + "\n");
            
            // Refresh table display
            for (int i = 0; i < readerTableModel.getRowCount(); i++) {
                if (readerId.equals(readerTableModel.getValueAt(i, 1))) {
                    readerTableModel.setValueAt(active, i, 4);
                    break;
                }
            }
        }
    }
    
    /**
     * Set all badge reader status
     */
    private void setAllReadersStatus(boolean active) {
        var router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        
        int count = 0;
        for (BadgeReader reader : readers.values()) {
            reader.setActive(active);
            count++;
            
            // Update table display (status column is at column 4, index 4)
            for (int i = 0; i < readerTableModel.getRowCount(); i++) {
                if (reader.getId().equals(readerTableModel.getValueAt(i, 1))) {
                    readerTableModel.setValueAt(active, i, 4);
                    break;
                }
            }
        }
        
        statusArea.append((active ? "Enabled" : "Disabled") + " " + count + " badge readers\n");
        readerTable.repaint();
    }
    
    /**
     * Select all rows in the badge reader table
     */
    private void selectAllReaders() {
        readerTable.selectAll();
        int selectedCount = readerTable.getSelectedRowCount();
        statusArea.append("Selected " + selectedCount + " badge readers in the table\n");
    }
    
    /**
     * Deselect all rows in the badge reader table
     */
    private void deselectAllReaders() {
        readerTable.clearSelection();
        statusArea.append("Deselected all badge readers in the table\n");
    }
    
    /**
     * Set all badge readers' participation status
     */
    private void setAllReadersSelected(boolean selected) {
        int count = 0;
        
        // Temporarily remove table model listener to avoid multiple status messages
        javax.swing.event.TableModelListener[] listeners = readerTableModel.getListeners(javax.swing.event.TableModelListener.class);
        for (javax.swing.event.TableModelListener listener : listeners) {
            readerTableModel.removeTableModelListener(listener);
        }
        
        try {
            for (int i = 0; i < readerTableModel.getRowCount(); i++) {
                String readerId = (String) readerTableModel.getValueAt(i, 1);
                if (readerId != null && !"None".equals(readerId)) {
                    // Set the value directly
                    readerTableModel.setValueAt(Boolean.valueOf(selected), i, 0);
                    count++;
                }
            }
        } finally {
            // Restore table model listeners
            for (javax.swing.event.TableModelListener listener : listeners) {
                readerTableModel.addTableModelListener(listener);
            }
        }
        
        // Force table repaint to ensure visual update
        readerTable.repaint();
        readerTable.revalidate();
        
        statusArea.append((selected ? "Selected" : "Deselected") + " " + count + " badge readers for simulation\n");
    }
    
    /**
     * Select all users in the user table
     */
    private void selectAllUsers() {
        userTable.selectAll();
        int selectedCount = userTable.getSelectedRowCount();
        statusArea.append("Selected " + selectedCount + " users in the table\n");
    }
    
    /**
     * Deselect all users in the user table
     */
    private void deselectAllUsers() {
        userTable.clearSelection();
        statusArea.append("Deselected all users in the table\n");
    }
    
    /**
     * Add all users to simulation list
     */
    private void addAllUsers() {
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, User> allUsers = dbManager.loadAllUsers();
        
        if (allUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No available users", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int addedCount = 0;
        int skippedCount = 0;
        int noBadgeCount = 0;
        
        for (User user : allUsers.values()) {
            // Skip users already in simulation list
            if (simulatedUsers.containsKey(user.getId())) {
                skippedCount++;
                continue;
            }
            
            // Check if user has badge
            Badge badge = userBadges.get(user.getId());
            if (badge == null) {
                if (user.getBadgeId() != null) {
                    badge = dbManager.loadBadgeById(user.getBadgeId());
                } else {
                    badge = dbManager.loadBadgeByUserId(user.getId());
                }
            }
            
            if (badge == null) {
                noBadgeCount++;
                continue;
            }
            
            // Add to simulation list
            simulatedUsers.put(user.getId(), user);
            userBadges.put(user.getId(), badge);
            addedCount++;
            statusArea.append("Added simulated user: " + user.getFullName() + " (Badge: " + badge.getCode() + ")\n");
        }
        
        // If simulator is running, need to update simulator
        if (simulator != null && simulator.isRunning()) {
            var router = accessControlSystem.getRouter();
            Map<String, BadgeReader> readers = router.getBadgeReaders();
            List<BadgeReader> readerList = new ArrayList<>(readers.values());
            
            simulator.stop();
            simulator = new EventSimulator(readerList);
            
            for (User user : simulatedUsers.values()) {
                Badge badge = userBadges.get(user.getId());
                if (badge != null) {
                    simulator.addSimulatedUser(user, badge);
                }
            }
            
            simulator.start();
            statusArea.append("Simulator updated\n");
        }
        
        String message = String.format(
            "Batch add users completed!\n\n" +
            "Statistics:\n" +
            "• Successfully added: %d users\n" +
            "• Skipped (already in list): %d users\n" +
            "• Skipped (no badge): %d users\n\n" +
            "Current total simulated users: %d",
            addedCount, skippedCount, noBadgeCount, simulatedUsers.size()
        );
        
        JOptionPane.showMessageDialog(this, message, "Batch Add Complete", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Clear all simulated users
     */
    private void clearAllSimulatedUsers() {
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Simulated user list is already empty", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int count = simulatedUsers.size();
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear all simulated users?\n\n" +
            "Currently " + count + " users in simulation list\n" +
            "If simulation is running, it will be stopped.",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // If simulation is running, stop first
            if (simulator != null && simulator.isRunning()) {
                simulator.stop();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                intervalSpinner.setEnabled(true);
                statusArea.append("Simulation stopped\n");
            }
            
            simulatedUsers.clear();
            userBadges.clear();
            statusArea.append("Cleared all simulated users (total: " + count + ")\n");
            
            JOptionPane.showMessageDialog(this, 
                "All simulated users cleared", "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
