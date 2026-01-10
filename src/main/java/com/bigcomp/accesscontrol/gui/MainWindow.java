// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main Window - GUI main interface for access control system
 */
public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;
    private AccessControlSystem accessControlSystem;
    private JLabel statusLabel;

    public MainWindow() {
        // Create shared access control system instance
        this.accessControlSystem = new AccessControlSystem();
        
        initializeComponents();
        setupLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("BigComp Access Control System");
        setSize(1200, 800);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Add functional tabs, pass shared system instance
        tabbedPane.addTab("Users", new UserManagementPanel(accessControlSystem));
        tabbedPane.addTab("Resources", new ResourceManagementPanel(accessControlSystem));
        tabbedPane.addTab("Groups", new ResourceGroupManagementPanel(accessControlSystem));
        tabbedPane.addTab("Profiles", new ProfileManagementPanel(accessControlSystem));
        tabbedPane.addTab("Monitor", new RealTimeMonitorPanel(accessControlSystem));
        tabbedPane.addTab("Logs", new LogViewerPanel(accessControlSystem));
        tabbedPane.addTab("Simulation", new EventSimulationPanel(accessControlSystem));
        tabbedPane.addChangeListener(e -> updateStatus());

        statusLabel = new JLabel();
        updateStatus();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        // Add menu bar
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> updateStatus());
        fileMenu.add(refreshItem);
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

    private void updateStatus() {
        String tabTitle = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
        statusLabel.setText("Ready  |  " + tabTitle);
    }
}

