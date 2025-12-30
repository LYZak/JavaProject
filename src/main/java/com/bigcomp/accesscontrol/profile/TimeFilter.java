// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.profile;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Time Filter - Defines time rules for resource access
 */
public class TimeFilter {
    private Set<Integer> years; // Allowed year set, null means all years
    private Set<Month> months; // Allowed month set, null means all months
    private Set<Integer> daysOfMonth; // Allowed day set, null means all days
    private Set<DayOfWeek> daysOfWeek; // Allowed day of week set, null means all days
    private List<TimeRange> timeRanges; // Allowed time range list, null means all times

    private boolean excludeYears; // Whether to exclude specified years
    private boolean excludeMonths; // Whether to exclude specified months
    private boolean excludeDaysOfMonth; // Whether to exclude specified days
    private boolean excludeDaysOfWeek; // Whether to exclude specified days of week
    private boolean excludeTimeRanges; // Whether to exclude specified time ranges

    public TimeFilter() {
        this.years = null;
        this.months = null;
        this.daysOfMonth = null;
        this.daysOfWeek = null;
        this.timeRanges = null;
        this.excludeYears = false;
        this.excludeMonths = false;
        this.excludeDaysOfMonth = false;
        this.excludeDaysOfWeek = false;
        this.excludeTimeRanges = false;
    }

    /**
     * Check if given time matches the filter
     */
    public boolean matches(LocalDateTime dateTime) {
        // Check year
        if (years != null) {
            boolean yearMatch = years.contains(dateTime.getYear());
            if (excludeYears ? yearMatch : !yearMatch) {
                return false;
            }
        }

        // Check month
        if (months != null) {
            boolean monthMatch = months.contains(dateTime.getMonth());
            if (excludeMonths ? monthMatch : !monthMatch) {
                return false;
            }
        }

        // Check day of month
        if (daysOfMonth != null) {
            boolean dayMatch = daysOfMonth.contains(dateTime.getDayOfMonth());
            if (excludeDaysOfMonth ? dayMatch : !dayMatch) {
                return false;
            }
        }

        // Check day of week
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            boolean weekDayMatch = daysOfWeek.contains(dateTime.getDayOfWeek());
            if (excludeDaysOfWeek ? weekDayMatch : !weekDayMatch) {
                return false;
            }
        }

        // Check time range
        if (timeRanges != null && !timeRanges.isEmpty()) {
            int hour = dateTime.getHour();
            int minute = dateTime.getMinute();
            int timeInMinutes = hour * 60 + minute;

            boolean inAnyRange = timeRanges.stream()
                .anyMatch(range -> range.contains(timeInMinutes));

            if (excludeTimeRanges ? inAnyRange : !inAnyRange) {
                return false;
            }
        }

        return true;
    }

    // Getters and Setters
    public Set<Integer> getYears() {
        return years;
    }

    public void setYears(Set<Integer> years) {
        this.years = years;
    }

    public void setYears(List<Integer> years) {
        this.years = years != null ? new HashSet<>(years) : null;
    }

    public Set<Month> getMonths() {
        return months;
    }

    public void setMonths(Set<Month> months) {
        this.months = months;
    }

    public Set<Integer> getDaysOfMonth() {
        return daysOfMonth;
    }

    public void setDaysOfMonth(Set<Integer> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public List<TimeRange> getTimeRanges() {
        return timeRanges;
    }

    public void setTimeRanges(List<TimeRange> timeRanges) {
        this.timeRanges = timeRanges;
    }

    public boolean isExcludeYears() {
        return excludeYears;
    }

    public void setExcludeYears(boolean excludeYears) {
        this.excludeYears = excludeYears;
    }

    public boolean isExcludeMonths() {
        return excludeMonths;
    }

    public void setExcludeMonths(boolean excludeMonths) {
        this.excludeMonths = excludeMonths;
    }

    public boolean isExcludeDaysOfMonth() {
        return excludeDaysOfMonth;
    }

    public void setExcludeDaysOfMonth(boolean excludeDaysOfMonth) {
        this.excludeDaysOfMonth = excludeDaysOfMonth;
    }

    public boolean isExcludeDaysOfWeek() {
        return excludeDaysOfWeek;
    }

    public void setExcludeDaysOfWeek(boolean excludeDaysOfWeek) {
        this.excludeDaysOfWeek = excludeDaysOfWeek;
    }

    public boolean isExcludeTimeRanges() {
        return excludeTimeRanges;
    }

    public void setExcludeTimeRanges(boolean excludeTimeRanges) {
        this.excludeTimeRanges = excludeTimeRanges;
    }

    /**
     * Time range inner class
     */
    public static class TimeRange {
        private int startMinutes; // Start time (in minutes)
        private int endMinutes; // End time (in minutes)

        public TimeRange(int startHour, int startMinute, int endHour, int endMinute) {
            this.startMinutes = startHour * 60 + startMinute;
            this.endMinutes = endHour * 60 + endMinute;
        }

        public boolean contains(int timeInMinutes) {
            return timeInMinutes >= startMinutes && timeInMinutes <= endMinutes;
        }

        public int getStartMinutes() {
            return startMinutes;
        }

        public int getEndMinutes() {
            return endMinutes;
        }
    }
}

