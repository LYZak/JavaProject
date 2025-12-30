package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * 用户管理面板
 */
public class UserManagementPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private DatabaseManager dbManager;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JComboBox<User.Gender> genderCombo;
    private JComboBox<User.UserType> userTypeCombo;

    public UserManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.dbManager = accessControlSystem.getDatabaseManager();
        initializeComponents();
        setupLayout();
        loadUsers();
    }

    private void initializeComponents() {
        // 表格
        String[] columnNames = {"ID", "姓名", "性别", "类型", "徽章ID"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 输入字段
        firstNameField = new JTextField(15);
        lastNameField = new JTextField(15);
        genderCombo = new JComboBox<>(User.Gender.values());
        userTypeCombo = new JComboBox<>(User.UserType.values());

        // 按钮
        JButton addButton = new JButton("添加用户");
        addButton.addActionListener(e -> addUser());

        JButton deleteButton = new JButton("删除用户");
        deleteButton.addActionListener(e -> deleteUser());

        JButton createBadgeButton = new JButton("创建徽章");
        createBadgeButton.addActionListener(e -> createBadge());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // 顶部：输入表单
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("名:"), gbc);
        gbc.gridx = 1;
        formPanel.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("姓:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("性别:"), gbc);
        gbc.gridx = 1;
        formPanel.add(genderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("类型:"), gbc);
        gbc.gridx = 1;
        formPanel.add(userTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("添加用户") {{
            addActionListener(e -> addUser());
        }});
        buttonPanel.add(new JButton("删除用户") {{
            addActionListener(e -> deleteUser());
        }});
        buttonPanel.add(new JButton("创建徽章") {{
            addActionListener(e -> createBadge());
        }});
        buttonPanel.add(new JButton("分配配置文件") {{
            addActionListener(e -> assignProfile());
        }});
        buttonPanel.add(new JButton("自动分配配置文件") {{
            addActionListener(e -> autoAssignProfilesForAll());
        }});
        formPanel.add(buttonPanel, gbc);

        // 中间：表格
        JScrollPane scrollPane = new JScrollPane(userTable);

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addUser() {
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            
            if (firstName.isEmpty() || lastName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入姓名", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String userId = UUID.randomUUID().toString();
            User user = new User(
                userId,
                (User.Gender) genderCombo.getSelectedItem(),
                firstName,
                lastName,
                (User.UserType) userTypeCombo.getSelectedItem()
            );

            dbManager.addUser(user);
            loadUsers();
            
            // 清空输入字段
            firstNameField.setText("");
            lastNameField.setText("");
            
            JOptionPane.showMessageDialog(this, "用户添加成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "添加用户失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的用户", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        String userName = (String) tableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "确定要删除用户 \"" + userName + "\" 吗？\n此操作将同时删除该用户的徽章和相关配置。", 
            "确认删除", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteUser(userId);
                loadUsers();
                JOptionPane.showMessageDialog(this, "用户删除成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "删除用户失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void createBadge() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择用户", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        try {
            // 获取用户信息
            Map<String, User> allUsers = dbManager.loadAllUsers();
            User user = allUsers.get(userId);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "用户不存在", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Badge badge = new Badge(userId);
            String badgeId = UUID.randomUUID().toString();
            dbManager.addBadge(badge, badgeId);
            
            // 更新用户的徽章ID
            user.setBadgeId(badgeId);
            dbManager.addUser(user);
            
            // 自动根据用户类型分配配置文件
            autoAssignProfileByUserType(user, badgeId);
            
            // 重新加载内存数据
            accessControlSystem.getAccessRequestProcessor().reloadData();
            
            loadUsers();
            JOptionPane.showMessageDialog(this, "徽章创建成功，已自动分配配置文件", "成功", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "创建徽章失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * 根据用户类型自动分配配置文件
     */
    private void autoAssignProfileByUserType(User user, String badgeId) {
        try {
            var profileManager = accessControlSystem.getProfileManager();
            String profileName = getDefaultProfileName(user.getUserType());
            
            if (profileName == null) {
                // 没有对应的默认配置文件，不自动分配
                return;
            }
            
            // 检查配置文件是否存在，如果不存在则创建默认配置文件
            Profile profile = profileManager.getProfile(profileName);
            if (profile == null) {
                // 创建默认配置文件
                createDefaultProfile(profileName, user.getUserType());
                profile = profileManager.getProfile(profileName);
            }
            
            if (profile != null) {
                // 分配配置文件
                dbManager.linkBadgeToProfile(badgeId, profileName);
                System.out.println("自动分配配置文件: " + user.getFullName() + " (" + 
                    user.getUserType() + ") -> " + profileName);
            }
        } catch (Exception e) {
            System.err.println("自动分配配置文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 根据用户类型获取默认配置文件名称
     */
    private String getDefaultProfileName(User.UserType userType) {
        switch (userType) {
            case EMPLOYEE:
                return "员工权限";
            case CONTRACTOR:
                return "承包商权限";
            case INTERN:
                return "实习生权限";
            case VISITOR:
                return "访客权限";
            case PROJECT_MANAGER:
                return "项目经理权限";
            default:
                return null;
        }
    }
    
    /**
     * 创建默认配置文件（如果不存在）
     */
    private void createDefaultProfile(String profileName, User.UserType userType) {
        try {
            var profileManager = accessControlSystem.getProfileManager();
            Profile profile = new Profile(profileName);
            TimeFilter filter = new TimeFilter();
            
            // 根据用户类型设置默认时间过滤器
            switch (userType) {
                case EMPLOYEE:
                    // 员工：工作日 8:00-18:00
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(8, 0, 18, 0)
                    ));
                    profile.addAccessRight("公共区域", filter);
                    profile.addAccessRight("办公区域", filter);
                    profile.addAccessRight("设备资源", filter);
                    break;
                    
                case CONTRACTOR:
                    // 承包商：工作日 9:00-17:00
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(9, 0, 17, 0)
                    ));
                    profile.addAccessRight("公共区域", filter);
                    profile.addAccessRight("办公区域", filter);
                    break;
                    
                case INTERN:
                    // 实习生：工作日 9:00-17:00
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(9, 0, 17, 0)
                    ));
                    profile.addAccessRight("公共区域", filter);
                    profile.addAccessRight("办公区域", filter);
                    break;
                    
                case VISITOR:
                    // 访客：工作日 10:00-16:00
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(10, 0, 16, 0)
                    ));
                    profile.addAccessRight("公共区域", filter);
                    break;
                    
                case PROJECT_MANAGER:
                    // 项目经理：工作日 7:00-20:00，可访问高安全区域
                    filter.setDaysOfWeek(Set.of(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY
                    ));
                    filter.setTimeRanges(List.of(
                        new TimeFilter.TimeRange(7, 0, 20, 0)
                    ));
                    profile.addAccessRight("公共区域", filter);
                    profile.addAccessRight("办公区域", filter);
                    profile.addAccessRight("设备资源", filter);
                    profile.addAccessRight("高安全区域", filter);
                    break;
                    
                default:
                    // 默认：全天候访问公共区域
                    profile.addAccessRight("公共区域", filter);
                    break;
            }
            
            // 保存配置文件
            profileManager.saveProfile(profile);
            System.out.println("创建默认配置文件: " + profileName);
        } catch (Exception e) {
            System.err.println("创建默认配置文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void assignProfile() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择用户", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        User user = dbManager.loadAllUsers().get(userId);
        
        if (user == null) {
            JOptionPane.showMessageDialog(this, "用户不存在", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (user.getBadgeId() == null) {
            JOptionPane.showMessageDialog(this, "该用户还没有徽章，请先创建徽章", "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 获取所有可用的配置文件
        var profileManager = accessControlSystem.getProfileManager();
        Map<String, Profile> profiles = profileManager.getAllProfiles();
        
        if (profiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可用的配置文件，请先在配置文件管理中创建", 
                "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String[] profileNames = profiles.keySet().toArray(new String[0]);
        
        // 显示选择对话框
        String selectedProfile = (String) JOptionPane.showInputDialog(this,
            "选择要分配给用户的配置文件:",
            "分配配置文件",
            JOptionPane.QUESTION_MESSAGE,
            null,
            profileNames,
            profileNames.length > 0 ? profileNames[0] : null);
        
        if (selectedProfile != null) {
            try {
                // 检查用户是否已有该配置文件
                Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
                Set<String> existingProfiles = userProfiles.get(user.getId());
                if (existingProfiles != null && existingProfiles.contains(selectedProfile)) {
                    JOptionPane.showMessageDialog(this, 
                        "用户已拥有配置文件 \"" + selectedProfile + "\"", 
                        "提示", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                
                // 分配配置文件（会添加到已有的配置文件中，不会覆盖）
                dbManager.linkBadgeToProfile(user.getBadgeId(), selectedProfile);
                accessControlSystem.getAccessRequestProcessor().reloadData();
                
                // 显示用户当前的所有配置文件
                userProfiles = dbManager.loadUserProfiles();
                existingProfiles = userProfiles.get(user.getId());
                StringBuilder profileList = new StringBuilder();
                if (existingProfiles != null && !existingProfiles.isEmpty()) {
                    for (String profile : existingProfiles) {
                        if (profileList.length() > 0) {
                            profileList.append(", ");
                        }
                        profileList.append(profile);
                    }
                }
                
                JOptionPane.showMessageDialog(this, 
                    "配置文件 \"" + selectedProfile + "\" 已分配给用户\n\n" +
                    "用户当前拥有的配置文件：\n" + profileList.toString(), 
                    "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "分配配置文件失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void loadUsers() {
        tableModel.setRowCount(0);
        try {
            // 使用loadAllUsers()来加载所有用户，包括没有徽章的用户
            Map<String, User> users = dbManager.loadAllUsers();
            for (User user : users.values()) {
                tableModel.addRow(new Object[]{
                    user.getId(),
                    user.getFullName(),
                    user.getGender().toString(),
                    user.getUserType().toString(),
                    user.getBadgeId() != null ? user.getBadgeId() : "无"
                });
            }
            
            // 重新加载内存数据（用于访问控制，只需要有徽章的用户）
            accessControlSystem.getAccessRequestProcessor().reloadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载用户失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * 为所有有徽章但没有配置文件的用户自动分配配置文件
     */
    private void autoAssignProfilesForAll() {
        try {
            Map<String, User> allUsers = dbManager.loadAllUsers();
            Map<String, Set<String>> userProfiles = dbManager.loadUserProfiles();
            
            int assignedCount = 0;
            int skippedWithProfile = 0;
            int skippedNoBadge = 0;
            int errorCount = 0;
            StringBuilder details = new StringBuilder();
            
            for (User user : allUsers.values()) {
                // 检查用户是否有徽章
                if (user.getBadgeId() == null || user.getBadgeId().isEmpty()) {
                    skippedNoBadge++;
                    continue;
                }
                
                // 检查用户是否已有配置文件
                Set<String> existingProfiles = userProfiles.get(user.getId());
                if (existingProfiles != null && !existingProfiles.isEmpty()) {
                    skippedWithProfile++;
                    continue;
                }
                
                // 自动分配配置文件
                try {
                    String profileName = getDefaultProfileName(user.getUserType());
                    if (profileName != null) {
                        // 检查配置文件是否存在，如果不存在则创建
                        var profileManager = accessControlSystem.getProfileManager();
                        Profile profile = profileManager.getProfile(profileName);
                        if (profile == null) {
                            createDefaultProfile(profileName, user.getUserType());
                        }
                        
                        // 分配配置文件
                        dbManager.linkBadgeToProfile(user.getBadgeId(), profileName);
                        assignedCount++;
                        details.append("  ✓ ").append(user.getFullName())
                               .append(" (").append(user.getUserType())
                               .append(") -> ").append(profileName).append("\n");
                    }
                } catch (Exception e) {
                    errorCount++;
                    details.append("  ✗ ").append(user.getFullName())
                           .append(" -> 失败: ").append(e.getMessage()).append("\n");
                }
            }
            
            // 重新加载内存数据
            accessControlSystem.getAccessRequestProcessor().reloadData();
            
            // 显示结果
            String message = String.format(
                "自动分配配置文件完成！\n\n" +
                "统计信息：\n" +
                "• 成功分配: %d 个用户\n" +
                "• 跳过（已有配置文件）: %d 个用户\n" +
                "• 跳过（无徽章）: %d 个用户\n" +
                "• 失败: %d 个用户\n\n" +
                "详细信息：\n%s",
                assignedCount, skippedWithProfile, skippedNoBadge, errorCount, 
                details.length() > 0 ? details.toString() : "无"
            );
            
            JOptionPane.showMessageDialog(this, message, 
                "自动分配完成", 
                assignedCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            
            // 刷新用户列表
            loadUsers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "自动分配配置文件失败: " + e.getMessage(), 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}

