package com.bigcomp.accesscontrol.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SystemClockTest {

    @AfterEach
    void tearDown() {
        SystemClock.clearCustomTime();
    }

    @Test
    void stepMovesCustomTimeForward() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        SystemClock.setCustomTime(base);
        SystemClock.step(Duration.ofHours(1));
        LocalDateTime after = SystemClock.now();
        assertTrue(after.isAfter(base.plusMinutes(59)));
    }

    @Test
    void timeScaleAdvancesTimeFasterThanRealTime() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        SystemClock.setCustomTime(base);
        SystemClock.setTimeScale(10.0);

        LocalDateTime t1 = SystemClock.now();
        Thread.sleep(60);
        LocalDateTime t2 = SystemClock.now();

        Duration advanced = Duration.between(t1, t2);
        assertTrue(advanced.toMillis() >= 300);
    }
}

