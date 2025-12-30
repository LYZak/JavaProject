// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.logging;

import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Log Manager - Responsible for recording and searching access logs
 */
public class LogManager {
    private static final String LOGS_BASE_DIR = "data/logs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy,MMM,dd,EEE");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Log access event
     */
    public void logAccess(AccessRequest request, User user, Resource resource, boolean granted) {
        LocalDateTime timestamp = request.getTimestamp();
        String status = granted ? "GRANTED" : "DENIED";

        // Build log line
        String logLine = String.format("%s,%s,%s,%s,%s,%s:%s,%s%n",
            DATE_FORMATTER.format(timestamp),
            TIME_FORMATTER.format(timestamp),
            request.getBadgeCode(),
            request.getBadgeReaderId(),
            resource.getId(),
            user.getId(),
            user.getFullName(),
            status
        );

        // Determine log file path
        Path logFile = getLogFilePath(timestamp);

        try {
            // Ensure directory exists
            Files.createDirectories(logFile.getParent());

            // Write log (append mode)
            Files.writeString(logFile, logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    /**
     * Get log file path (organized by year/month/day)
     */
    private Path getLogFilePath(LocalDateTime dateTime) {
        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        String fileName = FILE_DATE_FORMATTER.format(dateTime) + ".csv";

        return Paths.get(LOGS_BASE_DIR, String.valueOf(year), String.format("%02d", month), fileName);
    }

    /**
     * Search logs
     */
    public List<LogEntry> searchLogs(LogSearchCriteria criteria) throws IOException {
        List<LogEntry> results = new ArrayList<>();

        // Determine date range to search
        LocalDateTime startDate = criteria.getStartDate();
        LocalDateTime endDate = criteria.getEndDate();

        LocalDateTime current = startDate;
        while (!current.isAfter(endDate)) {
            Path logFile = getLogFilePath(current);
            if (Files.exists(logFile)) {
                List<String> lines = Files.readAllLines(logFile);
                for (String line : lines) {
                    LogEntry entry = parseLogLine(line);
                    if (entry != null && matchesCriteria(entry, criteria)) {
                        results.add(entry);
                    }
                }
            }
            current = current.plusDays(1);
        }

        return results;
    }

    /**
     * Parse log line
     * Log format: yyyy,MMM,dd,EEE,HH:mm:ss,badgeCode,badgeReaderId,resourceId,userId:userName,status
     */
    private LogEntry parseLogLine(String line) {
        try {
            line = line.trim();
            if (line.isEmpty()) {
                return null;
            }
            
            String[] parts = line.split(",");
            if (parts.length >= 10) {
                // Parse date and time
                int year = Integer.parseInt(parts[0]);
                String monthStr = parts[1];
                int day = Integer.parseInt(parts[2]);
                // parts[3] is day of week, skip
                String timeStr = parts[4];
                String[] timeParts = timeStr.split(":");
                if (timeParts.length < 3) {
                    return null;
                }
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                int second = Integer.parseInt(timeParts[2]);

                LocalDateTime timestamp = LocalDateTime.of(year, 
                    parseMonth(monthStr), day, hour, minute, second);

                // Parse user ID and user name (format: userId:userName)
                String userIdAndName = parts[8];
                String userId;
                String userName;
                if (userIdAndName.contains(":")) {
                    String[] userParts = userIdAndName.split(":", 2);
                    userId = userParts[0];
                    userName = userParts.length > 1 ? userParts[1] : "";
                } else {
                    userId = userIdAndName;
                    userName = "";
                }

                return new LogEntry(
                    timestamp,
                    parts[5], // badge code
                    parts[6], // badge reader id
                    parts[7], // resource id
                    userId,
                    userName,
                    "GRANTED".equals(parts[9].trim()) // granted
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to parse log line: " + line + " - " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Parse month string
     */
    private int parseMonth(String monthStr) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(monthStr)) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * Check if log entry matches search criteria
     */
    private boolean matchesCriteria(LogEntry entry, LogSearchCriteria criteria) {
        if (criteria.getBadgeCode() != null && 
            !entry.getBadgeCode().equals(criteria.getBadgeCode())) {
            return false;
        }
        if (criteria.getResourceId() != null && 
            !entry.getResourceId().equals(criteria.getResourceId())) {
            return false;
        }
        if (criteria.getUserId() != null && 
            !entry.getUserId().equals(criteria.getUserId())) {
            return false;
        }
        if (criteria.getGranted() != null && 
            entry.isGranted() != criteria.getGranted()) {
            return false;
        }
        return true;
    }

    /**
     * Log entry class
     */
    public static class LogEntry {
        private LocalDateTime timestamp;
        private String badgeCode;
        private String badgeReaderId;
        private String resourceId;
        private String userId;
        private String userName;
        private boolean granted;

        public LogEntry(LocalDateTime timestamp, String badgeCode, String badgeReaderId,
                       String resourceId, String userId, String userName, boolean granted) {
            this.timestamp = timestamp;
            this.badgeCode = badgeCode;
            this.badgeReaderId = badgeReaderId;
            this.resourceId = resourceId;
            this.userId = userId;
            this.userName = userName;
            this.granted = granted;
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getBadgeCode() { return badgeCode; }
        public String getBadgeReaderId() { return badgeReaderId; }
        public String getResourceId() { return resourceId; }
        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public boolean isGranted() { return granted; }
    }

    /**
     * Log search criteria class
     */
    public static class LogSearchCriteria {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String badgeCode;
        private String resourceId;
        private String userId;
        private Boolean granted;

        // Getters and Setters
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

        public String getBadgeCode() { return badgeCode; }
        public void setBadgeCode(String badgeCode) { this.badgeCode = badgeCode; }

        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public Boolean getGranted() { return granted; }
        public void setGranted(Boolean granted) { this.granted = granted; }
    }
}

