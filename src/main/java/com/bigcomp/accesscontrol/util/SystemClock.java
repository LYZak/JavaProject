// Group 2 ChenGong ZhangZhao LiangYizhuo
package com.bigcomp.accesscontrol.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * System Clock utility class - Allows modifying current time for testing
 */
public class SystemClock {
    private static boolean useCustomTime = false;
    private static LocalDateTime baseCustomTime = null;
    private static long baseRealTimeMillis = 0L;
    private static double timeScale = 1.0;

    /**
     * Get current time (returns custom time if set)
     */
    public static LocalDateTime now() {
        if (!useCustomTime || baseCustomTime == null) {
            return LocalDateTime.now();
        }
        long realDeltaMillis = System.currentTimeMillis() - baseRealTimeMillis;
        long scaledDeltaMillis = (long) (realDeltaMillis * timeScale);
        return baseCustomTime.plusNanos(scaledDeltaMillis * 1_000_000L);
    }

    /**
     * Set custom time (for testing)
     */
    public static void setCustomTime(LocalDateTime time) {
        useCustomTime = true;
        baseCustomTime = time;
        baseRealTimeMillis = System.currentTimeMillis();
    }

    public static void setTimeScale(double scale) {
        if (!useCustomTime) {
            setCustomTime(LocalDateTime.now());
        } else {
            baseCustomTime = now();
            baseRealTimeMillis = System.currentTimeMillis();
        }
        timeScale = scale <= 0 ? 1.0 : scale;
    }

    public static double getTimeScale() {
        return timeScale;
    }

    public static void step(Duration delta) {
        if (!useCustomTime) {
            setCustomTime(LocalDateTime.now());
        } else {
            baseCustomTime = now();
        }
        baseCustomTime = baseCustomTime.plus(delta);
        baseRealTimeMillis = System.currentTimeMillis();
    }

    /**
     * Clear custom time, resume using system time
     */
    public static void clearCustomTime() {
        useCustomTime = false;
        baseCustomTime = null;
        baseRealTimeMillis = 0L;
        timeScale = 1.0;
    }

    /**
     * Check if using custom time
     */
    public static boolean isUsingCustomTime() {
        return useCustomTime;
    }
}

