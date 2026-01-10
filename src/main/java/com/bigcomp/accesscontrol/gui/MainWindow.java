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
        I18n.setLanguage(I18n.defaultLanguageFor(Locale.getDefault()));
        applyLanguage();
        setSize(1200, 800);
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
        zh.put("about.body", "BigComp 门禁控制系统 v1.0\\n\\n综合门禁控制与管理系统");
        en.put("about.body", "BigComp Access Control System v1.0\\n\\nComprehensive access control and management system");

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
        zh.put("common.confirmDelete.title", "确认删除");
        en.put("common.confirmDelete.title", "Confirm Delete");
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
        zh.put("user.msg.confirmDelete", "确定要删除用户“{0}”吗？\\n该操作也会删除该用户的徽章及相关配置。");
        en.put("user.msg.confirmDelete", "Are you sure you want to delete user \"{0}\"?\\nThis operation will also delete the user's badge and related configurations.");
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
        zh.put("user.msg.profileAssigned", "已为用户分配配置文件“{0}”\\n\\n用户当前配置文件：\\n{1}");
        en.put("user.msg.profileAssigned", "Profile \"{0}\" has been assigned to user\\n\\nUser's current profiles:\\n{1}");
        zh.put("user.msg.assignProfileFailed", "分配配置文件失败：{0}");
        en.put("user.msg.assignProfileFailed", "Failed to assign profile: {0}");
        zh.put("user.msg.loadFailed", "加载用户失败：{0}");
        en.put("user.msg.loadFailed", "Failed to load users: {0}");
        zh.put("user.msg.autoAssignSummary", "自动分配完成！\\n\\n统计：\\n• 成功分配：{0} 人\\n• 跳过（已有配置）：{1} 人\\n• 跳过（无徽章）：{2} 人\\n• 失败：{3} 人\\n\\n明细：\\n{4}");
        en.put("user.msg.autoAssignSummary", "Auto-assign profiles completed!\\n\\nStatistics:\\n• Successfully assigned: {0} users\\n• Skipped (already has profile): {1} users\\n• Skipped (no badge): {2} users\\n• Failed: {3} users\\n\\nDetails:\\n{4}");
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
        zh.put("resource.msg.confirmDelete", "确定要删除资源“{0}”吗？\\n该操作也会删除关联的读卡器与资源组关系。");
        en.put("resource.msg.confirmDelete", "Are you sure you want to delete resource \"{0}\"?\\nThis operation will also delete associated badge readers and resource group associations.");
        zh.put("resource.msg.deleted", "资源删除成功");
        en.put("resource.msg.deleted", "Resource deleted successfully");
        zh.put("resource.msg.deleteFailed", "删除资源失败：{0}");
        en.put("resource.msg.deleteFailed", "Failed to delete resource: {0}");
        zh.put("resource.msg.selectResource", "请选择一个资源");
        en.put("resource.msg.selectResource", "Please select a resource");
        zh.put("resource.msg.readerExistsConfirm", "资源“{0}”已经存在读卡器：{1}\\n\\n是否创建新的读卡器？这将替换现有读卡器。");
        en.put("resource.msg.readerExistsConfirm", "Resource \"{0}\" already has a badge reader: {1}\\n\\nDo you want to create a new badge reader? This will replace the existing one.");
        zh.put("resource.msg.readerCreated", "读卡器创建成功");
        en.put("resource.msg.readerCreated", "Badge reader created successfully");
        zh.put("resource.msg.readerCreateFailed", "创建读卡器失败：{0}");
        en.put("resource.msg.readerCreateFailed", "Failed to create badge reader: {0}");
        zh.put("resource.msg.noResources", "没有可用资源，请先添加资源");
        en.put("resource.msg.noResources", "No available resources, please add resources first");
        zh.put("resource.msg.allHaveReaders", "所有资源都已配置读卡器！\\n\\n资源总数：{0}\\n已配置读卡器：{1}");
        en.put("resource.msg.allHaveReaders", "All resources already have badge readers configured!\\n\\nTotal resources: {0}\\nWith badge readers: {1}");
        zh.put("resource.action.createReadersAll", "为全部资源创建读卡器");
        en.put("resource.action.createReadersAll", "Create Badge Readers for All Resources");
        zh.put("resource.msg.createReadersConfirm", "将为 {0} 个资源创建读卡器\\n\\n统计：\\n• 资源总数：{1}\\n• 已有读卡器：{2}\\n• 待创建：{0}\\n\\n继续吗？");
        en.put("resource.msg.createReadersConfirm", "Will create badge readers for {0} resources\\n\\nStatistics:\\n• Total resources: {1}\\n• Already have badge readers: {2}\\n• Need to create: {0}\\n\\nContinue?");
        zh.put("resource.msg.createReadersDone", "读卡器创建完成！\\n\\n统计：\\n• 成功创建：%d\\n• 跳过（已存在）：%d\\n• 失败：%d\\n\\n明细：\\n%s");
        en.put("resource.msg.createReadersDone", "Badge reader creation completed!\\n\\nStatistics:\\n• Successfully created: %d\\n• Skipped (already exists): %d\\n• Failed: %d\\n\\nDetails:\\n%s");
        zh.put("resource.msg.createReadersTitle", "批量创建完成");
        en.put("resource.msg.createReadersTitle", "Batch Badge Reader Creation Complete");
        zh.put("resource.msg.noGroupsHelp", "没有可用资源组。\\n\\n请先在 data/groups/ 目录中创建资源组 JSON 文件。\\n示例：{\"name\":\"Office Area\",\"securityLevel\":1,\"resources\":[]}\\n\\n创建后请重启程序或刷新资源组。");
        en.put("resource.msg.noGroupsHelp", "No available resource groups.\\n\\nPlease create resource group JSON files in data/groups/ directory first.\\nExample: {\"name\": \"Office Area\", \"securityLevel\": 1, \"resources\": []}\\n\\nAfter creating, please restart the program or refresh resource groups.");
        zh.put("resource.msg.selectGroup", "请选择要将资源“{0}”关联到的资源组：");
        en.put("resource.msg.selectGroup", "Select resource group to link resource \"{0}\" to:");
        zh.put("resource.action.linkGroup", "关联资源组");
        en.put("resource.action.linkGroup", "Link to Resource Group");
        zh.put("resource.msg.linkedToGroup", "资源已关联到资源组“{0}”\\n\\n资源ID：{1}\\n现在可以在配置文件中为该资源组配置访问权限。");
        en.put("resource.msg.linkedToGroup", "Resource has been linked to resource group \"{0}\"\\n\\nResource ID: {1}\\nYou can now configure access permissions for this resource group in profiles.");
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

        zh.put("profile.title.list", "配置文件");
        en.put("profile.title.list", "Profiles");
        zh.put("profile.title.rights", "访问权限");
        en.put("profile.title.rights", "Access Rights");
        zh.put("profile.title.timeFilter", "时间过滤");
        en.put("profile.title.timeFilter", "Time Filter");
        zh.put("profile.title.profile", "配置文件信息");
        en.put("profile.title.profile", "Profile");

        zh.put("profile.field.group", "资源组：");
        en.put("profile.field.group", "Resource Group:");
        zh.put("profile.field.name", "配置文件名称：");
        en.put("profile.field.name", "Profile Name:");
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
        zh.put("profile.action.save", "保存配置文件");
        en.put("profile.action.save", "Save Profile");

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
        zh.put("sim.text.instructions", "说明：\\n• “参与”列：勾选表示该读卡器参与事件模拟\\n• “状态”列：启用/禁用读卡器，禁用后不会响应任何徽章操作\\n• 只有“参与”勾选且“状态”启用的读卡器才会生成模拟事件");
        en.put("sim.text.instructions", "Instructions:\\n• 'Participate' column: Check to include this badge reader in event simulation\\n• 'Status' column: Enable/disable badge reader, disabled readers won't respond to any badge operations\\n• Only badge readers with both 'Participate' checked and 'Status' enabled will generate events in simulation");
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

        zh.put("monitor.view.site", "场地平面图");
        en.put("monitor.view.site", "Site Layout");
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
        zh.put("monitor.event.log", "[{0}] {1} - 读卡器：{2}，资源：{3}，消息：{4}\\n");
        en.put("monitor.event.log", "[{0}] {1} - Badge Reader: {2}, Resource: {3}, Message: {4}\\n");

        Map<Language, Map<String, String>> dict = new HashMap<>();
        dict.put(Language.ZH, zh);
        dict.put(Language.EN, en);
        return dict;
    }
}
