// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.util;

import java.time.LocalDateTime;

/**
 * System Clock utility class - Allows modifying current time for testing
 */
public class SystemClock {
    private static LocalDateTime customTime = null;

    /**
     * Get current time (returns custom time if set)
     */
    public static LocalDateTime now() {
        return customTime != null ? customTime : LocalDateTime.now();
    }

    /**
     * Set custom time (for testing)
     */
    public static void setCustomTime(LocalDateTime time) {
        customTime = time;
    }

    /**
     * Clear custom time, resume using system time
     */
    public static void clearCustomTime() {
        customTime = null;
    }

    /**
     * Check if using custom time
     */
    public static boolean isUsingCustomTime() {
        return customTime != null;
    }
}

