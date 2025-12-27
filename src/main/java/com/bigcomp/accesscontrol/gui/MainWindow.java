package com.bigcomp.accesscontrol.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 主窗口 - 访问控制系统的GUI主界面
 */
public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;

    public MainWindow() {
        initializeComponents();
        setupLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("BigComp 门禁控制系统");
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        tabbedPane = new JTabbedPane();

        // 添加各个功能标签页
        tabbedPane.addTab("用户管理", new UserManagementPanel());
        tabbedPane.addTab("资源管理", new ResourceManagementPanel());
        tabbedPane.addTab("配置文件", new ProfileManagementPanel());
        tabbedPane.addTab("实时监控", new RealTimeMonitorPanel());
        tabbedPane.addTab("日志查看", new LogViewerPanel());
        tabbedPane.addTab("事件模拟", new EventSimulationPanel());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        // 添加菜单栏
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("文件");
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                "BigComp 门禁控制系统 v1.0\n\n" +
                "全面的门禁控制和管理系统",
                "关于",
                JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
}

