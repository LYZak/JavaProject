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
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
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
    private TitledBorder usersBorder;
    private TitledBorder readersBorder;
    private TitledBorder controlBorder;
    private JButton selectAllUsersButton;
    private JButton deselectAllUsersButton;
    private JButton addUserButton;
    private JButton addAllUsersButton;
    private JButton removeUserButton;
    private JButton clearUsersButton;
    private JButton selectAllReadersButton;
    private JButton deselectAllReadersButton;
    private JButton participateAllButton;
    private JButton participateNoneButton;
    private JButton enableAllButton;
    private JButton disableAllButton;
    private JButton refreshReadersButton;
    private JButton resetStatsButton;
    private JButton refreshDataButton;
    private JLabel intervalLabel;
    private JLabel systemTimeLabel;
    private JTextArea infoText;
    
    public EventSimulationPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.simulatedUsers = new HashMap<>();
        this.userBadges = new HashMap<>();
        initializeComponents();
        setupLayout();
        applyLanguage();
        loadData();
    }
    
    private void initializeComponents() {
        // User table
        String[] userColumns = getUserColumnNames();
        userTableModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userTable.setAutoCreateRowSorter(true);
        userTable.setFillsViewportHeight(true);
        styleSimpleTable(userTable);
        
        // Badge reader table (status column editable, add selection column)
        String[] readerColumns = getReaderColumnNames();
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
        readerTable.setFillsViewportHeight(true);
        readerTable.setShowGrid(false);
        readerTable.setIntercellSpacing(new Dimension(0, 0));
        
        // Set column widths
        readerTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        readerTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        readerTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        readerTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        readerTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        
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
                    checkBox.setSelected(I18n.t("common.active").equals(value));
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
                statusArea.append(selected ? I18n.f("sim.msg.readerAddedToSim", readerId) : I18n.f("sim.msg.readerRemovedFromSim", readerId));
            } else if (column == 4) {
                // Status column changed
                String readerId = (String) readerTableModel.getValueAt(row, 1);
                Object statusValue = readerTableModel.getValueAt(row, 4);
                boolean active = statusValue instanceof Boolean ? (Boolean) statusValue : 
                                I18n.t("common.active").equals(statusValue);
                
                updateReaderStatus(readerId, active);
            }
        });
        
        // Control buttons
        startButton = new JButton();
        startButton.addActionListener(e -> startSimulation());
        
        stopButton = new JButton();
        stopButton.addActionListener(e -> stopSimulation());
        stopButton.setEnabled(false);
        
        // Status area
        statusArea = new JTextArea(5, 40);
        statusArea.setEditable(false);
        Font textAreaFont = UIManager.getFont("TextArea.font");
        if (textAreaFont != null) {
            statusArea.setFont(textAreaFont);
        }
        statusArea.setMargin(new Insets(8, 8, 8, 8));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        
        // Left: User list
        JPanel leftPanel = new JPanel(new BorderLayout());
        usersBorder = new TitledBorder("");
        leftPanel.setBorder(usersBorder);
        leftPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        
        JPanel leftButtonPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        leftButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        JPanel userRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectAllUsersButton = new JButton();
        selectAllUsersButton.addActionListener(e -> selectAllUsers());
        deselectAllUsersButton = new JButton();
        deselectAllUsersButton.addActionListener(e -> deselectAllUsers());
        addUserButton = new JButton();
        addUserButton.addActionListener(e -> addSimulatedUser());
        addAllUsersButton = new JButton();
        addAllUsersButton.addActionListener(e -> addAllUsers());
        userRow1.add(selectAllUsersButton);
        userRow1.add(deselectAllUsersButton);
        userRow1.add(addUserButton);
        userRow1.add(addAllUsersButton);
        
        JPanel userRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        removeUserButton = new JButton();
        removeUserButton.addActionListener(e -> removeSimulatedUser());
        clearUsersButton = new JButton();
        clearUsersButton.setForeground(Color.RED);
        clearUsersButton.addActionListener(e -> clearAllSimulatedUsers());
        userRow2.add(removeUserButton);
        userRow2.add(clearUsersButton);
        
        leftButtonPanel.add(userRow1);
        leftButtonPanel.add(userRow2);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // Center: Badge reader list
        JPanel centerPanel = new JPanel(new BorderLayout());
        readersBorder = new TitledBorder("");
        centerPanel.setBorder(readersBorder);
        centerPanel.add(new JScrollPane(readerTable), BorderLayout.CENTER);
        
        // Add control buttons and info below badge reader list
        JPanel readerControlPanel = new JPanel(new BorderLayout());
        
        JPanel readerButtonPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        readerButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        JPanel readerRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectAllReadersButton = new JButton();
        selectAllReadersButton.addActionListener(e -> selectAllReaders());
        deselectAllReadersButton = new JButton();
        deselectAllReadersButton.addActionListener(e -> deselectAllReaders());
        participateAllButton = new JButton();
        participateAllButton.addActionListener(e -> setAllReadersSelected(true));
        participateNoneButton = new JButton();
        participateNoneButton.addActionListener(e -> setAllReadersSelected(false));
        readerRow1.add(selectAllReadersButton);
        readerRow1.add(deselectAllReadersButton);
        readerRow1.add(participateAllButton);
        readerRow1.add(participateNoneButton);
        
        JPanel readerRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        enableAllButton = new JButton();
        enableAllButton.addActionListener(e -> setAllReadersStatus(true));
        disableAllButton = new JButton();
        disableAllButton.addActionListener(e -> setAllReadersStatus(false));
        refreshReadersButton = new JButton();
        refreshReadersButton.addActionListener(e -> loadReaders());
        readerRow2.add(enableAllButton);
        readerRow2.add(disableAllButton);
        readerRow2.add(refreshReadersButton);
        
        readerButtonPanel.add(readerRow1);
        readerButtonPanel.add(readerRow2);
        readerControlPanel.add(readerButtonPanel, BorderLayout.NORTH);
        
        JPanel readerInfoPanel = new JPanel(new BorderLayout());
        infoText = new JTextArea();
        infoText.setEditable(false);
        infoText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        infoText.setBackground(readerControlPanel.getBackground());
        infoText.setForeground(Color.DARK_GRAY);
        infoText.setBorder(new EmptyBorder(8, 8, 8, 8));
        readerInfoPanel.add(infoText, BorderLayout.CENTER);
        readerControlPanel.add(readerInfoPanel, BorderLayout.SOUTH);
        
        centerPanel.add(readerControlPanel, BorderLayout.SOUTH);
        
        // Right: Control panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        controlBorder = new TitledBorder("");
        rightPanel.setBorder(controlBorder);
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        controlPanel.add(startButton, gbc);
        
        gbc.gridy = 1;
        controlPanel.add(stopButton, gbc);
        
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        intervalLabel = new JLabel();
        controlPanel.add(intervalLabel, gbc);
        gbc.gridx = 1;
        intervalSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 60, 1));
        controlPanel.add(intervalSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        statsLabel = new JLabel();
        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        controlPanel.add(statsLabel, gbc);
        
        updateStatistics();
        
        // System time control
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        systemTimeLabel = new JLabel();
        controlPanel.add(systemTimeLabel, gbc);
        gbc.gridx = 1;
        timeLabel = new JLabel(getCurrentTimeString());
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null) {
            timeLabel.setFont(labelFont);
        }
        controlPanel.add(timeLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 1;
        setTimeButton = new JButton();
        setTimeButton.addActionListener(e -> setSystemTime());
        controlPanel.add(setTimeButton, gbc);
        
        gbc.gridx = 1;
        resetTimeButton = new JButton();
        resetTimeButton.addActionListener(e -> resetSystemTime());
        resetTimeButton.setEnabled(SystemClock.isUsingCustomTime());
        controlPanel.add(resetTimeButton, gbc);
        
        // Update time display
        javax.swing.Timer timeUpdateTimer = new javax.swing.Timer(1000, e -> updateTimeDisplay());
        timeUpdateTimer.start();
        
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        resetStatsButton = new JButton();
        resetStatsButton.addActionListener(e -> resetStatistics());
        controlPanel.add(resetStatsButton, gbc);
        
        gbc.gridy = 7;
        refreshDataButton = new JButton();
        refreshDataButton.addActionListener(e -> {
                loadData();
                JOptionPane.showMessageDialog(EventSimulationPanel.this, 
                    I18n.t("sim.dialog.dataRefreshed"), I18n.t("common.info"), JOptionPane.INFORMATION_MESSAGE);
        });
        controlPanel.add(refreshDataButton, gbc);
        
        rightPanel.add(controlPanel, BorderLayout.NORTH);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(new EmptyBorder(8, 8, 8, 8));
        rightPanel.add(statusScroll, BorderLayout.CENTER);
        
        // Main layout
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        leftSplit.setResizeWeight(0.35);
        leftSplit.setBorder(null);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setResizeWeight(0.65);
        mainSplit.setBorder(null);
        
        add(mainSplit, BorderLayout.CENTER);
    }

    public void applyLanguage() {
        usersBorder.setTitle(I18n.t("sim.title.users"));
        readersBorder.setTitle(I18n.t("sim.title.readers"));
        controlBorder.setTitle(I18n.t("sim.title.control"));

        selectAllUsersButton.setText(I18n.t("common.selectAll"));
        deselectAllUsersButton.setText(I18n.t("common.deselectAll"));
        addUserButton.setText(I18n.t("sim.action.addUser"));
        addAllUsersButton.setText(I18n.t("sim.action.addAllUsers"));
        removeUserButton.setText(I18n.t("common.remove"));
        clearUsersButton.setText(I18n.t("common.clear"));

        selectAllReadersButton.setText(I18n.t("common.selectAll"));
        deselectAllReadersButton.setText(I18n.t("common.deselectAll"));
        participateAllButton.setText(I18n.t("sim.action.participateAll"));
        participateNoneButton.setText(I18n.t("sim.action.participateNone"));
        enableAllButton.setText(I18n.t("sim.action.enableAll"));
        disableAllButton.setText(I18n.t("sim.action.disableAll"));
        refreshReadersButton.setText(I18n.t("common.refresh"));

        startButton.setText(I18n.t("sim.action.start"));
        stopButton.setText(I18n.t("sim.action.stop"));
        intervalLabel.setText(I18n.t("sim.field.interval"));
        systemTimeLabel.setText(I18n.t("sim.field.systemTime"));
        setTimeButton.setText(I18n.t("sim.action.setTime"));
        resetTimeButton.setText(I18n.t("sim.action.resetTime"));
        resetStatsButton.setText(I18n.t("sim.action.resetStats"));
        refreshDataButton.setText(I18n.t("sim.action.refreshData"));

        infoText.setText(I18n.t("sim.text.instructions"));
        updateStatistics();

        userTableModel.setColumnIdentifiers(getUserColumnNames());
        readerTableModel.setColumnIdentifiers(getReaderColumnNames());
        userTable.getTableHeader().repaint();
        readerTable.getTableHeader().repaint();

        revalidate();
        repaint();
    }

    private String[] getUserColumnNames() {
        return new String[]{
            I18n.t("sim.user.col.userId"),
            I18n.t("sim.user.col.name"),
            I18n.t("sim.user.col.type"),
            I18n.t("sim.user.col.badgeCode")
        };
    }

    private String[] getReaderColumnNames() {
        return new String[]{
            I18n.t("sim.reader.col.participate"),
            I18n.t("sim.reader.col.readerId"),
            I18n.t("sim.reader.col.resourceId"),
            I18n.t("sim.reader.col.resourceName"),
            I18n.t("sim.reader.col.status")
        };
    }

    private void styleSimpleTable(JTable table) {
        table.setRowHeight(Math.max(table.getRowHeight(), 28));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
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
            
            String badgeCode = badge != null ? badge.getCode() : I18n.t("common.none");
            userTableModel.addRow(new Object[]{
                user.getId(),
                user.getFullName(),
                I18n.t("user.type." + user.getUserType().name()),
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
                false, I18n.t("common.none"), I18n.t("common.none"), I18n.t("sim.msg.noReadersPrompt"), false
            });
            statusArea.append(I18n.t("sim.msg.noReadersPrompt"));
            return;
        }
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> resources = dbManager.loadAllResources();
        
        for (BadgeReader reader : readers.values()) {
            String resourceId = reader.getResourceId();
            Resource resource = resources.get(resourceId);
            String resourceName = resource != null ? resource.getName() : I18n.t("common.unknown");
            String resourceType = resource != null ? I18n.t("resource.type." + resource.getType().name()) : I18n.t("common.unknown");
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
        statusArea.append(I18n.f("sim.msg.loadedReaders", readers.size()));
    }
    
    private void addSimulatedUser() {
        int[] selectedRows = userTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, I18n.t("sim.dialog.selectUsersToAdd"), I18n.t("common.warning"), 
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
                            I18n.f("sim.dialog.userNoBadge", user.getFullName()), 
                            I18n.t("common.warning"), 
                            JOptionPane.WARNING_MESSAGE);
                        continue;
                    }
                    
                    userBadges.put(userId, badge);
                }
                
                simulatedUsers.put(userId, user);
                statusArea.append(I18n.f("sim.msg.addedUser", user.getFullName(), badge.getCode()));
            } else if (simulatedUsers.containsKey(userId)) {
                statusArea.append(I18n.f("sim.msg.userAlreadyInSim", (user != null ? user.getFullName() : userId)));
            }
        }
    }
    
    private void removeSimulatedUser() {
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("sim.dialog.noUsersToRemove"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Show selection dialog
        String[] userNames = simulatedUsers.values().stream()
            .map(User::getFullName)
            .toArray(String[]::new);
        
        String selected = (String) JOptionPane.showInputDialog(this,
            I18n.t("sim.dialog.selectUserToRemove"),
            I18n.t("sim.title.removeUser"),
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
                statusArea.append(I18n.f("sim.msg.removedUser", selected));
                
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
                    statusArea.append(I18n.t("sim.msg.simulatorUpdated"));
                }
            }
        }
    }
    
    private void startSimulation() {
        if (simulator != null && simulator.isRunning()) {
            JOptionPane.showMessageDialog(this, I18n.t("sim.dialog.alreadyRunning"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                I18n.t("sim.dialog.addUsersFirst"), I18n.t("common.warning"), 
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
            String message = I18n.t("sim.dialog.noReaders");
            if (disabledCount > 0) {
                message += I18n.f("sim.dialog.readersCheckedButDisabled", disabledCount);
            } else {
                message += I18n.t("sim.dialog.noReadersInstruction");
            }
            JOptionPane.showMessageDialog(this, message, I18n.t("common.warning"), 
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
        statusArea.append(I18n.f("sim.msg.started", interval, readerList.size()));
        
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
            statusArea.append(I18n.t("sim.msg.stopped"));
        }
    }
    
    private void resetStatistics() {
        totalEvents = 0;
        grantedEvents = 0;
        deniedEvents = 0;
        updateStatistics();
        statusArea.append(I18n.t("sim.msg.statsReset"));
    }
    
    private void updateStatistics() {
        statsLabel.setText(I18n.f("sim.text.stats",
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
            I18n.t("sim.title.setTime"), true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        LocalDateTime currentTime = SystemClock.now();
        
        // Year
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel(I18n.t("sim.field.year")), gbc);
        gbc.gridx = 1;
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getYear(), 2020, 2030, 1));
        panel.add(yearSpinner, gbc);
        
        // Month
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel(I18n.t("sim.field.month")), gbc);
        gbc.gridx = 1;
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getMonthValue(), 1, 12, 1));
        panel.add(monthSpinner, gbc);
        
        // Day
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel(I18n.t("sim.field.day")), gbc);
        gbc.gridx = 1;
        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(currentTime.getDayOfMonth(), 1, 31, 1));
        panel.add(daySpinner, gbc);
        
        // Hour
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel(I18n.t("sim.field.hour")), gbc);
        gbc.gridx = 1;
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getHour(), 0, 23, 1));
        panel.add(hourSpinner, gbc);
        
        // Minute
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel(I18n.t("sim.field.minute")), gbc);
        gbc.gridx = 1;
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getMinute(), 0, 59, 1));
        panel.add(minuteSpinner, gbc);
        
        // Preset time buttons
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel presetPanel = new JPanel(new FlowLayout());
        JButton weekdayButton = new JButton(I18n.t("sim.action.weekdayMorning"));
        weekdayButton.addActionListener(e -> {
            LocalDateTime preset = LocalDateTime.now()
                .withHour(8).withMinute(0).withSecond(0);
            yearSpinner.setValue(preset.getYear());
            monthSpinner.setValue(preset.getMonthValue());
            daySpinner.setValue(preset.getDayOfMonth());
            hourSpinner.setValue(8);
            minuteSpinner.setValue(0);
        });
        presetPanel.add(weekdayButton);
        
        JButton weekendButton = new JButton(I18n.t("sim.action.weekendMorning"));
        weekendButton.addActionListener(e -> {
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
        presetPanel.add(weekendButton);
        panel.add(presetPanel, gbc);
        
        // Buttons
        gbc.gridy = 6;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton(I18n.t("common.ok"));
        okButton.addActionListener(e -> {
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
                statusArea.append(I18n.f("sim.msg.timeSet", customTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, I18n.t("sim.error.invalidTime") + ex.getMessage(), 
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(okButton);
        
        JButton cancelButton = new JButton(I18n.t("common.cancel"));
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
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
        statusArea.append(I18n.t("sim.msg.systemTimeReset"));
    }
    
    /**
     * Update time display
     */
    private void updateTimeDisplay() {
        if (timeLabel != null) {
            LocalDateTime now = SystemClock.now();
            String timeStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (SystemClock.isUsingCustomTime()) {
                timeStr += I18n.t("sim.text.custom");
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
            timeStr += I18n.t("sim.text.custom");
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
            statusArea.append(I18n.f("sim.msg.readerStatusChanged", readerId, (active ? I18n.t("common.active") : I18n.t("common.inactive"))));
            
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
        
        statusArea.append(I18n.f("sim.msg.setReadersStatus", 
            (active ? I18n.t("common.enabled") : I18n.t("common.disabled")), count));
        readerTable.repaint();
    }
    
    /**
     * Select all rows in the badge reader table
     */
    private void selectAllReaders() {
        readerTable.selectAll();
        int selectedCount = readerTable.getSelectedRowCount();
        statusArea.append(I18n.f("sim.msg.selectedReadersTable", selectedCount));
    }
    
    /**
     * Deselect all rows in the badge reader table
     */
    private void deselectAllReaders() {
        readerTable.clearSelection();
        statusArea.append(I18n.t("sim.msg.deselectedReadersTable"));
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
                if (readerId != null && !I18n.t("common.none").equals(readerId)) {
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
        
        statusArea.append(I18n.f("sim.msg.setReadersParticipation", 
            (selected ? I18n.t("common.selected") : I18n.t("common.deselected")), count));
    }
    
    /**
     * Select all users in the user table
     */
    private void selectAllUsers() {
        userTable.selectAll();
        int selectedCount = userTable.getSelectedRowCount();
        statusArea.append(I18n.f("sim.msg.selectedUsersTable", selectedCount));
    }
    
    /**
     * Deselect all users in the user table
     */
    private void deselectAllUsers() {
        userTable.clearSelection();
        statusArea.append(I18n.t("sim.msg.deselectedUsersTable"));
    }
    
    /**
     * Add all users to simulation list
     */
    private void addAllUsers() {
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, User> allUsers = dbManager.loadAllUsers();
        
        if (allUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("sim.dialog.noUsersAvailable"), I18n.t("common.info"), 
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
            statusArea.append(I18n.f("sim.msg.addedUser", user.getFullName(), badge.getCode()));
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
            statusArea.append(I18n.t("sim.msg.simulatorUpdated"));
        }
        
        String message = I18n.f("sim.msg.batchAddUsers.done", 
            addedCount, skippedCount, noBadgeCount, simulatedUsers.size()
        );
        
        JOptionPane.showMessageDialog(this, message, I18n.t("sim.msg.batchAddUsers.title"), 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Clear all simulated users
     */
    private void clearAllSimulatedUsers() {
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("sim.dialog.userListEmpty"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int count = simulatedUsers.size();
        int confirm = JOptionPane.showConfirmDialog(this,
            I18n.f("sim.dialog.confirmClearUsers", count),
            I18n.t("sim.title.confirmClear"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // If simulation is running, stop first
            if (simulator != null && simulator.isRunning()) {
                simulator.stop();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                intervalSpinner.setEnabled(true);
                statusArea.append(I18n.t("sim.msg.stopped"));
            }
            
            simulatedUsers.clear();
            userBadges.clear();
            statusArea.append(I18n.f("sim.msg.clearedUsers", count));
            
            JOptionPane.showMessageDialog(this, 
                I18n.t("sim.dialog.usersCleared"), I18n.t("common.success"), 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
