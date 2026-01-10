package com.bigcomp.accesscontrol.profile;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TimeFilterTest {

    @Test
    void testDayOfWeekFilter() {
        TimeFilter filter = new TimeFilter();
        filter.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        
        // Monday should match
        LocalDateTime monday = LocalDateTime.of(2023, 10, 23, 10, 0); // 2023-10-23 is Monday
        assertTrue(filter.matches(monday));
        
        // Friday should match
        LocalDateTime friday = LocalDateTime.of(2023, 10, 27, 10, 0); // 2023-10-27 is Friday
        assertTrue(filter.matches(friday));
        
        // Tuesday should not match
        LocalDateTime tuesday = LocalDateTime.of(2023, 10, 24, 10, 0);
        assertFalse(filter.matches(tuesday));
    }

    @Test
    void testExcludeDayOfWeekFilter() {
        TimeFilter filter = new TimeFilter();
        filter.setDaysOfWeek(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        filter.setExcludeDaysOfWeek(true);
        
        // Monday should match (not excluded)
        LocalDateTime monday = LocalDateTime.of(2023, 10, 23, 10, 0);
        assertTrue(filter.matches(monday));
        
        // Saturday should not match (excluded)
        LocalDateTime saturday = LocalDateTime.of(2023, 10, 28, 10, 0);
        assertFalse(filter.matches(saturday));
    }

    @Test
    void testTimeRangeFilter() {
        TimeFilter filter = new TimeFilter();
        // 9:00 - 17:00
        filter.setTimeRanges(List.of(new TimeFilter.TimeRange(9, 0, 17, 0)));
        
        // 10:00 should match
        LocalDateTime inside = LocalDateTime.of(2023, 10, 23, 10, 0);
        assertTrue(filter.matches(inside));
        
        // 9:00 should match (inclusive)
        LocalDateTime start = LocalDateTime.of(2023, 10, 23, 9, 0);
        assertTrue(filter.matches(start));
        
        // 17:00 should match (inclusive)
        LocalDateTime end = LocalDateTime.of(2023, 10, 23, 17, 0);
        assertTrue(filter.matches(end));
        
        // 8:59 should not match
        LocalDateTime before = LocalDateTime.of(2023, 10, 23, 8, 59);
        assertFalse(filter.matches(before));
        
        // 17:01 should not match
        LocalDateTime after = LocalDateTime.of(2023, 10, 23, 17, 1);
        assertFalse(filter.matches(after));
    }

    @Test
    void testMultipleTimeRanges() {
        TimeFilter filter = new TimeFilter();
        // 9:00-12:00 AND 14:00-18:00
        filter.setTimeRanges(List.of(
            new TimeFilter.TimeRange(9, 0, 12, 0),
            new TimeFilter.TimeRange(14, 0, 18, 0)
        ));
        
        assertTrue(filter.matches(LocalDateTime.of(2023, 10, 23, 10, 0)));
        assertTrue(filter.matches(LocalDateTime.of(2023, 10, 23, 15, 0)));
        assertFalse(filter.matches(LocalDateTime.of(2023, 10, 23, 13, 0))); // Lunch break
    }
}
