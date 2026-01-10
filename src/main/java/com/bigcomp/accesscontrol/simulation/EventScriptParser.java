package com.bigcomp.accesscontrol.simulation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EventScriptParser {

    public List<String> parseFile(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return parseLines(lines);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> parseLines(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            if (line.contains(",") && !line.contains("user=")) {
                String parsed = parseCsvLine(line);
                if (parsed != null) {
                    out.add(parsed);
                }
            } else {
                out.add(normalizeDsl(line));
            }
        }
        return out;
    }

    private String parseCsvLine(String line) {
        String[] parts = line.split("\\s*,\\s*");
        if (parts.length < 1) {
            return null;
        }
        if (parts[0].equalsIgnoreCase("action")) {
            return null;
        }
        String action = parts[0].trim();
        String badgeCode = parts.length >= 2 ? parts[1].trim() : "";
        String readerId = parts.length >= 3 ? parts[2].trim() : "";
        String resourceId = parts.length >= 4 ? parts[3].trim() : "";
        String count = parts.length >= 5 ? parts[4].trim() : "1";
        if (count.isEmpty()) {
            count = "1";
        }
        return action + " user=" + badgeCode + " reader=" + readerId + " resource=" + resourceId + " x" + count;
    }

    private String normalizeDsl(String line) {
        String normalized = line.replaceAll("\\s+", " ").trim();
        String[] parts = normalized.split(" ");
        if (parts.length == 0) {
            return normalized;
        }
        String action = parts[0];
        String user = "";
        String reader = "";
        String resource = "";
        String count = "1";
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.startsWith("user=")) {
                user = p.substring("user=".length());
            } else if (p.startsWith("reader=")) {
                reader = p.substring("reader=".length());
            } else if (p.startsWith("resource=")) {
                resource = p.substring("resource=".length());
            } else if (p.startsWith("x")) {
                count = p.substring(1);
            }
        }
        return action + " user=" + user + " reader=" + reader + " resource=" + resource + " x" + count;
    }
}

