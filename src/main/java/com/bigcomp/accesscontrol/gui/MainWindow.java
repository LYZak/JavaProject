// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import javax.swing.*;
import java.awt.*;

/**
 * Main Window - GUI main interface for access control system
 */
public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;
    private AccessControlSystem accessControlSystem;

    public MainWindow() {
        // Create shared access control system instance
        this.accessControlSystem = new AccessControlSystem();
        
        initializeComponents();
        setupLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("BigComp Access Control System");
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        tabbedPane = new JTabbedPane();

        // Add functional tabs, pass shared system instance
        tabbedPane.addTab("User Management", new UserManagementPanel(accessControlSystem));
        tabbedPane.addTab("Resource Management", new ResourceManagementPanel(accessControlSystem));
        tabbedPane.addTab("Resource Group Management", new ResourceGroupManagementPanel(accessControlSystem));
        tabbedPane.addTab("Profile Management", new ProfileManagementPanel(accessControlSystem));
        tabbedPane.addTab("Real-time Monitor", new RealTimeMonitorPanel(accessControlSystem));
        tabbedPane.addTab("Log Viewer", new LogViewerPanel(accessControlSystem));
        tabbedPane.addTab("Event Simulation", new EventSimulationPanel(accessControlSystem));
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        // Add menu bar
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                "BigComp Access Control System v1.0\n\n" +
                "Comprehensive access control and management system",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
}

