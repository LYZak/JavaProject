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
 * 事件模拟面板
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
        // 用户表格
        String[] userColumns = {"用户ID", "姓名", "类型", "徽章代码"};
        userTableModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // 读卡器表格（状态列可编辑，添加选择列）
        String[] readerColumns = {"参与模拟", "读卡器ID", "资源ID", "资源名称", "状态"};
        readerTableModel = new DefaultTableModel(readerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 4; // 参与模拟列和状态列可编辑
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0 || column == 4) {
                    return Boolean.class; // 复选框列
                }
                return String.class;
            }
        };
        readerTable = new JTable(readerTableModel);
        readerTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // 设置列宽
        readerTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        readerTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        readerTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        readerTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        readerTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        
        // 为状态列添加复选框渲染器和编辑器
        readerTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JCheckBox checkBox = new JCheckBox();
                if (value instanceof String) {
                    checkBox.setSelected("激活".equals(value));
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
                return checkBox.isSelected() ? "激活" : "停用";
            }
        });
        
        // 为"参与模拟"列添加复选框渲染器和编辑器
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
        
        // 为状态列添加复选框渲染器和编辑器
        readerTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JCheckBox checkBox = new JCheckBox();
                if (value instanceof Boolean) {
                    checkBox.setSelected((Boolean) value);
                } else if (value instanceof String) {
                    checkBox.setSelected("激活".equals(value));
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
        
        // 添加表格变化监听器
        readerTable.getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            
            if (column == 0) {
                // 参与模拟列变化
                String readerId = (String) readerTableModel.getValueAt(row, 1);
                Boolean selected = (Boolean) readerTableModel.getValueAt(row, 0);
                statusArea.append("读卡器 " + readerId + " " + (selected ? "已加入" : "已移除") + "模拟\n");
            } else if (column == 4) {
                // 状态列变化
                String readerId = (String) readerTableModel.getValueAt(row, 1);
                Object statusValue = readerTableModel.getValueAt(row, 4);
                boolean active = statusValue instanceof Boolean ? (Boolean) statusValue : 
                                "激活".equals(statusValue);
                
                updateReaderStatus(readerId, active);
            }
        });
        
        // 控制按钮
        startButton = new JButton("开始模拟");
        startButton.addActionListener(e -> startSimulation());
        
        stopButton = new JButton("停止模拟");
        stopButton.addActionListener(e -> stopSimulation());
        stopButton.setEnabled(false);
        
        // 状态区域
        statusArea = new JTextArea(5, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 左侧：用户列表
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("模拟用户列表"));
        leftPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        JPanel leftButtonPanel = new JPanel(new FlowLayout());
        leftButtonPanel.add(new JButton("添加用户") {{
            addActionListener(e -> addSimulatedUser());
        }});
        leftButtonPanel.add(new JButton("加入所有用户") {{
            addActionListener(e -> addAllUsers());
        }});
        leftButtonPanel.add(new JButton("移除用户") {{
            addActionListener(e -> removeSimulatedUser());
        }});
        leftButtonPanel.add(new JButton("清空用户列表") {{
            addActionListener(e -> clearAllSimulatedUsers());
        }});
        leftButtonPanel.add(new JButton("清空用户列表") {{
            addActionListener(e -> clearAllSimulatedUsers());
        }});
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // 中间：读卡器列表
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("读卡器列表（可用于模拟）"));
        centerPanel.add(new JScrollPane(readerTable), BorderLayout.CENTER);
        
        // 在读卡器列表下方添加控制按钮和信息提示
        JPanel readerControlPanel = new JPanel(new BorderLayout());
        
        JPanel readerButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        readerButtonPanel.add(new JButton("全选读卡器") {{
            addActionListener(e -> setAllReadersSelected(true));
        }});
        readerButtonPanel.add(new JButton("取消全选") {{
            addActionListener(e -> setAllReadersSelected(false));
        }});
        readerButtonPanel.add(new JButton("全部启用") {{
            addActionListener(e -> setAllReadersStatus(true));
        }});
        readerButtonPanel.add(new JButton("全部禁用") {{
            addActionListener(e -> setAllReadersStatus(false));
        }});
        readerButtonPanel.add(new JButton("刷新列表") {{
            addActionListener(e -> loadReaders());
        }});
        readerControlPanel.add(readerButtonPanel, BorderLayout.NORTH);
        
        JPanel readerInfoPanel = new JPanel(new BorderLayout());
        JTextArea infoText = new JTextArea(
            "使用说明：\n" +
            "• '参与模拟'列：勾选后该读卡器会参与事件模拟\n" +
            "• '状态'列：启用/禁用读卡器，停用的读卡器不会响应任何刷卡操作\n" +
            "• 只有同时勾选'参与模拟'且'状态'为启用的读卡器才会在模拟中生成事件"
        );
        infoText.setEditable(false);
        infoText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        infoText.setBackground(readerControlPanel.getBackground());
        infoText.setForeground(Color.DARK_GRAY);
        readerInfoPanel.add(infoText, BorderLayout.CENTER);
        readerControlPanel.add(readerInfoPanel, BorderLayout.SOUTH);
        
        centerPanel.add(readerControlPanel, BorderLayout.SOUTH);
        
        // 右侧：控制面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("模拟控制"));
        
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
        controlPanel.add(new JLabel("事件间隔(秒):"), gbc);
        gbc.gridx = 1;
        intervalSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 60, 1));
        controlPanel.add(intervalSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        statsLabel = new JLabel("统计: 总事件: 0 | 允许: 0 | 拒绝: 0");
        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        controlPanel.add(statsLabel, gbc);
        
        // 系统时间控制
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("系统时间:"), gbc);
        gbc.gridx = 1;
        timeLabel = new JLabel(getCurrentTimeString());
        timeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        controlPanel.add(timeLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 1;
        setTimeButton = new JButton("设置时间");
        setTimeButton.addActionListener(e -> setSystemTime());
        controlPanel.add(setTimeButton, gbc);
        
        gbc.gridx = 1;
        resetTimeButton = new JButton("重置时间");
        resetTimeButton.addActionListener(e -> resetSystemTime());
        resetTimeButton.setEnabled(SystemClock.isUsingCustomTime());
        controlPanel.add(resetTimeButton, gbc);
        
        // 更新时间显示
        javax.swing.Timer timeUpdateTimer = new javax.swing.Timer(1000, e -> updateTimeDisplay());
        timeUpdateTimer.start();
        
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        controlPanel.add(new JButton("重置统计") {{
            addActionListener(e -> resetStatistics());
        }}, gbc);
        
        gbc.gridy = 7;
        controlPanel.add(new JButton("刷新数据") {{
            addActionListener(e -> {
                loadData();
                JOptionPane.showMessageDialog(EventSimulationPanel.this, 
                    "数据已刷新", "提示", JOptionPane.INFORMATION_MESSAGE);
            });
        }}, gbc);
        
        rightPanel.add(controlPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        
        // 主布局
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
        // 使用loadAllUsers()来加载所有用户，包括没有徽章的用户
        Map<String, User> users = dbManager.loadAllUsers();
        // 加载所有徽章
        Map<String, Badge> allBadges = dbManager.loadAllBadges();
        
        for (User user : users.values()) {
            // 从数据库加载徽章，如果用户有徽章ID
            Badge badge = null;
            if (user.getBadgeId() != null) {
                badge = dbManager.loadBadgeById(user.getBadgeId());
            } else if (allBadges.containsKey(user.getId())) {
                badge = allBadges.get(user.getId());
            }
            
            // 如果找到了徽章，保存到userBadges中
            if (badge != null) {
                userBadges.put(user.getId(), badge);
            }
            
            String badgeCode = badge != null ? badge.getCode() : "无";
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
            // 如果没有读卡器，显示提示信息
            readerTableModel.addRow(new Object[]{
                false, "无", "无", "没有可用的读卡器", false
            });
            statusArea.append("提示: 没有可用的读卡器，请先在资源管理中创建资源和读卡器\n");
            return;
        }
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> resources = dbManager.loadAllResources();
        
        for (BadgeReader reader : readers.values()) {
            String resourceId = reader.getResourceId();
            Resource resource = resources.get(resourceId);
            String resourceName = resource != null ? resource.getName() : "未知";
            String resourceType = resource != null ? resource.getType().toString() : "未知";
            Boolean status = reader.isActive(); // 使用Boolean类型以便复选框显示
            Boolean selected = true; // 默认选中参与模拟
            
            readerTableModel.addRow(new Object[]{
                selected,  // 参与模拟
                reader.getId(),
                resourceId,
                resourceName + " (" + resourceType + ")",
                status  // 启用状态
            });
        }
        
        // 在状态区域显示读卡器数量
        statusArea.append("已加载 " + readers.size() + " 个读卡器（默认全部参与模拟）\n");
    }
    
    private void addSimulatedUser() {
        int[] selectedRows = userTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "请选择要添加的用户", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, User> allUsers = dbManager.loadAllUsers();
        
        for (int row : selectedRows) {
            String userId = (String) userTableModel.getValueAt(row, 0);
            User user = allUsers.get(userId);
            
            if (user != null && !simulatedUsers.containsKey(userId)) {
                // 从数据库加载徽章，如果用户有徽章
                Badge badge = userBadges.get(userId);
                if (badge == null) {
                    if (user.getBadgeId() != null) {
                        badge = dbManager.loadBadgeById(user.getBadgeId());
                    } else {
                        // 尝试通过用户ID加载
                        badge = dbManager.loadBadgeByUserId(userId);
                    }
                    
                    // 如果还是没有找到，创建一个新徽章（但不会保存到数据库）
                    if (badge == null) {
                        JOptionPane.showMessageDialog(this, 
                            "用户 \"" + user.getFullName() + "\" 没有徽章，无法添加到模拟。请先为用户创建徽章。", 
                            "提示", 
                            JOptionPane.WARNING_MESSAGE);
                        continue;
                    }
                    
                    userBadges.put(userId, badge);
                }
                
                simulatedUsers.put(userId, user);
                statusArea.append("添加模拟用户: " + user.getFullName() + " (徽章: " + badge.getCode() + ")\n");
            } else if (simulatedUsers.containsKey(userId)) {
                statusArea.append("用户 \"" + (user != null ? user.getFullName() : userId) + "\" 已在模拟列表中\n");
            }
        }
    }
    
    private void removeSimulatedUser() {
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可移除的模拟用户", "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 显示选择对话框
        String[] userNames = simulatedUsers.values().stream()
            .map(User::getFullName)
            .toArray(String[]::new);
        
        String selected = (String) JOptionPane.showInputDialog(this,
            "选择要移除的模拟用户:",
            "移除模拟用户",
            JOptionPane.QUESTION_MESSAGE,
            null,
            userNames,
            userNames[0]);
        
        if (selected != null) {
            // 找到并移除选中的用户
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
                statusArea.append("已移除模拟用户: " + selected + "\n");
                
                // 如果模拟器正在运行，需要更新模拟器
                if (simulator != null && simulator.isRunning()) {
                    // 重新创建模拟器以更新用户列表
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
                    statusArea.append("模拟器已更新\n");
                }
            }
        }
    }
    
    private void startSimulation() {
        if (simulator != null && simulator.isRunning()) {
            JOptionPane.showMessageDialog(this, "模拟已在运行中", "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "请先添加模拟用户", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        var router = accessControlSystem.getRouter();
        Map<String, BadgeReader> allReaders = router.getBadgeReaders();
        
        // 只选择勾选了"参与模拟"且状态为启用的读卡器
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
            String message = "没有可用的读卡器参与模拟。\n\n";
            if (disabledCount > 0) {
                message += "提示：有 " + disabledCount + " 个读卡器已勾选但未启用。\n";
                message += "请先启用这些读卡器，或勾选其他已启用的读卡器。";
            } else {
                message += "请先：\n";
                message += "1. 在'参与模拟'列勾选要使用的读卡器\n";
                message += "2. 确保这些读卡器的'状态'为启用";
            }
            JOptionPane.showMessageDialog(this, message, "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 获取事件间隔
        int interval = (Integer) intervalSpinner.getValue();
        
        // 创建模拟器（只使用选中的读卡器）
        simulator = new EventSimulator(readerList);
        simulator.setInterval(interval);
        
        // 添加模拟用户
        for (User user : simulatedUsers.values()) {
            Badge badge = userBadges.get(user.getId());
            if (badge != null) {
                simulator.addSimulatedUser(user, badge);
            }
        }
        
        // 开始模拟
        simulator.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        intervalSpinner.setEnabled(false);
        statusArea.append("模拟已开始 (间隔: " + interval + "秒, 使用 " + readerList.size() + " 个读卡器)\n");
        
        // 启动统计更新线程
        startStatisticsUpdate();
    }
    
    private void stopSimulation() {
        if (simulator != null) {
            simulator.stop();
            simulator = null;
            startButton.setEnabled(true);
            stopButton.setEnabled(true);
            intervalSpinner.setEnabled(true);
            statusArea.append("模拟已停止\n");
        }
    }
    
    private void resetStatistics() {
        totalEvents = 0;
        grantedEvents = 0;
        deniedEvents = 0;
        updateStatistics();
        statusArea.append("统计已重置\n");
    }
    
    private void updateStatistics() {
        statsLabel.setText(String.format("统计: 总事件: %d | 允许: %d | 拒绝: %d", 
            totalEvents, grantedEvents, deniedEvents));
    }
    
    private void startStatisticsUpdate() {
        // 创建一个线程来定期更新统计信息
        Thread statsThread = new Thread(() -> {
            while (simulator != null && simulator.isRunning()) {
                try {
                    // 从日志中读取统计信息（简化实现）
                    // 实际应该从LogManager或AccessControlSystem获取
                    Thread.sleep(2000); // 每2秒更新一次
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
     * 设置系统时间（用于测试）
     */
    private void setSystemTime() {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
            "设置系统时间", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        LocalDateTime currentTime = SystemClock.now();
        
        // 年份
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("年份:"), gbc);
        gbc.gridx = 1;
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getYear(), 2020, 2030, 1));
        panel.add(yearSpinner, gbc);
        
        // 月份
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("月份:"), gbc);
        gbc.gridx = 1;
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getMonthValue(), 1, 12, 1));
        panel.add(monthSpinner, gbc);
        
        // 日期
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("日期:"), gbc);
        gbc.gridx = 1;
        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(currentTime.getDayOfMonth(), 1, 31, 1));
        panel.add(daySpinner, gbc);
        
        // 小时
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("小时:"), gbc);
        gbc.gridx = 1;
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getHour(), 0, 23, 1));
        panel.add(hourSpinner, gbc);
        
        // 分钟
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("分钟:"), gbc);
        gbc.gridx = 1;
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(currentTime.getMinute(), 0, 59, 1));
        panel.add(minuteSpinner, gbc);
        
        // 预设时间按钮
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel presetPanel = new JPanel(new FlowLayout());
        presetPanel.add(new JButton("工作日 8:00") {{
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
        presetPanel.add(new JButton("周末 10:00") {{
            addActionListener(e -> {
                LocalDateTime preset = LocalDateTime.now()
                    .withHour(10).withMinute(0).withSecond(0);
                // 设置为最近的周六
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
        
        // 按钮
        gbc.gridy = 6;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("确定") {{
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
                    statusArea.append("系统时间已设置为: " + customTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n");
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "设置时间失败: " + ex.getMessage(), 
                        "错误", JOptionPane.ERROR_MESSAGE);
                }
            });
        }});
        buttonPanel.add(new JButton("取消") {{
            addActionListener(e -> dialog.dispose());
        }});
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * 重置系统时间
     */
    private void resetSystemTime() {
        SystemClock.clearCustomTime();
        updateTimeDisplay();
        resetTimeButton.setEnabled(false);
        statusArea.append("系统时间已重置为当前时间\n");
    }
    
    /**
     * 更新时间显示
     */
    private void updateTimeDisplay() {
        if (timeLabel != null) {
            LocalDateTime now = SystemClock.now();
            String timeStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (SystemClock.isUsingCustomTime()) {
                timeStr += " (自定义)";
                timeLabel.setForeground(Color.RED);
            } else {
                timeLabel.setForeground(Color.BLACK);
            }
            timeLabel.setText(timeStr);
        }
    }
    
    /**
     * 获取当前时间字符串
     */
    private String getCurrentTimeString() {
        LocalDateTime now = SystemClock.now();
        String timeStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (SystemClock.isUsingCustomTime()) {
            timeStr += " (自定义)";
        }
        return timeStr;
    }
    
    /**
     * 更新读卡器状态
     */
    private void updateReaderStatus(String readerId, boolean active) {
        var router = accessControlSystem.getRouter();
        BadgeReader reader = router.getBadgeReaders().get(readerId);
        if (reader != null) {
            reader.setActive(active);
            statusArea.append("读卡器 " + readerId + " 已" + (active ? "启用" : "禁用") + "\n");
            
            // 刷新表格显示
            for (int i = 0; i < readerTableModel.getRowCount(); i++) {
                if (readerId.equals(readerTableModel.getValueAt(i, 0))) {
                    readerTableModel.setValueAt(active, i, 3);
                    break;
                }
            }
        }
    }
    
    /**
     * 设置所有读卡器状态
     */
    private void setAllReadersStatus(boolean active) {
        var router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        
        int count = 0;
        for (BadgeReader reader : readers.values()) {
            reader.setActive(active);
            count++;
            
            // 更新表格显示（状态列在第4列，索引为4）
            for (int i = 0; i < readerTableModel.getRowCount(); i++) {
                if (reader.getId().equals(readerTableModel.getValueAt(i, 1))) {
                    readerTableModel.setValueAt(active, i, 4);
                    break;
                }
            }
        }
        
        statusArea.append("已" + (active ? "启用" : "禁用") + " " + count + " 个读卡器\n");
        readerTable.repaint();
    }
    
    /**
     * 设置所有读卡器的参与模拟状态
     */
    private void setAllReadersSelected(boolean selected) {
        int count = 0;
        for (int i = 0; i < readerTableModel.getRowCount(); i++) {
            String readerId = (String) readerTableModel.getValueAt(i, 1);
            if (readerId != null && !"无".equals(readerId)) {
                readerTableModel.setValueAt(selected, i, 0);
                count++;
            }
        }
        
        statusArea.append((selected ? "已选择" : "已取消") + " " + count + " 个读卡器参与模拟\n");
        readerTable.repaint();
    }
    
    /**
     * 加入所有用户到模拟列表
     */
    private void addAllUsers() {
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, User> allUsers = dbManager.loadAllUsers();
        
        if (allUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可用的用户", "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int addedCount = 0;
        int skippedCount = 0;
        int noBadgeCount = 0;
        
        for (User user : allUsers.values()) {
            // 跳过已经在模拟列表中的用户
            if (simulatedUsers.containsKey(user.getId())) {
                skippedCount++;
                continue;
            }
            
            // 检查用户是否有徽章
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
            
            // 添加到模拟列表
            simulatedUsers.put(user.getId(), user);
            userBadges.put(user.getId(), badge);
            addedCount++;
            statusArea.append("添加模拟用户: " + user.getFullName() + " (徽章: " + badge.getCode() + ")\n");
        }
        
        // 如果模拟器正在运行，需要更新模拟器
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
            statusArea.append("模拟器已更新\n");
        }
        
        String message = String.format(
            "批量添加用户完成！\n\n" +
            "统计信息：\n" +
            "• 成功添加: %d 个用户\n" +
            "• 跳过（已在列表中）: %d 个用户\n" +
            "• 跳过（无徽章）: %d 个用户\n\n" +
            "当前模拟用户总数: %d",
            addedCount, skippedCount, noBadgeCount, simulatedUsers.size()
        );
        
        JOptionPane.showMessageDialog(this, message, "批量添加完成", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * 清空所有模拟用户
     */
    private void clearAllSimulatedUsers() {
        if (simulatedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "模拟用户列表已为空", "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int count = simulatedUsers.size();
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要清空所有模拟用户吗？\n\n" +
            "当前有 " + count + " 个用户在模拟列表中\n" +
            "如果模拟正在运行，将会停止模拟。",
            "确认清空",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // 如果模拟正在运行，先停止
            if (simulator != null && simulator.isRunning()) {
                simulator.stop();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                intervalSpinner.setEnabled(true);
                statusArea.append("模拟已停止\n");
            }
            
            simulatedUsers.clear();
            userBadges.clear();
            statusArea.append("已清空所有模拟用户（共 " + count + " 个）\n");
            
            JOptionPane.showMessageDialog(this, 
                "已清空所有模拟用户", "成功", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
