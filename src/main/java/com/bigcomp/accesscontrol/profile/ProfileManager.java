// Group 2 ChenGong ZhangZhao LiangYiKuo
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
 * Profile Manager - Responsible for loading, saving and managing profiles
 */
public class ProfileManager {
    private static final String PROFILES_DIR = "data/profiles";
    private Map<String, Profile> profiles; // Profile name -> Profile object
    private ObjectMapper objectMapper;

    public ProfileManager() {
        this.profiles = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        loadProfiles();
    }

    /**
     * Load all profiles from files
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
                        System.err.println("Failed to load profile: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("Failed to load profile directory: " + e.getMessage());
        }
    }

    /**
     * Load single profile from file (public method for backup recovery)
     */
    public Profile loadProfileFromFile(File file) throws IOException {
        // Simplified implementation: use JSON format
        // Actual implementation needs to parse custom time filter syntax
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
     * Parse time filter (simplified implementation)
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
            String[] dayNamesCN = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            
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
                // Parse time range string, e.g., "8:00-12:00"
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
     * Save profile to file
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
        
        // Convert TimeFilter to TimeFilterData
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
        // Update profile in memory (ensure using latest object)
        profiles.put(profile.getName(), profile);
        
        // Reload to ensure latest data from file (handle possible serialization issues)
        try {
            Profile reloadedProfile = loadProfileFromFile(file);
            if (reloadedProfile != null) {
                profiles.put(profile.getName(), reloadedProfile);
            }
        } catch (Exception e) {
            // If reload fails, use in-memory object
            System.err.println("Failed to reload profile, using in-memory object: " + e.getMessage());
        }
    }

    /**
     * Get profile
     */
    public Profile getProfile(String name) {
        return profiles.get(name);
    }

    /**
     * Get all profiles
     */
    public Map<String, Profile> getAllProfiles() {
        return new HashMap<>(profiles);
    }

    /**
     * Delete profile
     */
    public void deleteProfile(String name) throws IOException {
        profiles.remove(name);
        File file = new File(PROFILES_DIR, name + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    // Internal data classes for JSON serialization
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

