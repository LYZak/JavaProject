package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import javax.swing.*;
import java.awt.*;

/**
 * 主窗口 - 访问控制系统的GUI主界面
 */
public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;
    private AccessControlSystem accessControlSystem;

    public MainWindow() {
        // 创建共享的访问控制系统实例
        this.accessControlSystem = new AccessControlSystem();
        
        initializeComponents();
        setupLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("BigComp 门禁控制系统");
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        tabbedPane = new JTabbedPane();

        // 添加各个功能标签页，传递共享的系统实例
        tabbedPane.addTab("用户管理", new UserManagementPanel(accessControlSystem));
        tabbedPane.addTab("资源管理", new ResourceManagementPanel(accessControlSystem));
        tabbedPane.addTab("资源组管理", new ResourceGroupManagementPanel(accessControlSystem));
        tabbedPane.addTab("配置文件", new ProfileManagementPanel(accessControlSystem));
        tabbedPane.addTab("实时监控", new RealTimeMonitorPanel(accessControlSystem));
        tabbedPane.addTab("日志查看", new LogViewerPanel(accessControlSystem));
        tabbedPane.addTab("事件模拟", new EventSimulationPanel(accessControlSystem));
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

