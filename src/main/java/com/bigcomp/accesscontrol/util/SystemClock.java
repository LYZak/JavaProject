package com.bigcomp.accesscontrol.util;

import java.time.LocalDateTime;

/**
 * 系统时钟工具类 - 允许修改当前时间用于测试
 */
public class SystemClock {
    private static LocalDateTime customTime = null;

    /**
     * 获取当前时间（如果设置了自定义时间则返回自定义时间）
     */
    public static LocalDateTime now() {
        return customTime != null ? customTime : LocalDateTime.now();
    }

    /**
     * 设置自定义时间（用于测试）
     */
    public static void setCustomTime(LocalDateTime time) {
        customTime = time;
    }

    /**
     * 清除自定义时间，恢复使用系统时间
     */
    public static void clearCustomTime() {
        customTime = null;
    }

    /**
     * 检查是否使用自定义时间
     */
    public static boolean isUsingCustomTime() {
        return customTime != null;
    }
}

