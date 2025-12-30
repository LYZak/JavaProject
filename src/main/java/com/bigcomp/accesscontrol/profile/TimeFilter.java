package com.bigcomp.accesscontrol.profile;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * 时间过滤器 - 定义资源访问的时间规则
 */
public class TimeFilter {
    private Set<Integer> years; // 允许的年份集合，null表示所有年份
    private Set<Month> months; // 允许的月份集合，null表示所有月份
    private Set<Integer> daysOfMonth; // 允许的日期集合，null表示所有日期
    private Set<DayOfWeek> daysOfWeek; // 允许的星期集合，null表示所有星期
    private List<TimeRange> timeRanges; // 允许的时间范围列表，null表示所有时间

    private boolean excludeYears; // 是否排除指定的年份
    private boolean excludeMonths; // 是否排除指定的月份
    private boolean excludeDaysOfMonth; // 是否排除指定的日期
    private boolean excludeDaysOfWeek; // 是否排除指定的星期
    private boolean excludeTimeRanges; // 是否排除指定的时间范围

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
     * 检查给定的时间是否匹配过滤器
     */
    public boolean matches(LocalDateTime dateTime) {
        // 检查年份
        if (years != null) {
            boolean yearMatch = years.contains(dateTime.getYear());
            if (excludeYears ? yearMatch : !yearMatch) {
                return false;
            }
        }

        // 检查月份
        if (months != null) {
            boolean monthMatch = months.contains(dateTime.getMonth());
            if (excludeMonths ? monthMatch : !monthMatch) {
                return false;
            }
        }

        // 检查日期
        if (daysOfMonth != null) {
            boolean dayMatch = daysOfMonth.contains(dateTime.getDayOfMonth());
            if (excludeDaysOfMonth ? dayMatch : !dayMatch) {
                return false;
            }
        }

        // 检查星期
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            boolean weekDayMatch = daysOfWeek.contains(dateTime.getDayOfWeek());
            if (excludeDaysOfWeek ? weekDayMatch : !weekDayMatch) {
                return false;
            }
        }

        // 检查时间范围
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
     * 时间范围内部类
     */
    public static class TimeRange {
        private int startMinutes; // 开始时间（分钟）
        private int endMinutes; // 结束时间（分钟）

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

