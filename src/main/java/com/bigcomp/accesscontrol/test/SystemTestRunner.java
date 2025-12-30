package com.bigcomp.accesscontrol.test;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.*;
import com.bigcomp.accesscontrol.profile.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统功能测试运行器
 * 自动创建测试数据并演示所有功能
 */
public class SystemTestRunner {
    private AccessControlSystem accessControlSystem;
    private DatabaseManager dbManager;
    
    public SystemTestRunner() {
        this.accessControlSystem = new AccessControlSystem();
        this.dbManager = accessControlSystem.getDatabaseManager();
    }
    
    /**
     * 运行完整测试
     */
    public void runFullTest() {
        System.out.println("==========================================");
        System.out.println("BigComp 门禁控制系统 - 完整功能测试");
        System.out.println("==========================================\n");
        
        try {
            // 1. 创建测试用户
            System.out.println("步骤 1: 创建测试用户...");
            createTestUsers();
            System.out.println("✓ 用户创建完成\n");
            
            // 2. 创建测试资源
            System.out.println("步骤 2: 创建测试资源...");
            createTestResources();
            System.out.println("✓ 资源创建完成\n");
            
            // 3. 创建读卡器
            System.out.println("步骤 3: 创建读卡器...");
            createTestBadgeReaders();
            System.out.println("✓ 读卡器创建完成\n");
            
            // 4. 创建资源组
            System.out.println("步骤 4: 创建资源组...");
            createTestResourceGroups();
            System.out.println("✓ 资源组创建完成\n");
            
            // 5. 创建配置文件
            System.out.println("步骤 5: 创建配置文件...");
            createTestProfiles();
            System.out.println("✓ 配置文件创建完成\n");
            
            // 6. 分配配置文件
            System.out.println("步骤 6: 分配配置文件给用户...");
            assignProfilesToUsers();
            System.out.println("✓ 配置文件分配完成\n");
            
            // 7. 生成测试日志
            System.out.println("步骤 7: 生成测试日志...");
            generateTestLogs();
            System.out.println("✓ 测试日志生成完成\n");
            
            System.out.println("==========================================");
            System.out.println("所有测试数据创建完成！");
            System.out.println("==========================================");
            System.out.println("\n测试数据摘要：");
            printTestSummary();
            
        } catch (Exception e) {
            System.err.println("测试过程中出现错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建测试用户
     */
    private void createTestUsers() throws Exception {
        // 创建所有类型的用户（覆盖所有用户类型）
        User[] users = {
            // 员工
            new User("EMP001", User.Gender.MALE, "张", "三", User.UserType.EMPLOYEE),
            new User("EMP002", User.Gender.FEMALE, "李", "四", User.UserType.EMPLOYEE),
            // 承包商
            new User("CON001", User.Gender.MALE, "王", "五", User.UserType.CONTRACTOR),
            // 实习生
            new User("INT001", User.Gender.FEMALE, "赵", "六", User.UserType.INTERN),
            // 访客
            new User("VIS001", User.Gender.MALE, "钱", "七", User.UserType.VISITOR),
            // 项目经理
            new User("PM001", User.Gender.FEMALE, "孙", "八", User.UserType.PROJECT_MANAGER)
        };
        
        for (User user : users) {
            dbManager.addUser(user);
            // 为每个用户创建徽章
            Badge badge = new Badge(user.getId());
            badge.setExpirationDate(LocalDateTime.now().plusYears(1));
            String badgeId = UUID.randomUUID().toString();
            dbManager.addBadge(badge, badgeId);
            // 更新用户的徽章ID
            user.setBadgeId(badgeId);
            dbManager.addUser(user);
            System.out.println("  创建用户: " + user.getFullName() + " (" + user.getUserType() + ") - 徽章: " + badge.getCode());
        }
    }
    
    /**
     * 创建测试资源
     */
    private void createTestResources() throws Exception {
        Resource[] resources = {
            // 场地入口（GATE类型）
            new Resource("RES001", "主入口大门", Resource.ResourceType.GATE, "场地", "主办公楼", "1楼"),
            new Resource("RES002", "停车场入口", Resource.ResourceType.GATE, "场地", "停车场", "地面"),
            new Resource("RES003", "侧门入口", Resource.ResourceType.GATE, "场地", "主办公楼", "1楼"),
            
            // 建筑入口（DOOR类型）
            new Resource("RES004", "主办公楼正门", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "1楼"),
            new Resource("RES005", "主办公楼侧门", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "1楼"),
            new Resource("RES006", "副楼入口", Resource.ResourceType.DOOR, "副楼", "副楼", "1楼"),
            
            // 办公室门（DOOR类型）
            new Resource("RES007", "办公室1", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "2楼"),
            new Resource("RES008", "办公室2", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "2楼"),
            new Resource("RES009", "办公室3", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "3楼"),
            new Resource("RES010", "会议室", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "2楼"),
            new Resource("RES011", "技术室", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "3楼"),
            
            // 电梯和楼梯
            new Resource("RES012", "主电梯", Resource.ResourceType.ELEVATOR, "主办公楼", "主办公楼", "1楼"),
            new Resource("RES013", "副电梯", Resource.ResourceType.ELEVATOR, "主办公楼", "主办公楼", "1楼"),
            new Resource("RES014", "主楼梯", Resource.ResourceType.STAIRWAY, "主办公楼", "主办公楼", "1楼"),
            new Resource("RES015", "紧急楼梯", Resource.ResourceType.STAIRWAY, "主办公楼", "主办公楼", "1楼"),
            
            // 打印机（PRINTER类型）
            new Resource("RES016", "打印机1", Resource.ResourceType.PRINTER, "主办公楼", "主办公楼", "2楼"),
            new Resource("RES017", "打印机2", Resource.ResourceType.PRINTER, "主办公楼", "主办公楼", "3楼"),
            new Resource("RES018", "彩色打印机", Resource.ResourceType.PRINTER, "主办公楼", "主办公楼", "2楼"),
            
            // 饮料机（BEVERAGE_DISPENSER类型）
            new Resource("RES019", "饮料机1", Resource.ResourceType.BEVERAGE_DISPENSER, "主办公楼", "主办公楼", "2楼"),
            new Resource("RES020", "饮料机2", Resource.ResourceType.BEVERAGE_DISPENSER, "主办公楼", "主办公楼", "3楼"),
            
            // 停车场（PARKING类型）
            new Resource("RES021", "地下停车场A区", Resource.ResourceType.PARKING, "主办公楼", "主办公楼", "地下1层"),
            new Resource("RES022", "地下停车场B区", Resource.ResourceType.PARKING, "主办公楼", "主办公楼", "地下1层"),
            new Resource("RES023", "地面停车场", Resource.ResourceType.PARKING, "场地", "停车场", "地面"),
            
            // 高安全区域（DOOR类型）
            new Resource("RES024", "服务器室", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "3楼"),
            new Resource("RES025", "数据中心", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "3楼"),
            new Resource("RES026", "财务室", Resource.ResourceType.DOOR, "主办公楼", "主办公楼", "2楼")
        };
        
        for (Resource resource : resources) {
            dbManager.addResource(resource);
            System.out.println("  创建资源: " + resource.getName() + " (" + resource.getType() + ")");
        }
    }
    
    /**
     * 创建读卡器
     */
    private void createTestBadgeReaders() throws Exception {
        // 为每个资源创建读卡器
        Map<String, Resource> resources = dbManager.loadAllResources();
        int readerIndex = 1;
        
        for (Resource resource : resources.values()) {
            BadgeReader reader = new BadgeReader("BR" + String.format("%03d", readerIndex), resource.getId());
            dbManager.addBadgeReader(reader);
            resource.setBadgeReaderId(reader.getId());
            
            // 注册到路由器
            accessControlSystem.getRouter().registerBadgeReader(reader);
            
            System.out.println("  创建读卡器: " + reader.getId() + " -> " + resource.getName());
            readerIndex++;
        }
    }
    
    /**
     * 创建资源组
     */
    private void createTestResourceGroups() throws Exception {
        GroupManager groupManager = new GroupManager();
        Map<String, Resource> resources = dbManager.loadAllResources();
        
        // 公共区域组（低安全级别）
        ResourceGroup publicArea = new ResourceGroup("公共区域", 1);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.GATE || 
                res.getName().contains("入口") || 
                res.getName().contains("大门")) {
                publicArea.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "公共区域");
            }
        }
        groupManager.saveGroup(publicArea);
        System.out.println("  创建资源组: 公共区域 (安全级别: 1, 资源数: " + publicArea.getResourceIds().size() + ")");
        
        // 办公区域组（中安全级别）
        ResourceGroup officeArea = new ResourceGroup("办公区域", 2);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.DOOR && 
                (res.getName().contains("办公室") || res.getName().contains("会议室"))) {
                officeArea.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "办公区域");
            }
        }
        groupManager.saveGroup(officeArea);
        System.out.println("  创建资源组: 办公区域 (安全级别: 2, 资源数: " + officeArea.getResourceIds().size() + ")");
        
        // 设备资源组
        ResourceGroup equipmentGroup = new ResourceGroup("设备资源", 2);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.PRINTER || 
                res.getType() == Resource.ResourceType.BEVERAGE_DISPENSER) {
                equipmentGroup.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "设备资源");
            }
        }
        groupManager.saveGroup(equipmentGroup);
        System.out.println("  创建资源组: 设备资源 (安全级别: 2, 资源数: " + equipmentGroup.getResourceIds().size() + ")");
        
        // 高安全区域组
        ResourceGroup highSecurityArea = new ResourceGroup("高安全区域", 3);
        for (Resource res : resources.values()) {
            if (res.getName().contains("服务器") || 
                res.getName().contains("数据中心") ||
                res.getName().contains("财务室") ||
                res.getType() == Resource.ResourceType.ELEVATOR) {
                highSecurityArea.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "高安全区域");
            }
        }
        groupManager.saveGroup(highSecurityArea);
        System.out.println("  创建资源组: 高安全区域 (安全级别: 3, 资源数: " + highSecurityArea.getResourceIds().size() + ")");
        
        // 停车场组
        ResourceGroup parkingGroup = new ResourceGroup("停车场区域", 1);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.PARKING) {
                parkingGroup.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "停车场区域");
            }
        }
        groupManager.saveGroup(parkingGroup);
        System.out.println("  创建资源组: 停车场区域 (安全级别: 1, 资源数: " + parkingGroup.getResourceIds().size() + ")");
        
        // 楼梯组
        ResourceGroup stairwayGroup = new ResourceGroup("楼梯区域", 2);
        for (Resource res : resources.values()) {
            if (res.getType() == Resource.ResourceType.STAIRWAY) {
                stairwayGroup.addResource(res.getId());
                dbManager.linkResourceToGroup(res.getId(), "楼梯区域");
            }
        }
        groupManager.saveGroup(stairwayGroup);
        System.out.println("  创建资源组: 楼梯区域 (安全级别: 2, 资源数: " + stairwayGroup.getResourceIds().size() + ")");
    }
    
    /**
     * 创建配置文件
     */
    private void createTestProfiles() throws Exception {
        ProfileManager profileManager = accessControlSystem.getProfileManager();
        
        // 员工权限配置
        Profile employeeProfile = new Profile("员工权限");
        TimeFilter employeeFilter = new TimeFilter();
        employeeFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        employeeFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(8, 0, 18, 0)
        ));
        employeeProfile.addAccessRight("公共区域", employeeFilter);
        employeeProfile.addAccessRight("办公区域", employeeFilter);
        employeeProfile.addAccessRight("设备资源", employeeFilter);
        profileManager.saveProfile(employeeProfile);
        System.out.println("  创建配置文件: 员工权限");
        
        // 承包商权限配置
        Profile contractorProfile = new Profile("承包商权限");
        TimeFilter contractorFilter = new TimeFilter();
        contractorFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        contractorFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(9, 0, 17, 0)
        ));
        contractorProfile.addAccessRight("公共区域", contractorFilter);
        contractorProfile.addAccessRight("办公区域", contractorFilter);
        profileManager.saveProfile(contractorProfile);
        System.out.println("  创建配置文件: 承包商权限");
        
        // 实习生权限配置
        Profile internProfile = new Profile("实习生权限");
        TimeFilter internFilter = new TimeFilter();
        internFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        internFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(9, 0, 17, 0)
        ));
        internProfile.addAccessRight("公共区域", internFilter);
        internProfile.addAccessRight("办公区域", internFilter);
        profileManager.saveProfile(internProfile);
        System.out.println("  创建配置文件: 实习生权限");
        
        // 访客权限配置
        Profile visitorProfile = new Profile("访客权限");
        TimeFilter visitorFilter = new TimeFilter();
        visitorFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        visitorFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(10, 0, 16, 0)
        ));
        visitorProfile.addAccessRight("公共区域", visitorFilter);
        profileManager.saveProfile(visitorProfile);
        System.out.println("  创建配置文件: 访客权限");
        
        // 项目经理权限配置（高级权限）
        Profile pmProfile = new Profile("项目经理权限");
        TimeFilter pmFilter = new TimeFilter();
        pmFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        pmFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(7, 0, 20, 0) // 更长的访问时间
        ));
        pmProfile.addAccessRight("公共区域", pmFilter);
        pmProfile.addAccessRight("办公区域", pmFilter);
        pmProfile.addAccessRight("设备资源", pmFilter);
        pmProfile.addAccessRight("高安全区域", pmFilter); // 可以访问高安全区域
        profileManager.saveProfile(pmProfile);
        System.out.println("  创建配置文件: 项目经理权限");
        
        // 全天候权限配置（演示ALL时间过滤器）
        Profile fullAccessProfile = new Profile("全天候权限");
        TimeFilter fullAccessFilter = new TimeFilter();
        // 不设置任何限制，表示ALL（所有时间）
        fullAccessProfile.addAccessRight("公共区域", fullAccessFilter);
        fullAccessProfile.addAccessRight("办公区域", fullAccessFilter);
        profileManager.saveProfile(fullAccessProfile);
        System.out.println("  创建配置文件: 全天候权限 (ALL时间)");
        
        // 排除时间配置（演示EXCEPT功能）
        // 注意：当前TimeFilter实现中，excludeTimeRanges表示排除整个timeRanges列表
        // 要实现"排除12:00-14:00"，需要设置两个时间范围：8:00-12:00 和 14:00-18:00
        Profile excludeLunchProfile = new Profile("排除午休时间");
        TimeFilter excludeLunchFilter = new TimeFilter();
        excludeLunchFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        ));
        // 设置两个时间范围，排除中间的午休时间
        excludeLunchFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(8, 0, 12, 0),  // 上午
            new TimeFilter.TimeRange(14, 0, 18, 0)  // 下午
        ));
        excludeLunchProfile.addAccessRight("办公区域", excludeLunchFilter);
        profileManager.saveProfile(excludeLunchProfile);
        System.out.println("  创建配置文件: 排除午休时间 (8:00-12:00, 14:00-18:00)");
        
        // 排除星期配置（演示EXCEPT星期功能）
        Profile excludeWeekendProfile = new Profile("排除周末");
        TimeFilter excludeWeekendFilter = new TimeFilter();
        // 排除周末（周六和周日）
        excludeWeekendFilter.setDaysOfWeek(Set.of(
            java.time.DayOfWeek.SATURDAY,
            java.time.DayOfWeek.SUNDAY
        ));
        excludeWeekendFilter.setExcludeDaysOfWeek(true); // 排除这些星期
        excludeWeekendFilter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(8, 0, 18, 0)
        ));
        excludeWeekendProfile.addAccessRight("公共区域", excludeWeekendFilter);
        excludeWeekendProfile.addAccessRight("办公区域", excludeWeekendFilter);
        profileManager.saveProfile(excludeWeekendProfile);
        System.out.println("  创建配置文件: 排除周末 (EXCEPT Saturday, Sunday)");
    }
    
    /**
     * 分配配置文件给用户
     */
    private void assignProfilesToUsers() throws Exception {
        Map<String, User> users = dbManager.loadAllUsers();
        
        for (User user : users.values()) {
            // 通过用户ID查找徽章
            Badge badge = dbManager.loadBadgeByUserId(user.getId());
            if (badge == null) continue;
            
            String profileName = null;
            switch (user.getUserType()) {
                case EMPLOYEE:
                    profileName = "员工权限";
                    break;
                case CONTRACTOR:
                    profileName = "承包商权限";
                    break;
                case INTERN:
                    profileName = "实习生权限";
                    break;
                case VISITOR:
                    profileName = "访客权限";
                    break;
                case PROJECT_MANAGER:
                    profileName = "项目经理权限";
                    break;
                default:
                    break;
            }
            
            if (profileName != null) {
                // 需要通过徽章代码找到徽章ID
                String badgeId = findBadgeIdByCode(badge.getCode());
                if (badgeId != null) {
                    dbManager.linkBadgeToProfile(badgeId, profileName);
                    System.out.println("  分配配置文件: " + user.getFullName() + " -> " + profileName);
                }
            }
        }
    }
    
    /**
     * 通过徽章代码查找徽章ID
     */
    private String findBadgeIdByCode(String badgeCode) {
        try {
            String sql = "SELECT id FROM badges WHERE code = ?";
            try (var pstmt = dbManager.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, badgeCode);
                try (var rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("id");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("查找徽章ID失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 生成测试日志
     */
    private void generateTestLogs() {
        // 这个功能由LogManager在运行时自动生成
        // 这里只是提示
        System.out.println("  提示: 日志将在访问事件发生时自动生成");
        System.out.println("  日志位置: data/logs/年/月/日期.csv");
        System.out.println("\n模拟功能说明：");
        System.out.println("  1. 在GUI的'事件模拟'标签页中：");
        System.out.println("     - 选择用户并点击'添加用户'添加到模拟列表");
        System.out.println("     - 设置事件间隔（1-60秒）");
        System.out.println("     - 点击'开始模拟'开始生成访问事件");
        System.out.println("  2. 模拟器支持一致的行为模式：");
        System.out.println("     - 用户会先访问场地入口（GATE）");
        System.out.println("     - 然后访问建筑入口（DOOR，1楼）");
        System.out.println("     - 最后访问办公室、电梯等内部资源");
        System.out.println("  3. 时间测试功能：");
        System.out.println("     - 点击'设置时间'可以修改系统时间");
        System.out.println("     - 用于测试不同时间段的访问规则（如周末、非工作时间）");
        System.out.println("     - 点击'重置时间'恢复系统时间");
    }
    
    /**
     * 打印测试摘要
     */
    private void printTestSummary() {
        try {
            Map<String, User> users = dbManager.loadAllUsers();
            Map<String, Resource> resources = dbManager.loadAllResources();
            
            GroupManager groupManager = new GroupManager();
            Map<String, ResourceGroup> groups = groupManager.getAllGroups();
            
            ProfileManager profileManager = accessControlSystem.getProfileManager();
            Map<String, Profile> profiles = profileManager.getAllProfiles();
            
            System.out.println("用户数量: " + users.size());
            System.out.println("资源数量: " + resources.size());
            System.out.println("资源组数量: " + groups.size());
            System.out.println("配置文件数量: " + profiles.size());
            
            System.out.println("\n用户列表:");
            for (User user : users.values()) {
                Badge badge = dbManager.loadBadgeByUserId(user.getId());
                System.out.println("  - " + user.getFullName() + " (" + user.getUserType() + 
                    ") - 徽章: " + (badge != null ? badge.getCode() : "无"));
            }
            
            System.out.println("\n资源组列表:");
            for (ResourceGroup group : groups.values()) {
                System.out.println("  - " + group.getName() + " (安全级别: " + 
                    group.getSecurityLevel() + ", 资源数: " + group.getResourceIds().size() + ")");
            }
            
            System.out.println("\n配置文件列表:");
            for (Profile profile : profiles.values()) {
                System.out.println("  - " + profile.getName() + " (访问权限数: " + 
                    profile.getAccessRights().size() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("打印摘要时出错: " + e.getMessage());
        }
    }
    
    /**
     * 主方法 - 运行测试
     */
    public static void main(String[] args) {
        SystemTestRunner runner = new SystemTestRunner();
        runner.runFullTest();
        
        System.out.println("\n==========================================");
        System.out.println("测试完成！现在可以启动GUI应用程序。");
        System.out.println("运行 Main.java 启动图形界面。");
        System.out.println("==========================================");
        System.out.println("\n下一步操作：");
        System.out.println("1. 启动GUI：运行 Main.java 或使用 run_all.bat");
        System.out.println("2. 测试模拟功能：");
        System.out.println("   - 进入'事件模拟'标签页");
        System.out.println("   - 选择用户并添加到模拟列表");
        System.out.println("   - 设置事件间隔并开始模拟");
        System.out.println("   - 观察实时监控面板中的访问事件");
        System.out.println("   - 查看日志面板中的访问记录");
        System.out.println("3. 测试时间功能：");
        System.out.println("   - 在事件模拟面板中点击'设置时间'");
        System.out.println("   - 设置为周末或非工作时间");
        System.out.println("   - 观察访问是否被正确拒绝");
        System.out.println("==========================================");
    }
}

