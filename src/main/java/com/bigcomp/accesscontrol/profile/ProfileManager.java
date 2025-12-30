package com.bigcomp.accesscontrol.profile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * 配置文件管理器 - 负责加载、保存和管理配置文件
 */
public class ProfileManager {
    private static final String PROFILES_DIR = "data/profiles";
    private Map<String, Profile> profiles; // 配置文件名称 -> 配置文件对象
    private ObjectMapper objectMapper;

    public ProfileManager() {
        this.profiles = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        loadProfiles();
    }

    /**
     * 从文件加载所有配置文件
     */
    private void loadProfiles() {
        try {
            Path profilesPath = Paths.get(PROFILES_DIR);
            if (!Files.exists(profilesPath)) {
                Files.createDirectories(profilesPath);
                return;
            }

            Files.list(profilesPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        Profile profile = loadProfileFromFile(path.toFile());
                        if (profile != null) {
                            profiles.put(profile.getName(), profile);
                        }
                    } catch (Exception e) {
                        System.err.println("加载配置文件失败: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("加载配置文件目录失败: " + e.getMessage());
        }
    }

    /**
     * 从文件加载单个配置文件（公共方法，用于恢复备份）
     */
    public Profile loadProfileFromFile(File file) throws IOException {
        // 简化实现：使用JSON格式
        // 实际实现需要解析自定义的时间过滤器语法
        String content = Files.readString(file.toPath());
        ProfileData data = objectMapper.readValue(content, ProfileData.class);
        
        Profile profile = new Profile(data.name);
        for (Map.Entry<String, TimeFilterData> entry : data.accessRights.entrySet()) {
            TimeFilter filter = parseTimeFilter(entry.getValue());
            profile.addAccessRight(entry.getKey(), filter);
        }
        
        return profile;
    }

    /**
     * 解析时间过滤器（简化实现）
     */
    private TimeFilter parseTimeFilter(TimeFilterData data) {
        TimeFilter filter = new TimeFilter();
        
        if (data.years != null) {
            filter.setYears(data.years);
            filter.setExcludeYears(data.excludeYears);
        }
        if (data.months != null && !data.months.isEmpty()) {
            Set<Month> monthSet = new HashSet<>();
            String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
            String[] monthNamesShort = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            
            for (String monthStr : data.months) {
                for (int i = 0; i < monthNames.length; i++) {
                    if (monthStr.equalsIgnoreCase(monthNames[i]) || 
                        monthStr.equalsIgnoreCase(monthNamesShort[i]) ||
                        monthStr.equalsIgnoreCase(String.valueOf(i + 1))) {
                        monthSet.add(Month.of(i + 1));
                        break;
                    }
                }
            }
            if (!monthSet.isEmpty()) {
                filter.setMonths(monthSet);
                filter.setExcludeMonths(data.excludeMonths);
            }
        }
        if (data.daysOfWeek != null && !data.daysOfWeek.isEmpty()) {
            Set<DayOfWeek> daySet = new HashSet<>();
            String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            String[] dayNamesShort = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            String[] dayNamesCN = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            
            for (String dayStr : data.daysOfWeek) {
                for (int i = 0; i < dayNames.length; i++) {
                    if (dayStr.equalsIgnoreCase(dayNames[i]) || 
                        dayStr.equalsIgnoreCase(dayNamesShort[i]) ||
                        dayStr.equalsIgnoreCase(dayNamesCN[i]) ||
                        dayStr.equalsIgnoreCase(String.valueOf(i + 1))) {
                        daySet.add(DayOfWeek.of(i + 1));
                        break;
                    }
                }
            }
            if (!daySet.isEmpty()) {
                filter.setDaysOfWeek(daySet);
                filter.setExcludeDaysOfWeek(data.excludeDaysOfWeek);
            }
        }
        if (data.timeRanges != null) {
            List<TimeFilter.TimeRange> ranges = new ArrayList<>();
            for (String rangeStr : data.timeRanges) {
                // 解析时间范围字符串，例如 "8:00-12:00"
                String[] parts = rangeStr.split("-");
                if (parts.length == 2) {
                    String[] start = parts[0].split(":");
                    String[] end = parts[1].split(":");
                    if (start.length == 2 && end.length == 2) {
                        ranges.add(new TimeFilter.TimeRange(
                            Integer.parseInt(start[0]),
                            Integer.parseInt(start[1]),
                            Integer.parseInt(end[0]),
                            Integer.parseInt(end[1])
                        ));
                    }
                }
            }
            filter.setTimeRanges(ranges);
            filter.setExcludeTimeRanges(data.excludeTimeRanges);
        }
        
        return filter;
    }

    /**
     * 保存配置文件到文件
     */
    public void saveProfile(Profile profile) throws IOException {
        Path profilesPath = Paths.get(PROFILES_DIR);
        if (!Files.exists(profilesPath)) {
            Files.createDirectories(profilesPath);
        }

        File file = new File(profilesPath.toFile(), profile.getName() + ".json");
        ProfileData data = new ProfileData();
        data.name = profile.getName();
        data.accessRights = new HashMap<>();
        
        // 将TimeFilter转换为TimeFilterData
        Map<String, TimeFilter> accessRights = profile.getAccessRights();
        for (Map.Entry<String, TimeFilter> entry : accessRights.entrySet()) {
            TimeFilter filter = entry.getValue();
            TimeFilterData filterData = new TimeFilterData();
            
            if (filter.getYears() != null) {
                filterData.years = new ArrayList<>(filter.getYears());
                filterData.excludeYears = filter.isExcludeYears();
            }
            
            if (filter.getMonths() != null) {
                filterData.months = new ArrayList<>();
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                for (Month month : filter.getMonths()) {
                    filterData.months.add(monthNames[month.getValue() - 1]);
                }
                filterData.excludeMonths = filter.isExcludeMonths();
            }
            
            if (filter.getDaysOfMonth() != null) {
                filterData.daysOfMonth = new ArrayList<>(filter.getDaysOfMonth());
                filterData.excludeDaysOfMonth = filter.isExcludeDaysOfMonth();
            }
            
            if (filter.getDaysOfWeek() != null) {
                filterData.daysOfWeek = new ArrayList<>();
                String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                for (DayOfWeek day : filter.getDaysOfWeek()) {
                    filterData.daysOfWeek.add(dayNames[day.getValue() - 1]);
                }
                filterData.excludeDaysOfWeek = filter.isExcludeDaysOfWeek();
            }
            
            if (filter.getTimeRanges() != null && !filter.getTimeRanges().isEmpty()) {
                filterData.timeRanges = new ArrayList<>();
                for (TimeFilter.TimeRange range : filter.getTimeRanges()) {
                    int startMinutes = range.getStartMinutes();
                    int endMinutes = range.getEndMinutes();
                    String rangeStr = String.format("%d:%02d-%d:%02d",
                        startMinutes / 60, startMinutes % 60,
                        endMinutes / 60, endMinutes % 60);
                    filterData.timeRanges.add(rangeStr);
                }
                filterData.excludeTimeRanges = filter.isExcludeTimeRanges();
            }
            
            data.accessRights.put(entry.getKey(), filterData);
        }
        
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        // 更新内存中的配置文件（确保使用最新的对象）
        profiles.put(profile.getName(), profile);
        
        // 重新加载以确保从文件读取的最新数据（处理可能的序列化问题）
        try {
            Profile reloadedProfile = loadProfileFromFile(file);
            if (reloadedProfile != null) {
                profiles.put(profile.getName(), reloadedProfile);
            }
        } catch (Exception e) {
            // 如果重新加载失败，使用内存中的对象
            System.err.println("重新加载配置文件失败，使用内存对象: " + e.getMessage());
        }
    }

    /**
     * 获取配置文件
     */
    public Profile getProfile(String name) {
        return profiles.get(name);
    }

    /**
     * 获取所有配置文件
     */
    public Map<String, Profile> getAllProfiles() {
        return new HashMap<>(profiles);
    }

    /**
     * 删除配置文件
     */
    public void deleteProfile(String name) throws IOException {
        profiles.remove(name);
        File file = new File(PROFILES_DIR, name + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    // 内部数据类用于JSON序列化
    private static class ProfileData {
        public String name;
        public Map<String, TimeFilterData> accessRights;
    }

    private static class TimeFilterData {
        public List<Integer> years;
        public List<String> months;
        public List<Integer> daysOfMonth;
        public List<String> daysOfWeek;
        public List<String> timeRanges;
        public boolean excludeYears;
        public boolean excludeMonths;
        public boolean excludeDaysOfMonth;
        public boolean excludeDaysOfWeek;
        public boolean excludeTimeRanges;
    }
}

