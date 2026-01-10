// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Main Window - GUI main interface for access control system
 */
public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;
    private AccessControlSystem accessControlSystem;
    private JLabel statusLabel;
    private JMenu fileMenu;
    private JMenu helpMenu;
    private JMenu languageMenu;
    private JMenuItem refreshItem;
    private JMenuItem exitItem;
    private JMenuItem aboutItem;
    private JRadioButtonMenuItem zhItem;
    private JRadioButtonMenuItem enItem;
    private UserManagementPanel userPanel;
    private ResourceManagementPanel resourcePanel;
    private ResourceGroupManagementPanel groupPanel;
    private ProfileManagementPanel profilePanel;
    private RealTimeMonitorPanel monitorPanel;
    private LogViewerPanel logPanel;
    private EventSimulationPanel simulationPanel;

    public MainWindow() {
        // Create shared access control system instance
        this.accessControlSystem = new AccessControlSystem();
        
        initializeComponents();
        setupLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        I18n.setLanguage(I18n.Language.EN);
        applyLanguage();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Add functional tabs, pass shared system instance
        userPanel = new UserManagementPanel(accessControlSystem);
        resourcePanel = new ResourceManagementPanel(accessControlSystem);
        groupPanel = new ResourceGroupManagementPanel(accessControlSystem);
        profilePanel = new ProfileManagementPanel(accessControlSystem);
        monitorPanel = new RealTimeMonitorPanel(accessControlSystem);
        logPanel = new LogViewerPanel(accessControlSystem);
        simulationPanel = new EventSimulationPanel(accessControlSystem);

        tabbedPane.addTab(I18n.t("tab.users"), userPanel);
        tabbedPane.addTab(I18n.t("tab.resources"), resourcePanel);
        tabbedPane.addTab(I18n.t("tab.groups"), groupPanel);
        tabbedPane.addTab(I18n.t("tab.profiles"), profilePanel);
        tabbedPane.addTab(I18n.t("tab.monitor"), monitorPanel);
        tabbedPane.addTab(I18n.t("tab.logs"), logPanel);
        tabbedPane.addTab(I18n.t("tab.simulation"), simulationPanel);
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
        
        fileMenu = new JMenu();
        refreshItem = new JMenuItem();
        refreshItem.addActionListener(e -> updateStatus());
        fileMenu.add(refreshItem);
        exitItem = new JMenuItem();
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        languageMenu = new JMenu();
        zhItem = new JRadioButtonMenuItem();
        enItem = new JRadioButtonMenuItem();
        ButtonGroup langGroup = new ButtonGroup();
        langGroup.add(zhItem);
        langGroup.add(enItem);
        zhItem.addActionListener(e -> {
            I18n.setLanguage(I18n.Language.ZH);
            applyLanguage();
        });
        enItem.addActionListener(e -> {
            I18n.setLanguage(I18n.Language.EN);
            applyLanguage();
        });
        languageMenu.add(zhItem);
        languageMenu.add(enItem);

        helpMenu = new JMenu();
        aboutItem = new JMenuItem();
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                I18n.t("about.body"),
                I18n.t("about.title"),
                JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(languageMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        if (I18n.getLanguage() == I18n.Language.ZH) {
            zhItem.setSelected(true);
        } else {
            enItem.setSelected(true);
        }
    }

    private void updateStatus() {
        String tabTitle = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
        statusLabel.setText(I18n.f("status.ready", tabTitle));
    }

    private void applyLanguage() {
        setTitle(I18n.t("app.title"));
        fileMenu.setText(I18n.t("menu.file"));
        refreshItem.setText(I18n.t("menu.refresh"));
        exitItem.setText(I18n.t("menu.exit"));
        languageMenu.setText(I18n.t("menu.language"));
        zhItem.setText(I18n.t("menu.lang.zh"));
        enItem.setText(I18n.t("menu.lang.en"));
        helpMenu.setText(I18n.t("menu.help"));
        aboutItem.setText(I18n.t("menu.about"));

        tabbedPane.setTitleAt(0, I18n.t("tab.users"));
        tabbedPane.setTitleAt(1, I18n.t("tab.resources"));
        tabbedPane.setTitleAt(2, I18n.t("tab.groups"));
        tabbedPane.setTitleAt(3, I18n.t("tab.profiles"));
        tabbedPane.setTitleAt(4, I18n.t("tab.monitor"));
        tabbedPane.setTitleAt(5, I18n.t("tab.logs"));
        tabbedPane.setTitleAt(6, I18n.t("tab.simulation"));

        userPanel.applyLanguage();
        resourcePanel.applyLanguage();
        groupPanel.applyLanguage();
        profilePanel.applyLanguage();
        monitorPanel.applyLanguage();
        logPanel.applyLanguage();
        simulationPanel.applyLanguage();

        updateStatus();
        SwingUtilities.updateComponentTreeUI(this);
    }
}

final class I18n {
    enum Language { ZH, EN }

    private static volatile Language language = Language.EN;
    private static final Map<Language, Map<String, String>> DICT = buildDict();

    static void setLanguage(Language lang) {
        if (lang != null) {
            language = lang;
        }
    }

    static Language getLanguage() {
        return language;
    }

    static Language defaultLanguageFor(Locale locale) {
        if (locale != null) {
            String lang = locale.getLanguage();
            if (lang != null && lang.toLowerCase(Locale.ROOT).startsWith("zh")) {
                return Language.ZH;
            }
        }
        return Language.EN;
    }

    static String t(String key) {
        Map<String, String> map = DICT.get(language);
        if (map == null) {
            return key;
        }
        return map.getOrDefault(key, key);
    }

    static String f(String key, Object... args) {
        return MessageFormat.format(t(key), args);
    }

    private static Map<Language, Map<String, String>> buildDict() {
        Map<String, String> zh = new HashMap<>();
        Map<String, String> en = new HashMap<>();

        zh.put("app.title", "BigComp 门禁控制系统");
        en.put("app.title", "BigComp Access Control System");

        zh.put("menu.file", "文件");
        en.put("menu.file", "File");
        zh.put("menu.refresh", "刷新");
        en.put("menu.refresh", "Refresh");
        zh.put("menu.exit", "退出");
        en.put("menu.exit", "Exit");
        zh.put("menu.help", "帮助");
        en.put("menu.help", "Help");
        zh.put("menu.about", "关于");
        en.put("menu.about", "About");

        zh.put("menu.language", "语言");
        en.put("menu.language", "Language");
        zh.put("menu.lang.zh", "中文");
        en.put("menu.lang.zh", "Chinese");
        zh.put("menu.lang.en", "English");
        en.put("menu.lang.en", "English");

        zh.put("tab.users", "用户");
        en.put("tab.users", "Users");
        zh.put("tab.resources", "资源");
        en.put("tab.resources", "Resources");
        zh.put("tab.groups", "资源组");
        en.put("tab.groups", "Groups");
        zh.put("tab.profiles", "配置文件");
        en.put("tab.profiles", "Profiles");
        zh.put("tab.monitor", "实时监控");
        en.put("tab.monitor", "Monitor");
        zh.put("tab.logs", "日志");
        en.put("tab.logs", "Logs");
        zh.put("tab.simulation", "事件模拟");
        en.put("tab.simulation", "Simulation");

        zh.put("status.ready", "就绪  |  {0}");
        en.put("status.ready", "Ready  |  {0}");

        zh.put("about.title", "关于");
        en.put("about.title", "About");
        zh.put("about.body", "BigComp 门禁控制系统 v1.0\n\n综合门禁控制与管理系统");
        en.put("about.body", "BigComp Access Control System v1.0\n\nComprehensive access control and management system");

        zh.put("common.error", "错误");
        en.put("common.error", "Error");
        zh.put("common.success", "成功");
        en.put("common.success", "Success");
        zh.put("common.warning", "警告");
        en.put("common.warning", "Warning");
        zh.put("common.info", "提示");
        en.put("common.info", "Info");
        zh.put("common.none", "无");
        en.put("common.none", "None");
        zh.put("common.selected", "已选择");
        en.put("common.selected", "Selected");
        zh.put("common.deselected", "已取消选择");
        en.put("common.deselected", "Deselected");
        zh.put("common.enabled", "已启用");
        en.put("common.enabled", "Enabled");
        zh.put("common.disabled", "已禁用");
        en.put("common.disabled", "Disabled");
        zh.put("common.colon", "：");
        en.put("common.colon", ": ");
        zh.put("common.unknown", "未知");
        en.put("common.unknown", "Unknown");
        zh.put("common.active", "启用");
        en.put("common.active", "Active");
        zh.put("common.inactive", "禁用");
        en.put("common.inactive", "Inactive");
        zh.put("common.custom", "自定义");
        en.put("common.custom", "Custom");
        zh.put("common.confirmDelete.title", "确认删除");
        en.put("common.confirmDelete.title", "Confirm Delete");
        zh.put("common.ok", "确定");
        en.put("common.ok", "OK");
        zh.put("common.cancel", "取消");
        en.put("common.cancel", "Cancel");
        zh.put("common.refresh", "刷新");
        en.put("common.refresh", "Refresh");
        zh.put("common.add", "添加");
        en.put("common.add", "Add");
        zh.put("common.remove", "移除");
        en.put("common.remove", "Remove");
        zh.put("common.save", "保存");
        en.put("common.save", "Save");
        zh.put("common.selectAll", "全选");
        en.put("common.selectAll", "Select All");
        zh.put("common.deselectAll", "全不选");
        en.put("common.deselectAll", "Deselect All");
        zh.put("common.clear", "清空");
        en.put("common.clear", "Clear");

        zh.put("user.title", "用户");
        en.put("user.title", "User");
        zh.put("user.firstName", "名：");
        en.put("user.firstName", "First Name:");
        zh.put("user.lastName", "姓：");
        en.put("user.lastName", "Last Name:");
        zh.put("user.gender", "性别：");
        en.put("user.gender", "Gender:");
        zh.put("user.type", "类型：");
        en.put("user.type", "Type:");

        zh.put("user.gender.MALE", "男");
        en.put("user.gender.MALE", "Male");
        zh.put("user.gender.FEMALE", "女");
        en.put("user.gender.FEMALE", "Female");
        zh.put("user.gender.OTHER", "其他");
        en.put("user.gender.OTHER", "Other");

        zh.put("user.type.EMPLOYEE", "员工");
        en.put("user.type.EMPLOYEE", "Employee");
        zh.put("user.type.CONTRACTOR", "承包商");
        en.put("user.type.CONTRACTOR", "Contractor");
        zh.put("user.type.INTERN", "实习生");
        en.put("user.type.INTERN", "Intern");
        zh.put("user.type.VISITOR", "访客");
        en.put("user.type.VISITOR", "Visitor");
        zh.put("user.type.PROJECT_MANAGER", "项目经理");
        en.put("user.type.PROJECT_MANAGER", "Project Manager");

        zh.put("user.action.add", "添加用户");
        en.put("user.action.add", "Add User");
        zh.put("user.action.delete", "删除用户");
        en.put("user.action.delete", "Delete User");
        zh.put("user.action.createBadge", "创建徽章");
        en.put("user.action.createBadge", "Create Badge");
        zh.put("user.action.assignProfile", "分配配置文件");
        en.put("user.action.assignProfile", "Assign Profile");
        zh.put("user.action.autoAssign", "自动分配");
        en.put("user.action.autoAssign", "Auto-assign");

        zh.put("user.col.id", "ID");
        en.put("user.col.id", "ID");
        zh.put("user.col.name", "姓名");
        en.put("user.col.name", "Name");
        zh.put("user.col.gender", "性别");
        en.put("user.col.gender", "Gender");
        zh.put("user.col.type", "类型");
        en.put("user.col.type", "Type");
        zh.put("user.col.badgeId", "徽章ID");
        en.put("user.col.badgeId", "Badge ID");

        zh.put("user.msg.enterName", "请输入姓名");
        en.put("user.msg.enterName", "Please enter name");
        zh.put("user.msg.added", "用户添加成功");
        en.put("user.msg.added", "User added successfully");
        zh.put("user.msg.addFailed", "添加用户失败：{0}");
        en.put("user.msg.addFailed", "Failed to add user: {0}");
        zh.put("user.msg.selectDelete", "请选择要删除的用户");
        en.put("user.msg.selectDelete", "Please select a user to delete");
        zh.put("user.msg.confirmDelete", "确定要删除用户“{0}”吗？\n该操作也会删除该用户的徽章及相关配置。");
        en.put("user.msg.confirmDelete", "Are you sure you want to delete user \"{0}\"?\nThis operation will also delete the user's badge and related configurations.");
        zh.put("user.msg.deleted", "用户删除成功");
        en.put("user.msg.deleted", "User deleted successfully");
        zh.put("user.msg.deleteFailed", "删除用户失败：{0}");
        en.put("user.msg.deleteFailed", "Failed to delete user: {0}");
        zh.put("user.msg.selectUser", "请选择一个用户");
        en.put("user.msg.selectUser", "Please select a user");
        zh.put("user.msg.notExist", "用户不存在");
        en.put("user.msg.notExist", "User does not exist");
        zh.put("user.msg.badgeCreatedAutoProfile", "徽章创建成功，已自动分配配置文件");
        en.put("user.msg.badgeCreatedAutoProfile", "Badge created successfully, profile automatically assigned");
        zh.put("user.msg.badgeCreateFailed", "创建徽章失败：{0}");
        en.put("user.msg.badgeCreateFailed", "Failed to create badge: {0}");
        zh.put("user.msg.noBadge", "该用户还没有徽章，请先创建徽章");
        en.put("user.msg.noBadge", "This user does not have a badge yet, please create a badge first");
        zh.put("user.msg.noProfiles", "没有可用的配置文件，请先在配置文件管理中创建");
        en.put("user.msg.noProfiles", "No available profiles, please create profiles in Profile Management first");
        zh.put("user.msg.selectProfile", "请选择要分配给用户的配置文件：");
        en.put("user.msg.selectProfile", "Select profile to assign to user:");
        zh.put("user.msg.profileAlready", "用户已经拥有配置文件“{0}”");
        en.put("user.msg.profileAlready", "User already has profile \"{0}\"");
        zh.put("user.msg.profileAssigned", "已为用户分配配置文件“{0}”\n\n用户当前配置文件：\n{1}");
        en.put("user.msg.profileAssigned", "Profile \"{0}\" has been assigned to user\n\nUser's current profiles:\n{1}");
        zh.put("user.msg.assignProfileFailed", "分配配置文件失败：{0}");
        en.put("user.msg.assignProfileFailed", "Failed to assign profile: {0}");
        zh.put("user.msg.loadFailed", "加载用户失败：{0}");
        en.put("user.msg.loadFailed", "Failed to load users: {0}");
        zh.put("user.msg.autoAssignSummary", "自动分配完成！\n\n统计：\n• 成功分配：{0} 人\n• 跳过（已有配置）：{1} 人\n• 跳过（无徽章）：{2} 人\n• 失败：{3} 人\n\n明细：\n{4}");
        en.put("user.msg.autoAssignSummary", "Auto-assign profiles completed!\n\nStatistics:\n• Successfully assigned: {0} users\n• Skipped (already has profile): {1} users\n• Skipped (no badge): {2} users\n• Failed: {3} users\n\nDetails:\n{4}");
        zh.put("user.msg.autoAssignDetailSuccess", "  ✓ {0} ({1}) -> {2}");
        en.put("user.msg.autoAssignDetailSuccess", "  ✓ {0} ({1}) -> {2}");
        zh.put("user.msg.autoAssignDetailFail", "  ✗ {0} -> 失败: {1}");
        en.put("user.msg.autoAssignDetailFail", "  ✗ {0} -> Failed: {1}");
        zh.put("user.msg.autoAssignFailed", "自动分配失败：{0}");
        en.put("user.msg.autoAssignFailed", "Failed to auto-assign profiles: {0}");

        zh.put("resource.title", "资源");
        en.put("resource.title", "Resource");
        zh.put("resource.name", "名称：");
        en.put("resource.name", "Name:");
        zh.put("resource.type", "类型：");
        en.put("resource.type", "Type:");
        zh.put("resource.location", "位置：");
        en.put("resource.location", "Location:");
        zh.put("resource.building", "楼栋：");
        en.put("resource.building", "Building:");
        zh.put("resource.floor", "楼层：");
        en.put("resource.floor", "Floor:");

        zh.put("resource.type.DOOR", "门");
        en.put("resource.type.DOOR", "Door");
        zh.put("resource.type.GATE", "闸机");
        en.put("resource.type.GATE", "Gate");
        zh.put("resource.type.ELEVATOR", "电梯");
        en.put("resource.type.ELEVATOR", "Elevator");
        zh.put("resource.type.STAIRWAY", "楼梯");
        en.put("resource.type.STAIRWAY", "Stairway");
        zh.put("resource.type.PRINTER", "打印机");
        en.put("resource.type.PRINTER", "Printer");
        zh.put("resource.type.BEVERAGE_DISPENSER", "饮料机");
        en.put("resource.type.BEVERAGE_DISPENSER", "Beverage Dispenser");
        zh.put("resource.type.PARKING", "停车场");
        en.put("resource.type.PARKING", "Parking");

        zh.put("resource.state.CONTROLLED", "受控");
        en.put("resource.state.CONTROLLED", "Controlled");
        zh.put("resource.state.UNCONTROLLED", "不受控");
        en.put("resource.state.UNCONTROLLED", "Uncontrolled");

        zh.put("resource.action.add", "添加资源");
        en.put("resource.action.add", "Add Resource");
        zh.put("resource.action.delete", "删除资源");
        en.put("resource.action.delete", "Delete Resource");
        zh.put("resource.action.createReader", "创建读卡器");
        en.put("resource.action.createReader", "Create Badge Reader");
        zh.put("resource.action.createReadersAll", "批量创建读卡器");
        en.put("resource.action.createReadersAll", "Create Readers (All)");
        zh.put("resource.action.linkGroup", "关联资源组");
        en.put("resource.action.linkGroup", "Link to Group");

        zh.put("resource.col.id", "ID");
        en.put("resource.col.id", "ID");
        zh.put("resource.col.name", "名称");
        en.put("resource.col.name", "Name");
        zh.put("resource.col.type", "类型");
        en.put("resource.col.type", "Type");
        zh.put("resource.col.location", "位置");
        en.put("resource.col.location", "Location");
        zh.put("resource.col.building", "楼栋");
        en.put("resource.col.building", "Building");
        zh.put("resource.col.floor", "楼层");
        en.put("resource.col.floor", "Floor");
        zh.put("resource.col.state", "状态");
        en.put("resource.col.state", "State");

        zh.put("resource.msg.enterName", "请输入资源名称");
        en.put("resource.msg.enterName", "Please enter resource name");
        zh.put("resource.msg.added", "资源添加成功");
        en.put("resource.msg.added", "Resource added successfully");
        zh.put("resource.msg.addFailed", "添加资源失败：{0}");
        en.put("resource.msg.addFailed", "Failed to add resource: {0}");
        zh.put("resource.msg.selectDelete", "请选择要删除的资源");
        en.put("resource.msg.selectDelete", "Please select a resource to delete");
        zh.put("resource.msg.confirmDelete", "确定要删除资源“{0}”吗？\n该操作也会删除关联的读卡器与资源组关系。");
        en.put("resource.msg.confirmDelete", "Are you sure you want to delete resource \"{0}\"?\nThis operation will also delete associated badge readers and resource group associations.");
        zh.put("resource.msg.deleted", "资源删除成功");
        en.put("resource.msg.deleted", "Resource deleted successfully");
        zh.put("resource.msg.deleteFailed", "删除资源失败：{0}");
        en.put("resource.msg.deleteFailed", "Failed to delete resource: {0}");
        zh.put("resource.msg.selectResource", "请选择一个资源");
        en.put("resource.msg.selectResource", "Please select a resource");
        zh.put("resource.msg.readerExistsConfirm", "资源“{0}”已经存在读卡器：{1}\n\n是否创建新的读卡器？这将替换现有读卡器。");
        en.put("resource.msg.readerExistsConfirm", "Resource \"{0}\" already has a badge reader: {1}\n\nDo you want to create a new badge reader? This will replace the existing one.");
        zh.put("resource.msg.readerCreated", "读卡器创建成功");
        en.put("resource.msg.readerCreated", "Badge reader created successfully");
        zh.put("resource.msg.readerCreateFailed", "创建读卡器失败：{0}");
        en.put("resource.msg.readerCreateFailed", "Failed to create badge reader: {0}");
        zh.put("resource.msg.noResources", "没有可用资源，请先添加资源");
        en.put("resource.msg.noResources", "No available resources, please add resources first");
        zh.put("resource.msg.allHaveReaders", "所有资源都已配置读卡器！\n\n资源总数：{0}\n已配置读卡器：{1}");
        en.put("resource.msg.allHaveReaders", "All resources already have badge readers configured!\n\nTotal resources: {0}\nWith badge readers: {1}");
        zh.put("resource.action.createReadersAll", "为全部资源创建读卡器");
        en.put("resource.action.createReadersAll", "Create Badge Readers for All Resources");
        zh.put("resource.msg.createReadersConfirm", "将为 {0} 个资源创建读卡器\n\n统计：\n• 资源总数：{1}\n• 已有读卡器：{2}\n• 待创建：{0}\n\n继续吗？");
        en.put("resource.msg.createReadersConfirm", "Will create badge readers for {0} resources\n\nStatistics:\n• Total resources: {1}\n• Already have badge readers: {2}\n• Need to create: {0}\n\nContinue?");
        zh.put("resource.msg.createReadersDone", "读卡器创建完成！\n\n统计：\n• 成功创建：{0}\n• 跳过（已存在）：{1}\n• 失败：{2}\n\n明细：\n{3}");
        en.put("resource.msg.createReadersDone", "Badge reader creation completed!\n\nStatistics:\n• Successfully created: {0}\n• Skipped (already exists): {1}\n• Failed: {2}\n\nDetails:\n{3}");
        zh.put("resource.msg.createReadersTitle", "批量创建完成");
        en.put("resource.msg.createReadersTitle", "Batch Badge Reader Creation Complete");
        zh.put("resource.msg.noGroupsHelp", "没有可用资源组。\n\n请先在 data/groups/ 目录中创建资源组 JSON 文件。\n示例：{\"name\":\"Office Area\",\"securityLevel\":1,\"resources\":[]}\n\n创建后请重启程序或刷新资源组。");
        en.put("resource.msg.noGroupsHelp", "No available resource groups.\n\nPlease create resource group JSON files in data/groups/ directory first.\nExample: {\"name\": \"Office Area\", \"securityLevel\": 1, \"resources\": []}\n\nAfter creating, please restart the program or refresh resource groups.");
        zh.put("resource.msg.selectGroup", "请选择要将资源“{0}”关联到的资源组：");
        en.put("resource.msg.selectGroup", "Select resource group to link resource \"{0}\" to:");
        zh.put("resource.action.linkGroup", "关联资源组");
        en.put("resource.action.linkGroup", "Link to Resource Group");
        zh.put("resource.msg.linkedToGroup", "资源已关联到资源组“{0}”\n\n资源ID：{1}\n现在可以在配置文件中为该资源组配置访问权限。");
        en.put("resource.msg.linkedToGroup", "Resource has been linked to resource group \"{0}\"\n\nResource ID: {1}\nYou can now configure access permissions for this resource group in profiles.");
        zh.put("resource.msg.linkFailed", "关联资源组失败：{0}");
        en.put("resource.msg.linkFailed", "Failed to link resource group: {0}");
        zh.put("resource.msg.loadFailed", "加载资源失败：{0}");
        en.put("resource.msg.loadFailed", "Failed to load resources: {0}");

        zh.put("group.title.list", "资源组");
        en.put("group.title.list", "Resource Groups");
        zh.put("group.title.details", "资源组详情");
        en.put("group.title.details", "Group Details");
        zh.put("group.title.available", "可用资源");
        en.put("group.title.available", "Available Resources");
        zh.put("group.field.name", "资源组名称：");
        en.put("group.field.name", "Resource Group Name:");
        zh.put("group.field.security", "安全等级：");
        en.put("group.field.security", "Security Level:");
        zh.put("group.action.new", "新建");
        en.put("group.action.new", "New Group");
        zh.put("group.action.autoCreate", "自动创建");
        en.put("group.action.autoCreate", "Auto-create");
        zh.put("group.action.delete", "删除资源组");
        en.put("group.action.delete", "Delete Resource Group");
        zh.put("group.action.uncontrolled", "设为非受控");
        en.put("group.action.uncontrolled", "Set UNCONTROLLED");
        zh.put("group.action.controlled", "设为受控");
        en.put("group.action.controlled", "Set CONTROLLED");
        zh.put("group.tip.uncontrolled", "将本组所有资源设为 UNCONTROLLED（紧急放行）");
        en.put("group.tip.uncontrolled", "Set all resources in this group to UNCONTROLLED state (Emergency Open)");
        zh.put("group.tip.controlled", "将本组所有资源设为 CONTROLLED（正常）");
        en.put("group.tip.controlled", "Set all resources in this group to CONTROLLED state (Normal)");
        zh.put("group.action.addSelected", "添加选中");
        en.put("group.action.addSelected", "Add Selected");
        zh.put("group.col.resourceId", "资源ID");
        en.put("group.col.resourceId", "Resource ID");
        zh.put("group.col.resourceName", "资源名称");
        en.put("group.col.resourceName", "Resource Name");
        zh.put("group.col.type", "类型");
        en.put("group.col.type", "Type");
        zh.put("group.col.location", "位置");
        en.put("group.col.location", "Location");
        zh.put("group.col.status", "状态");
        en.put("group.col.status", "Status");
        zh.put("group.status.added", "已添加");
        en.put("group.status.added", "Added");
        zh.put("group.status.notAdded", "未添加");
        en.put("group.status.notAdded", "Not Added");
        zh.put("group.info.details", "资源组：{0}\n安全等级：{1}\n资源数量：{2}\n文件路径：{3}");
        en.put("group.info.details", "Resource Group: {0}\nSecurity Level: {1}\nResource Count: {2}\nFile Path: {3}");
        zh.put("group.info.notSaved", "未保存");
        en.put("group.info.notSaved", "Not saved");
        zh.put("group.msg.enterName", "请输入资源组名称：");
        en.put("group.msg.enterName", "Please enter resource group name:");
        zh.put("group.msg.newTitle", "新建资源组");
        en.put("group.msg.newTitle", "New Resource Group");
        zh.put("group.msg.created", "资源组创建成功");
        en.put("group.msg.created", "Resource group created successfully");
        zh.put("group.msg.createFailed", "创建资源组失败：{0}");
        en.put("group.msg.createFailed", "Failed to create resource group: {0}");
        zh.put("group.msg.selectDelete", "请从左侧列表中选择要删除的资源组");
        en.put("group.msg.selectDelete", "Please select a resource group from the left list to delete");
        zh.put("group.msg.confirmDelete", "确定要删除资源组“{0}”吗？\n\n资源组信息：\n• 名称：{1}\n• 安全等级：{2}\n• 资源数量：{3}\n\n警告：此操作将：\n• 删除资源组 JSON 文件\n• 删除数据库中的关联记录\n• 此操作无法撤销！");
        en.put("group.msg.confirmDelete", "Are you sure you want to delete resource group \"{0}\"?\n\nResource Group Information:\n• Name: {1}\n• Security Level: {2}\n• Resource Count: {3}\n\nWarning: This operation will:\n• Delete resource group JSON file\n• Delete associated records in database\n• This operation cannot be undone!");
        zh.put("group.msg.confirmDeleteTitle", "确认删除资源组");
        en.put("group.msg.confirmDeleteTitle", "Confirm Delete Resource Group");
        zh.put("group.msg.deleted", "资源组删除成功");
        en.put("group.msg.deleted", "Resource group deleted successfully");
        zh.put("group.msg.deleteFailed", "删除资源组失败：{0}");
        en.put("group.msg.deleteFailed", "Failed to delete resource group: {0}");
        zh.put("group.msg.noGroupSelected", "请选择或先创建一个资源组");
        en.put("group.msg.noGroupSelected", "Please select or create a resource group first");
        zh.put("group.msg.resourceAlreadyIn", "该资源已在此资源组中");
        en.put("group.msg.resourceAlreadyIn", "This resource is already in this resource group");
        zh.put("group.msg.resourceAdded", "资源已添加到资源组");
        en.put("group.msg.resourceAdded", "Resource added to resource group");
        zh.put("group.msg.resourceRemoved", "资源已从资源组移除");
        en.put("group.msg.resourceRemoved", "Resource removed from resource group");
        zh.put("group.msg.confirmRemove", "确定要从资源组中移除资源“{0}”吗？");
        en.put("group.msg.confirmRemove", "Are you sure you want to remove resource \"{0}\" from the resource group?");
        zh.put("group.msg.confirmRemoveTitle", "确认移除");
        en.put("group.msg.confirmRemoveTitle", "Confirm Remove");
        zh.put("group.msg.addResourceFailed", "添加资源失败：{0}");
        en.put("group.msg.addResourceFailed", "Failed to add resource: {0}");
        zh.put("group.msg.selectToAdd", "请先选择要添加的资源");
        en.put("group.msg.selectToAdd", "Please select resources to add first");
        zh.put("group.msg.addResources.done", "成功将 {0} 个资源添加到资源组\n跳过 {1} 个已存在的资源");
        en.put("group.msg.addResources.done", "Successfully added {0} resources to resource group\nSkipped {1} existing resources");
        zh.put("group.msg.saveSuccess", "资源组保存成功");
        en.put("group.msg.saveSuccess", "Resource group saved successfully");
        zh.put("group.msg.saveFailed", "保存资源组失败：{0}");
        en.put("group.msg.saveFailed", "Failed to save resource group: {0}");
        zh.put("group.msg.confirmStateChange", "确定将资源组“{0}”中的所有资源状态设为 {1} 吗？");
        en.put("group.msg.confirmStateChange", "Are you sure you want to set all resources in group \"{0}\" to {1}?");
        zh.put("group.msg.confirmStateChangeTitle", "确认状态变更");
        en.put("group.msg.confirmStateChangeTitle", "Confirm State Change");
        zh.put("group.msg.updateStateSuccess", "已成功将 {0} 个资源状态更新为 {1}");
        en.put("group.msg.updateStateSuccess", "Successfully updated {0} resources to {1}");
        zh.put("group.msg.updateResourcesFailed", "更新资源失败：{0}");
        en.put("group.msg.updateResourcesFailed", "Failed to update resources: {0}");
        zh.put("group.msg.noResources", "没有可用资源，请先在资源管理中创建资源");
        en.put("group.msg.noResources", "No available resources, please create resources in Resource Management first");
        zh.put("group.msg.selectStrategy", "请选择资源组创建策略：");
        en.put("group.msg.selectStrategy", "Select resource group creation strategy:");
        zh.put("group.msg.autoCreateTitle", "自动创建资源组");
        en.put("group.msg.autoCreateTitle", "Auto-create Resource Groups");
        zh.put("group.msg.autoCreate.done", "成功创建 {0} 个资源组\n跳过 {1} 个已存在的资源组");
        en.put("group.msg.autoCreate.done", "Successfully created {0} resource groups\nSkipped {1} existing resource groups");
        zh.put("group.msg.autoCreateError", "创建资源组时出错：{0}");
        en.put("group.msg.autoCreateError", "Error creating resource groups: {0}");
        zh.put("group.strategy.building", "按楼栋分组");
        en.put("group.strategy.building", "Group by Building");
        zh.put("group.strategy.floor", "按楼层分组");
        en.put("group.strategy.floor", "Group by Floor");
        zh.put("group.strategy.floor.pattern", "{0} 层");
        en.put("group.strategy.floor.pattern", "Floor {0}");
        zh.put("group.strategy.buildingFloor.pattern", "{0} - {1} 层");
        en.put("group.strategy.buildingFloor.pattern", "{0} - Floor {1}");
        zh.put("group.strategy.floorType.pattern", "{0} 层 - {1}");
        en.put("group.strategy.floorType.pattern", "Floor {0} - {1}");
        zh.put("group.strategy.type", "按资源类型分组");
        en.put("group.strategy.type", "Group by Resource Type");
        zh.put("group.strategy.buildingFloor", "按楼栋 + 楼层分组");
        en.put("group.strategy.buildingFloor", "Group by Building + Floor");
        zh.put("group.strategy.buildingType", "按楼栋 + 类型分组");
        en.put("group.strategy.buildingType", "Group by Building + Type");
        zh.put("group.strategy.buildingType.pattern", "{0} - {1}");
        en.put("group.strategy.buildingType.pattern", "{0} - {1}");
        zh.put("group.strategy.floorType", "按楼层 + 类型分组");
        en.put("group.strategy.floorType", "Group by Floor + Type");
        zh.put("group.strategy.all", "创建所有可能的组合");
        en.put("group.strategy.all", "Create All Possible Combinations");

        zh.put("profile.title.list", "配置文件");
        en.put("profile.title.list", "Profiles");
        zh.put("profile.title.rights", "访问权限");
        en.put("profile.title.rights", "Access Rights");
        zh.put("profile.title.timeFilter", "时间过滤");
        en.put("profile.title.timeFilter", "Time Filter");
        zh.put("profile.title.profile", "配置文件信息");
        en.put("profile.title.profile", "Profile");

        zh.put("profile.field.name", "名称：");
        en.put("profile.field.name", "Name:");
        zh.put("profile.field.group", "资源组：");
        en.put("profile.field.group", "Resource Group:");
        zh.put("profile.field.days", "允许的工作日：");
        en.put("profile.field.days", "Allowed Days of Week:");
        zh.put("profile.field.excludeDays", "排除所选日期（而不是允许）");
        en.put("profile.field.excludeDays", "Exclude selected days (instead of allowing)");
        zh.put("profile.field.timeRange", "时间范围：");
        en.put("profile.field.timeRange", "Time Range:");
        zh.put("profile.field.excludeTime", "排除时间范围（而不是允许）");
        en.put("profile.field.excludeTime", "Exclude time range (instead of allowing)");
        zh.put("profile.text.timeHelp", "注意：如果未选择日期，则允许所有日期。如果未设置时间范围，则允许所有时间。");
        en.put("profile.text.timeHelp", "Note: If no days are selected, all days are allowed. If no time range is set, all times are allowed.");
        zh.put("profile.title.editTime", "编辑时间过滤器");
        en.put("profile.title.editTime", "Edit Time Filter");
        zh.put("profile.title.restore", "恢复权限配置");
        en.put("profile.title.restore", "Restore Profile");
        zh.put("profile.col.group", "资源组");
        en.put("profile.col.group", "Resource Group");
        zh.put("profile.col.timeFilter", "时间过滤规则");
        en.put("profile.col.timeFilter", "Time Filter");

        zh.put("profile.action.new", "新建");
        en.put("profile.action.new", "New");
        zh.put("profile.action.modify", "修改");
        en.put("profile.action.modify", "Modify");
        zh.put("profile.action.delete", "删除");
        en.put("profile.action.delete", "Delete");
        zh.put("profile.action.restore", "恢复");
        en.put("profile.action.restore", "Restore");
        zh.put("profile.action.refreshGroups", "刷新资源组");
        en.put("profile.action.refreshGroups", "Refresh Groups");
        zh.put("profile.action.addRight", "添加权限");
        en.put("profile.action.addRight", "Add Right");
        zh.put("profile.action.removeRight", "移除权限");
        en.put("profile.action.removeRight", "Remove Right");
        zh.put("profile.action.editTimeFilter", "编辑时间过滤");
        en.put("profile.action.editTimeFilter", "Edit Time Filter");
        zh.put("profile.action.save", "保存");
        en.put("profile.action.save", "Save");

        zh.put("profile.msg.noGroups", "（没有资源组，请先在 data/groups/ 创建）");
        en.put("profile.msg.noGroups", "(No resource groups, please create in data/groups/ directory)");
        zh.put("profile.msg.enterName", "请输入配置文件名称：");
        en.put("profile.msg.enterName", "Please enter profile name:");
        zh.put("profile.msg.createFailed", "创建配置文件失败：{0}");
        en.put("profile.msg.createFailed", "Failed to create profile: {0}");
        zh.put("profile.msg.selectDelete", "请选择要删除的配置文件");
        en.put("profile.msg.selectDelete", "Please select a profile to delete");
        zh.put("profile.msg.confirmDelete", "确定要删除配置文件“{0}”吗？");
        en.put("profile.msg.confirmDelete", "Are you sure you want to delete profile \"{0}\"?");
        zh.put("profile.msg.deleted", "配置文件删除成功");
        en.put("profile.msg.deleted", "Profile deleted successfully");
        zh.put("profile.msg.deleteFailed", "删除配置文件失败：{0}");
        en.put("profile.msg.deleteFailed", "Failed to delete profile: {0}");

        zh.put("profile.msg.refreshed", "资源组列表已刷新");
        en.put("profile.msg.refreshed", "Resource group list refreshed");
        zh.put("profile.msg.days", "工作日：{0}");
        en.put("profile.msg.days", "Days: {0}");
        zh.put("profile.msg.timeRanges", "时间段：{0} 个");
        en.put("profile.msg.timeRanges", "Time Ranges: {0} range(s)");
        zh.put("profile.msg.noRestrictions", "无限制");
        en.put("profile.msg.noRestrictions", "No restrictions");
        zh.put("profile.msg.selectProfileFirst", "请先选择或创建一个权限配置");
        en.put("profile.msg.selectProfileFirst", "Please select or create a profile first");
        zh.put("profile.msg.noGroupsHint", "请先创建资源组。\n\n资源组文件应以 JSON 格式放置在 data/groups/ 目录中。\n示例：{\"name\": \"办公区\", \"securityLevel\": 1, \"resources\": [\"资源ID\"]}\n\n创建后，请点击“刷新”按钮重新加载。");
        en.put("profile.msg.noGroupsHint", "Please create resource groups first.\n\nResource group files should be placed in data/groups/ directory, in JSON format.\nExample: {\"name\": \"Office Area\", \"securityLevel\": 1, \"resources\": [\"resource-id\"]}\n\nAfter creating, please click the \"Refresh\" button to reload.");
        zh.put("profile.msg.overwriteConfirm", "该资源组的访问权限已存在。是否覆盖？");
        en.put("profile.msg.overwriteConfirm", "Access right for this resource group already exists. Overwrite?");
        zh.put("profile.msg.rightAdded", "访问权限已添加");
        en.put("profile.msg.rightAdded", "Access right added");
        zh.put("profile.msg.saveProfileFailed", "保存配置文件失败：{0}");
        en.put("profile.msg.saveProfileFailed", "Failed to save profile: {0}");
        zh.put("profile.msg.selectRightToDelete", "请选择要删除的访问权限");
        en.put("profile.msg.selectRightToDelete", "Please select an access right to delete");
        zh.put("profile.msg.confirmDeleteRight", "确定要删除资源组“{0}”的访问权限吗？");
        en.put("profile.msg.confirmDeleteRight", "Are you sure you want to delete access right for resource group \"{0}\"?");
        zh.put("profile.msg.rightDeleted", "访问权限已删除");
        en.put("profile.msg.rightDeleted", "Access right deleted");
        zh.put("profile.msg.selectRightToEdit", "请选择要编辑的访问权限");
        en.put("profile.msg.selectRightToEdit", "Please select an access right to edit");
        zh.put("profile.msg.selectProfileToModify", "请选择要修改的权限配置");
        en.put("profile.msg.selectProfileToModify", "Please select a profile to modify");
        zh.put("profile.msg.notExist", "权限配置不存在");
        en.put("profile.msg.notExist", "Profile does not exist");
        zh.put("profile.msg.backupFailed", "创建备份失败：{0}");
        en.put("profile.msg.backupFailed", "Failed to create backup: {0}");
        zh.put("profile.msg.enterNewName", "请输入新的权限配置名称（留空则保持原名）：");
        en.put("profile.msg.enterNewName", "Please enter new profile name (leave empty to keep original name):");
        zh.put("profile.msg.modifySuccess", "权限配置修改成功");
        en.put("profile.msg.modifySuccess", "Profile modified successfully");
        zh.put("profile.msg.modifyFailed", "修改权限配置失败：{0}");
        en.put("profile.msg.modifyFailed", "Failed to modify profile: {0}");
        zh.put("profile.msg.selectProfileToRestore", "请选择要恢复的权限配置");
        en.put("profile.msg.selectProfileToRestore", "Please select a profile to restore");
        zh.put("profile.msg.backupDirNotFound", "未找到备份目录");
        en.put("profile.msg.backupDirNotFound", "Backup directory not found");
        zh.put("profile.msg.noBackupsFound", "未找到该配置的备份文件");
        en.put("profile.msg.noBackupsFound", "No backups found for this profile");
        zh.put("profile.msg.backupTime", "备份时间：{0}");
        en.put("profile.msg.backupTime", "Backup time: {0}");
        zh.put("profile.msg.selectBackup", "选择要恢复的备份：");
        en.put("profile.msg.selectBackup", "Select backup to restore:");
        zh.put("profile.msg.confirmRestore", "确定要恢复此备份吗？当前配置将被覆盖。");
        en.put("profile.msg.confirmRestore", "Are you sure you want to restore this backup? Current configuration will be overwritten.");
        zh.put("profile.msg.confirmRestoreTitle", "确认恢复");
        en.put("profile.msg.confirmRestoreTitle", "Confirm Restore");
        zh.put("profile.msg.restoreSuccess", "权限配置恢复成功");
        en.put("profile.msg.restoreSuccess", "Profile restored successfully");
        zh.put("profile.msg.restoreFailedRead", "恢复失败：无法读取备份文件");
        en.put("profile.msg.restoreFailedRead", "Restore failed: Unable to read backup file");
        zh.put("profile.msg.restoreFailed", "恢复权限配置失败：{0}");
        en.put("profile.msg.restoreFailed", "Failed to restore profile: {0}");
        zh.put("profile.msg.selectProfileToSave", "请选择要保存的权限配置");
        en.put("profile.msg.selectProfileToSave", "Please select a profile to save");
        zh.put("profile.msg.nameEmpty", "权限配置名称不能为空");
        en.put("profile.msg.nameEmpty", "Profile name cannot be empty");
        zh.put("profile.msg.saveSuccess", "权限配置保存成功");
        en.put("profile.msg.saveSuccess", "Profile saved successfully");
        zh.put("profile.msg.saveFailed", "保存权限配置失败：{0}");
        en.put("profile.msg.saveFailed", "Failed to save profile: {0}");
        zh.put("profile.text.to", " 至 ");
        en.put("profile.text.to", " to ");
        zh.put("profile.day.mon", "一");
        en.put("profile.day.mon", "Mon");
        zh.put("profile.day.tue", "二");
        en.put("profile.day.tue", "Tue");
        zh.put("profile.day.wed", "三");
        en.put("profile.day.wed", "Wed");
        zh.put("profile.day.thu", "四");
        en.put("profile.day.thu", "Thu");
        zh.put("profile.day.fri", "五");
        en.put("profile.day.fri", "Fri");
        zh.put("profile.day.sat", "六");
        en.put("profile.day.sat", "Sat");
        zh.put("profile.day.sun", "日");
        en.put("profile.day.sun", "Sun");

        zh.put("common.unit.mb", "MB");
        en.put("common.unit.mb", "MB");
        zh.put("common.unit.kb", "KB");
        en.put("common.unit.kb", "KB");
        zh.put("log.title.search", "查询条件");
        en.put("log.title.search", "Search");
        zh.put("log.title.records", "日志记录");
        en.put("log.title.records", "Records");
        zh.put("log.field.startDate", "开始日期 (yyyy-MM-dd)：");
        en.put("log.field.startDate", "Start Date (yyyy-MM-dd):");
        zh.put("log.field.endDate", "结束日期 (yyyy-MM-dd)：");
        en.put("log.field.endDate", "End Date (yyyy-MM-dd):");
        zh.put("log.field.badgeCode", "徽章码：");
        en.put("log.field.badgeCode", "Badge Code:");
        zh.put("log.field.resourceId", "资源ID：");
        en.put("log.field.resourceId", "Resource ID:");
        zh.put("log.field.userId", "用户ID：");
        en.put("log.field.userId", "User ID:");
        zh.put("log.field.status", "状态：");
        en.put("log.field.status", "Status:");
        zh.put("log.action.search", "查询");
        en.put("log.action.search", "Search");
        zh.put("log.action.clearCriteria", "清空条件");
        en.put("log.action.clearCriteria", "Clear Criteria");
        zh.put("log.action.export", "导出日志");
        en.put("log.action.export", "Export Logs");
        zh.put("log.action.clearLogs", "清空日志");
        en.put("log.action.clearLogs", "Clear Logs");
        zh.put("log.msg.noLogs", "未找到日志文件");
        en.put("log.msg.noLogs", "No log files found");
        zh.put("log.msg.clearLogs.confirm", "即将删除 {0} 个日志文件（总大小：{1}）\n\n确定要继续吗？");
        en.put("log.msg.clearLogs.confirm", "About to delete {0} log files (Total size: {1})\n\nAre you sure you want to continue?");
        zh.put("log.msg.clearLogs.confirmTitle", "最终确认");
        en.put("log.msg.clearLogs.confirmTitle", "Final Confirmation");
        zh.put("log.msg.clearLogs.done", "日志清理完成！\n\n统计：\n• 成功删除：{0} 个文件\n• 失败：{1} 个文件");
        en.put("log.msg.clearLogs.done", "Log clearing completed!\n\nStatistics:\n• Successfully deleted: {0} files\n• Failed: {1} files");
        zh.put("log.msg.clearLogs.doneTitle", "清理完成");
        en.put("log.msg.clearLogs.doneTitle", "Clear Complete");
        zh.put("log.msg.clearLogs.error", "清理日志失败：{0}");
        en.put("log.msg.clearLogs.error", "Failed to clear logs: {0}");
        zh.put("log.msg.noMatches", "未找到匹配的日志记录");
        en.put("log.msg.noMatches", "No matching log records found");
        zh.put("log.msg.searchResultTitle", "查询结果");
        en.put("log.msg.searchResultTitle", "Search Results");
        zh.put("log.msg.searchComplete", "已找到 {0} 条日志记录");
        en.put("log.msg.searchComplete", "Found {0} log records");
        zh.put("log.msg.searchCompleteTitle", "查询完成");
        en.put("log.msg.searchCompleteTitle", "Search Complete");
        zh.put("log.msg.searchError", "查询日志失败：{0}");
        en.put("log.msg.searchError", "Failed to search logs: {0}");
        zh.put("log.msg.searchErrorTitle", "查询错误");
        en.put("log.msg.searchErrorTitle", "Search Error");
        zh.put("log.msg.dateFormatError", "日期格式错误，请使用 yyyy-MM-dd 格式");
        en.put("log.msg.dateFormatError", "Date format error, please use yyyy-MM-dd format");
        zh.put("log.msg.exportDataError", "获取日志数据失败：{0}");
        en.put("log.msg.exportDataError", "Failed to get log data: {0}");
        zh.put("log.msg.noExportData", "没有可导出的日志数据");
        en.put("log.msg.noExportData", "No log data to export");
        zh.put("log.msg.exportTitle", "导出日志");
        en.put("log.msg.exportTitle", "Export Logs");
        zh.put("log.msg.exportSuccess", "成功导出 {0} 条日志记录到：\n{1}");
        en.put("log.msg.exportSuccess", "Successfully exported {0} log records to:\n{1}");
        zh.put("log.msg.exportSuccessTitle", "导出成功");
        en.put("log.msg.exportSuccessTitle", "Export Successful");
        zh.put("log.msg.exportError", "导出日志失败：{0}");
        en.put("log.msg.exportError", "Failed to export logs: {0}");
        zh.put("log.csv.header", "时间,徽章码,读卡器ID,资源ID,用户ID,用户姓名,状态");
        en.put("log.csv.header", "Time,Badge Code,Badge Reader ID,Resource ID,User ID,User Name,Status");
        zh.put("log.csv.granted", "通过");
        en.put("log.csv.granted", "Granted");
        zh.put("log.csv.denied", "拒绝");
        en.put("log.csv.denied", "Denied");
        zh.put("log.msg.selectRecordFirst", "请先选择一条日志记录");
        en.put("log.msg.selectRecordFirst", "Please select a log record first");
        zh.put("log.msg.selectRecordFirstTitle", "警告");
        en.put("log.msg.selectRecordFirstTitle", "Warning");
        zh.put("log.diag.title", "权限诊断 - {0}");
        en.put("log.diag.title", "Access Control Diagnosis - {0}");
        zh.put("log.diag.reportTitle", "系统状态报告");
        en.put("log.diag.reportTitle", "System Status Report");
        zh.put("log.msg.clearLogs.confirmInitial", "确定要清空所有日志文件吗？\n\n此操作将：\n• 删除 data/logs/ 目录下的所有日志文件\n• 此操作无法撤销！\n\n是否继续？");
        en.put("log.msg.clearLogs.confirmInitial", "Are you sure you want to clear all log files?\n\nThis operation will:\n• Delete all log files in data/logs/ directory\n• This operation cannot be undone!\n\nContinue?");
        zh.put("log.msg.clearLogs.confirmInitialTitle", "确认清空日志");
        en.put("log.msg.clearLogs.confirmInitialTitle", "Confirm Clear Logs");
        zh.put("log.msg.noLogsDir", "日志目录不存在，无需清理");
        en.put("log.msg.noLogsDir", "Log directory does not exist, no need to clear");
        zh.put("common.close", "关闭");
        en.put("common.close", "Close");
        zh.put("common.info", "信息");
        en.put("common.info", "Info");
        zh.put("log.status.all", "全部");
        en.put("log.status.all", "All");
        zh.put("log.status.granted", "通过");
        en.put("log.status.granted", "Granted");
        zh.put("log.status.denied", "拒绝");
        en.put("log.status.denied", "Denied");
        zh.put("log.col.time", "时间");
        en.put("log.col.time", "Time");
        zh.put("log.col.badgeCode", "徽章码");
        en.put("log.col.badgeCode", "Badge Code");
        zh.put("log.col.readerId", "读卡器ID");
        en.put("log.col.readerId", "Badge Reader ID");
        zh.put("log.col.resourceId", "资源ID");
        en.put("log.col.resourceId", "Resource ID");
        zh.put("log.col.userId", "用户ID");
        en.put("log.col.userId", "User ID");
        zh.put("log.col.userName", "用户姓名");
        en.put("log.col.userName", "User Name");
        zh.put("log.col.status", "状态");
        en.put("log.col.status", "Status");

        zh.put("sim.title.users", "模拟用户");
        en.put("sim.title.users", "Users");
        zh.put("sim.title.readers", "读卡器");
        en.put("sim.title.readers", "Badge Readers");
        zh.put("sim.title.control", "控制面板");
        en.put("sim.title.control", "Control");
        zh.put("sim.action.start", "开始模拟");
        en.put("sim.action.start", "Start Simulation");
        zh.put("sim.action.stop", "停止模拟");
        en.put("sim.action.stop", "Stop Simulation");
        zh.put("sim.field.interval", "事件间隔（秒）：");
        en.put("sim.field.interval", "Event Interval (seconds):");
        zh.put("sim.field.systemTime", "系统时间：");
        en.put("sim.field.systemTime", "System Time:");
        zh.put("sim.action.setTime", "设置时间");
        en.put("sim.action.setTime", "Set Time");
        zh.put("sim.action.resetTime", "重置时间");
        en.put("sim.action.resetTime", "Reset Time");
        zh.put("sim.action.resetStats", "重置统计");
        en.put("sim.action.resetStats", "Reset Statistics");
        zh.put("sim.action.refreshData", "刷新数据");
        en.put("sim.action.refreshData", "Refresh Data");
        zh.put("sim.action.addUser", "添加用户");
        en.put("sim.action.addUser", "Add User");
        zh.put("sim.msg.batchAddUsers.done", "批量添加用户完成！\n\n统计：\n• 成功添加：{0} 人\n• 跳过（已在列表中）：{1} 人\n• 跳过（无徽章）：{2} 人\n\n当前模拟用户总数：{3}");
        en.put("sim.msg.batchAddUsers.done", "Batch add users completed!\n\nStatistics:\n• Successfully added: {0} users\n• Skipped (already in list): {1} users\n• Skipped (no badge): {2} users\n\nCurrent total simulated users: {3}");
        zh.put("sim.msg.batchAddUsers.title", "批量添加完成");
        en.put("sim.msg.batchAddUsers.title", "Batch Add Complete");
        zh.put("sim.action.addAllUsers", "添加全部");
        en.put("sim.action.addAllUsers", "Add All");
        zh.put("sim.action.participateAll", "全部参与");
        en.put("sim.action.participateAll", "Participate All");
        zh.put("sim.action.participateNone", "全部不参与");
        en.put("sim.action.participateNone", "Participate None");
        zh.put("sim.action.enableAll", "全部启用");
        en.put("sim.action.enableAll", "Enable All");
        zh.put("sim.action.disableAll", "全部禁用");
        en.put("sim.action.disableAll", "Disable All");
        zh.put("sim.text.instructions", "说明：\n• “参与”列：勾选表示该读卡器参与事件模拟\n• “状态”列：启用/禁用读卡器，禁用后不会响应任何徽章操作\n• 只有“参与”勾选且“状态”启用的读卡器才会生成模拟事件");
        en.put("sim.text.instructions", "Instructions:\n• 'Participate' column: Check to include this badge reader in event simulation\n• 'Status' column: Enable/disable badge reader, disabled readers won't respond to any badge operations\n• Only badge readers with both 'Participate' checked and 'Status' enabled will generate events in simulation");
        zh.put("sim.user.col.userId", "用户ID");
        en.put("sim.user.col.userId", "User ID");
        zh.put("sim.user.col.name", "姓名");
        en.put("sim.user.col.name", "Name");
        zh.put("sim.user.col.type", "类型");
        en.put("sim.user.col.type", "Type");
        zh.put("sim.user.col.badgeCode", "徽章码");
        en.put("sim.user.col.badgeCode", "Badge Code");
        zh.put("sim.reader.col.participate", "参与");
        en.put("sim.reader.col.participate", "Participate");
        zh.put("sim.reader.col.readerId", "读卡器ID");
        en.put("sim.reader.col.readerId", "Badge Reader ID");
        zh.put("sim.reader.col.resourceId", "资源ID");
        en.put("sim.reader.col.resourceId", "Resource ID");
        zh.put("sim.reader.col.resourceName", "资源名称");
        en.put("sim.reader.col.resourceName", "Resource Name");
        zh.put("sim.reader.col.status", "状态");
        en.put("sim.reader.col.status", "Status");

        zh.put("sim.title.setTime", "设置系统时间");
        en.put("sim.title.setTime", "Set System Time");
        zh.put("sim.field.year", "年：");
        en.put("sim.field.year", "Year:");
        zh.put("sim.field.month", "月：");
        en.put("sim.field.month", "Month:");
        zh.put("sim.field.day", "日：");
        en.put("sim.field.day", "Day:");
        zh.put("sim.field.hour", "时：");
        en.put("sim.field.hour", "Hour:");
        zh.put("sim.field.minute", "分：");
        en.put("sim.field.minute", "Minute:");
        zh.put("sim.action.weekdayMorning", "工作日 8:00");
        en.put("sim.action.weekdayMorning", "Weekday 8:00");
        zh.put("sim.action.weekendMorning", "周末 10:00");
        en.put("sim.action.weekendMorning", "Weekend 10:00");
        zh.put("sim.text.stats", "统计：总事件：{0} | 准许：{1} | 拒绝：{2}");
        en.put("sim.text.stats", "Statistics: Total Events: {0} | Granted: {1} | Denied: {2}");
        zh.put("sim.error.invalidTime", "设置时间失败：");
        en.put("sim.error.invalidTime", "Failed to set time: ");

        zh.put("sim.msg.readerAddedToSim", "读卡器 {0} 已添加到模拟中\n");
        en.put("sim.msg.readerAddedToSim", "Badge reader {0} added to simulation\n");
        zh.put("sim.msg.readerRemovedFromSim", "读卡器 {0} 已从模拟中移除\n");
        en.put("sim.msg.readerRemovedFromSim", "Badge reader {0} removed from simulation\n");
        zh.put("sim.msg.noReadersPrompt", "注意：没有可用的读卡器，请先在资源管理中创建资源和读卡器\n");
        en.put("sim.msg.noReadersPrompt", "Note: No available badge readers, please create resources and badge readers in Resource Management first\n");
        zh.put("sim.msg.loadedReaders", "已加载 {0} 个读卡器（默认全部参与模拟）\n");
        en.put("sim.msg.loadedReaders", "Loaded {0} badge readers (all participate in simulation by default)\n");
        zh.put("sim.msg.addedUser", "已添加模拟用户：{0} (卡号: {1})\n");
        en.put("sim.msg.addedUser", "Added simulated user: {0} (Badge: {1})\n");
        zh.put("sim.msg.userAlreadyInSim", "用户 \"{0}\" 已在模拟列表中\n");
        en.put("sim.msg.userAlreadyInSim", "User \"{0}\" is already in simulation list\n");
        zh.put("sim.msg.removedUser", "已移除模拟用户：{0}\n");
        en.put("sim.msg.removedUser", "Removed simulated user: {0}\n");
        zh.put("sim.msg.simulatorUpdated", "模拟器已更新\n");
        en.put("sim.msg.simulatorUpdated", "Simulator updated\n");
        zh.put("sim.msg.started", "模拟已开始（间隔：{0} 秒，使用 {1} 个读卡器）\n");
        en.put("sim.msg.started", "Simulation started (interval: {0} seconds, using {1} badge readers)\n");
        zh.put("sim.msg.stopped", "模拟已停止\n");
        en.put("sim.msg.stopped", "Simulation stopped\n");
        zh.put("sim.msg.statsReset", "统计信息已重置\n");
        en.put("sim.msg.statsReset", "Statistics reset\n");
        zh.put("sim.msg.timeSet", "系统时间设置为：{0}\n");
        en.put("sim.msg.timeSet", "System time set to: {0}\n");
        zh.put("sim.msg.timeReset", "系统时间已重置为当前时间\n");
        en.put("sim.msg.timeReset", "System time reset to current time\n");
        zh.put("sim.msg.readerEnabled", "读卡器 {0} 已启用\n");
        en.put("sim.msg.readerEnabled", "Badge reader {0} enabled\n");
        zh.put("sim.msg.readerDisabled", "读卡器 {0} 已禁用\n");
        en.put("sim.msg.readerDisabled", "Badge reader {0} disabled\n");
        zh.put("sim.msg.enabledCount", "已启用 {0} 个读卡器\n");
        en.put("sim.msg.enabledCount", "Enabled {0} badge readers\n");
        zh.put("sim.msg.disabledCount", "已禁用 {0} 个读卡器\n");
        en.put("sim.msg.disabledCount", "Disabled {0} badge readers\n");
        zh.put("sim.msg.selectedCount", "已在表格中选择 {0} 个读卡器\n");
        en.put("sim.msg.selectedCount", "Selected {0} badge readers in the table\n");
        zh.put("sim.msg.deselectAll", "已取消选择表格中所有读卡器\n");
        en.put("sim.msg.deselectAll", "Deselected all badge readers in the table\n");

        zh.put("sim.dialog.dataRefreshed", "数据已刷新");
        en.put("sim.dialog.dataRefreshed", "Data refreshed");
        zh.put("sim.dialog.selectUsersToAdd", "请选择要添加的用户");
        en.put("sim.dialog.selectUsersToAdd", "Please select users to add");
        zh.put("sim.dialog.userNoBadge", "用户 \"{0}\" 没有卡，无法添加到模拟。请先为该用户创建卡。");
        en.put("sim.dialog.userNoBadge", "User \"{0}\" has no badge, cannot add to simulation. Please create a badge for the user first.");
        zh.put("sim.dialog.noUsersToRemove", "没有可移除的模拟用户");
        en.put("sim.dialog.noUsersToRemove", "No simulated users to remove");
        zh.put("sim.dialog.selectUserToRemove", "选择要移除的模拟用户：");
        en.put("sim.dialog.selectUserToRemove", "Select simulated user to remove:");
        zh.put("sim.title.removeUser", "移除模拟用户");
        en.put("sim.title.removeUser", "Remove Simulated User");
        zh.put("sim.dialog.alreadyRunning", "模拟已在运行中");
        en.put("sim.dialog.alreadyRunning", "Simulation is already running");
        zh.put("sim.dialog.addUsersFirst", "请先添加模拟用户");
        en.put("sim.dialog.addUsersFirst", "Please add simulated users first");
        zh.put("sim.dialog.noReaders", "没有可用的读卡器参与模拟。\n\n");
        en.put("sim.dialog.noReaders", "No available badge readers to participate in simulation.\n\n");
        zh.put("sim.dialog.readersCheckedButDisabled", "注意：{0} 个读卡器已勾选但未启用。\n请先启用这些读卡器，或勾选其他已启用的读卡器。");
        en.put("sim.dialog.readersCheckedButDisabled", "Note: {0} badge readers are checked but not enabled.\nPlease enable these badge readers first, or check other enabled badge readers.");
        zh.put("sim.dialog.noReadersInstruction", "请：\n1. 在“参与”列中勾选读卡器\n2. 确保这些读卡器的“状态”为启用");
        en.put("sim.dialog.noReadersInstruction", "Please:\n1. Check badge readers in the 'Participate' column\n2. Ensure these badge readers' 'Status' is enabled");

        zh.put("sim.msg.systemTimeReset", "系统时间已重置为当前时间\n");
        en.put("sim.msg.systemTimeReset", "System time reset to current time\n");
        zh.put("sim.msg.readerStatusChanged", "读卡器 {0} 状态已更改为 {1}\n");
        en.put("sim.msg.readerStatusChanged", "Badge reader {0} status changed to {1}\n");
        zh.put("sim.text.custom", " (自定义)");
        en.put("sim.text.custom", " (Custom)");
        zh.put("sim.msg.setReadersStatus", "{0}了 {1} 个读卡器\n");
        en.put("sim.msg.setReadersStatus", "{0} {1} badge readers\n");
        zh.put("sim.msg.selectedReadersTable", "在表格中选择了 {0} 个读卡器\n");
        en.put("sim.msg.selectedReadersTable", "Selected {0} badge readers in the table\n");
        zh.put("sim.msg.deselectedReadersTable", "取消选择了表格中所有的读卡器\n");
        en.put("sim.msg.deselectedReadersTable", "Deselected all badge readers in the table\n");
        zh.put("sim.msg.setReadersParticipation", "{0}了 {1} 个读卡器的模拟参与状态\n");
        en.put("sim.msg.setReadersParticipation", "{0} {1} badge readers for simulation\n");
        zh.put("sim.msg.selectedUsersTable", "在表格中选择了 {0} 个用户\n");
        en.put("sim.msg.selectedUsersTable", "Selected {0} users in the table\n");
        zh.put("sim.msg.deselectedUsersTable", "取消选择了表格中所有的用户\n");
        en.put("sim.msg.deselectedUsersTable", "Deselected all users in the table\n");
        zh.put("sim.dialog.noUsersAvailable", "没有可用的用户");
        en.put("sim.dialog.noUsersAvailable", "No available users");
        zh.put("sim.dialog.userListEmpty", "模拟用户列表已为空");
        en.put("sim.dialog.userListEmpty", "Simulated user list is already empty");
        zh.put("sim.dialog.confirmClearUsers", "确定要清空所有模拟用户吗？\n\n当前模拟列表中有 {0} 个用户\n如果模拟正在运行，它将被停止。");
        en.put("sim.dialog.confirmClearUsers", "Are you sure you want to clear all simulated users?\n\nCurrently {0} users in simulation list\nIf simulation is running, it will be stopped.");
        zh.put("sim.title.confirmClear", "确认清空");
        en.put("sim.title.confirmClear", "Confirm Clear");
        zh.put("sim.dialog.usersCleared", "所有模拟用户已清空");
        en.put("sim.dialog.usersCleared", "All simulated users cleared");
        zh.put("sim.msg.batchAddUsers.done", "批量添加用户完成：\n- 添加：{0}\n- 跳过（已在列表中）：{1}\n- 无卡（无法添加）：{2}\n\n当前模拟列表共有 {3} 个用户。");
        en.put("sim.msg.batchAddUsers.done", "Batch add users done:\n- Added: {0}\n- Skipped (already in list): {1}\n- No badge (could not add): {2}\n\nTotal users in simulation list: {3}.");
        zh.put("sim.msg.batchAddUsers.title", "批量添加完成");
        en.put("sim.msg.batchAddUsers.title", "Batch Add Done");

        zh.put("monitor.view.site", "场地平面图");
        en.put("monitor.view.site", "Site Layout");

        zh.put("profile.default.employee", "员工权限");
        en.put("profile.default.employee", "Employee Permission");
        zh.put("profile.default.contractor", "承包商权限");
        en.put("profile.default.contractor", "Contractor Permission");
        zh.put("profile.default.intern", "实习生权限");
        en.put("profile.default.intern", "Intern Permission");
        zh.put("profile.default.visitor", "访客权限");
        en.put("profile.default.visitor", "Visitor Permission");
        zh.put("profile.default.project_manager", "项目经理权限");
        en.put("profile.default.project_manager", "Project Manager Permission");

        zh.put("access.right.public_area", "公共区域");
        en.put("access.right.public_area", "Public Area");
        zh.put("access.right.office_area", "办公区域");
        en.put("access.right.office_area", "Office Area");
        zh.put("access.right.equipment_resources", "设备资源");
        en.put("access.right.equipment_resources", "Equipment Resources");
        zh.put("access.right.high_security_area", "高安全性区域");
        en.put("access.right.high_security_area", "High Security Area");
        zh.put("monitor.view.office", "办公楼平面图");
        en.put("monitor.view.office", "Office Layout");
        zh.put("monitor.label.view", "视图：");
        en.put("monitor.label.view", "View:");
        zh.put("monitor.label.zoom", "缩放：");
        en.put("monitor.label.zoom", "Zoom:");
        zh.put("monitor.action.configure", "配置位置");
        en.put("monitor.action.configure", "Configure Positions");
        zh.put("monitor.action.autoConfigure", "自动配置");
        en.put("monitor.action.autoConfigure", "Auto-configure");
        zh.put("monitor.action.savePositions", "保存位置");
        en.put("monitor.action.savePositions", "Save Positions");
        zh.put("monitor.action.resetZoom", "重置");
        en.put("monitor.action.resetZoom", "Reset");
        zh.put("monitor.action.zoomOut", "-");
        en.put("monitor.action.zoomOut", "-");
        zh.put("monitor.action.zoomIn", "+");
        en.put("monitor.action.zoomIn", "+");
        zh.put("monitor.text.scale", "{0}%");
        en.put("monitor.text.scale", "{0}%");
        zh.put("monitor.tip.zoomOut", "缩小");
        en.put("monitor.tip.zoomOut", "Zoom Out");
        zh.put("monitor.tip.zoomIn", "放大");
        en.put("monitor.tip.zoomIn", "Zoom In");
        zh.put("monitor.tip.resetZoom", "重置缩放");
        en.put("monitor.tip.resetZoom", "Reset Zoom");
        zh.put("monitor.tip.viewCombo", "场地平面图：site-layout.png | 办公楼平面图：office-layout.png");
        en.put("monitor.tip.viewCombo", "Site Layout: site-layout.png | Office Layout: office-layout.png");
        zh.put("monitor.title.log", "实时事件日志");
        en.put("monitor.title.log", "Event Log");
        zh.put("monitor.action.clearLog", "清空日志");
        en.put("monitor.action.clearLog", "Clear Log");
        zh.put("monitor.event.granted", "✓ 通过");
        en.put("monitor.event.granted", "✓ Granted");
        zh.put("monitor.event.denied", "✗ 拒绝");
        en.put("monitor.event.denied", "✗ Denied");
        zh.put("monitor.event.log", "[{0}] {1} - 读卡器：{2}，资源：{3}，消息：{4}\n");
        en.put("monitor.event.log", "[{0}] {1} - Badge Reader: {2}, Resource: {3}, Message: {4}\n");

        zh.put("monitor.msg.posSaved", "位置配置已保存");
        en.put("monitor.msg.posSaved", "Position configuration saved");
        zh.put("monitor.msg.saveFailed", "保存位置配置失败：{0}");
        en.put("monitor.msg.saveFailed", "Failed to save position configuration: {0}");
        zh.put("monitor.msg.noReaders", "没有可用的读卡器");
        en.put("monitor.msg.noReaders", "No available badge readers");
        zh.put("monitor.msg.confirmAutoConfig", "是否自动为所有读卡器分配位置？\n系统将根据资源类型和位置自动分配坐标。\n稍后您可以手动调整位置。");
        en.put("monitor.msg.confirmAutoConfig", "Automatically assign positions for all badge readers?\nSystem will automatically assign coordinates based on resource type and location.\nYou can manually adjust positions later.");
        zh.put("monitor.title.autoConfig", "自动配置位置");
        en.put("monitor.title.autoConfig", "Auto-configure Positions");
        zh.put("monitor.msg.autoConfigComplete", "已自动为 {0} 个读卡器分配位置\n您可以在地图上拖动以调整位置，然后点击“保存位置配置”进行保存。");
        en.put("monitor.msg.autoConfigComplete", "Automatically assigned positions for {0} badge readers\nYou can drag to adjust positions on the map, then click 'Save Position Configuration' to save.");
        zh.put("monitor.save.comment", "读卡器位置配置");
        en.put("monitor.save.comment", "Badge Reader Position Configuration");
        zh.put("monitor.reader.prefix", "读卡器 {0}");
        en.put("monitor.reader.prefix", "R{0}");
        zh.put("monitor.title.configComplete", "配置完成");
        en.put("monitor.title.configComplete", "Configuration Complete");
        zh.put("monitor.title.configDialog", "配置读卡器位置");
        en.put("monitor.title.configDialog", "Configure Badge Reader Positions");
        zh.put("monitor.text.configInstructions", "读卡器位置配置说明：\n\n" +
            "目的：\n" +
            "• 在地图上设置读卡器的显示位置\n" +
            "• 用于实时监控面板中读卡器位置的可视化显示\n" +
            "• 当发生访问事件时，将在相应的读卡器位置显示闪烁指示器\n\n" +
            "用法：\n" +
            "1. 点击“自动配置所有位置”按钮一次性为所有读卡器分配位置\n" +
            "2. 在地图上点击并拖动读卡器图标以手动调整位置\n" +
            "3. 点击读卡器图标查看详细信息\n" +
            "4. 调整后，点击“保存位置配置”按钮进行保存\n" +
            "5. 位置配置按视图类型（场地布局/办公楼布局）分别保存\n\n" +
            "提示：\n" +
            "• 建议先使用“自动配置所有位置”进行快速配置\n" +
            "• 然后根据实际布局手动微调位置\n" +
            "• 不同视图类型的位置配置是独立的");
        en.put("monitor.text.configInstructions", "Badge Reader Position Configuration Instructions:\n\n" +
            "Purpose:\n" +
            "• Set badge reader display positions on the map\n" +
            "• Used for visual display of badge reader positions in real-time monitor panel\n" +
            "• When access events occur, flash indicators will be shown at corresponding badge reader positions\n\n" +
            "Usage:\n" +
            "1. Click 'Auto-configure All Positions' button to assign positions for all badge readers at once\n" +
            "2. Click and drag badge reader icons on the map to manually adjust positions\n" +
            "3. Click badge reader icons to view detailed information\n" +
            "4. After adjustment, click 'Save Position Configuration' button to save\n" +
            "5. Position configurations are saved separately by view type (Site Layout/Office Layout)\n\n" +
            "Tips:\n" +
            "• It's recommended to use 'Auto-configure All Positions' for quick configuration first\n" +
            "• Then manually fine-tune positions according to actual layout\n" +
            "• Position configurations for different view types are independent");
        zh.put("monitor.info.readerId", "读卡器 ID: ");
        en.put("monitor.info.readerId", "Badge Reader ID: ");
        zh.put("monitor.info.resourceName", "资源名称: ");
        en.put("monitor.info.resourceName", "Resource Name: ");
        zh.put("monitor.info.resourceType", "资源类型: ");
        en.put("monitor.info.resourceType", "Resource Type: ");
        zh.put("monitor.info.location", "位置: ");
        en.put("monitor.info.location", "Location: ");
        zh.put("monitor.info.building", "建筑物: ");
        en.put("monitor.info.building", "Building: ");
        zh.put("monitor.info.floor", "楼层: ");
        en.put("monitor.info.floor", "Floor: ");
        zh.put("monitor.info.resourceId", "资源 ID: ");
        en.put("monitor.info.resourceId", "Resource ID: ");
        zh.put("monitor.info.posOriginal", "位置 (原始): ({0}, {1})");
        en.put("monitor.info.posOriginal", "Position (Original): ({0}, {1})");
        zh.put("monitor.info.posCurrent", "位置 (当前缩放): ({0}, {1})");
        en.put("monitor.info.posCurrent", "Position (Current Scale): ({0}, {1})");
        zh.put("monitor.title.readerInfo", "读卡器信息");
        en.put("monitor.title.readerInfo", "Badge Reader Information");

        Map<Language, Map<String, String>> dict = new HashMap<>();
        dict.put(Language.ZH, zh);
        dict.put(Language.EN, en);
        return dict;
    }
}
