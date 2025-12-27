package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.UUID;

/**
 * 用户管理面板
 */
public class UserManagementPanel extends JPanel {
    private JTable userTable;
    private DefaultTableModel tableModel;
    private DatabaseManager dbManager;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JComboBox<User.Gender> genderCombo;
    private JComboBox<User.UserType> userTypeCombo;

    public UserManagementPanel() {
        this.dbManager = new DatabaseManager();
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

        int confirm = JOptionPane.showConfirmDialog(this, 
            "确定要删除选中的用户吗？", "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // 实现删除逻辑
            JOptionPane.showMessageDialog(this, "删除功能待实现", "提示", JOptionPane.INFORMATION_MESSAGE);
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
            Badge badge = new Badge(userId);
            String badgeId = UUID.randomUUID().toString();
            dbManager.addBadge(badge, badgeId);
            
            // 更新用户的徽章ID
            User user = new User(userId, User.Gender.MALE, "", "", User.UserType.EMPLOYEE);
            user.setBadgeId(badgeId);
            dbManager.addUser(user);
            
            loadUsers();
            JOptionPane.showMessageDialog(this, "徽章创建成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "创建徽章失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadUsers() {
        // 简化实现：从数据库加载用户并显示在表格中
        tableModel.setRowCount(0);
        // 实际实现需要从数据库查询
    }
}

