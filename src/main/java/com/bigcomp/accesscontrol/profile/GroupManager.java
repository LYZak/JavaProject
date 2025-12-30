// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.profile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Resource Group Manager - Responsible for loading, saving and managing resource groups
 */
public class GroupManager {
    private static final String GROUPS_DIR = "data/groups";
    private Map<String, ResourceGroup> groups; // Group name -> Resource group object
    private ObjectMapper objectMapper;

    public GroupManager() {
        this.groups = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        loadGroups();
    }

    /**
     * Load all resource groups from files
     */
    private void loadGroups() {
        try {
            Path groupsPath = Paths.get(GROUPS_DIR);
            if (!Files.exists(groupsPath)) {
                Files.createDirectories(groupsPath);
                return;
            }

            Files.list(groupsPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        ResourceGroup group = loadGroupFromFile(path.toFile());
                        if (group != null) {
                            groups.put(group.getName(), group);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load resource group: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("Failed to load resource group directory: " + e.getMessage());
        }
    }

    /**
     * Load single resource group from file
     */
    private ResourceGroup loadGroupFromFile(File file) throws IOException {
        String content = Files.readString(file.toPath());
        GroupData data = objectMapper.readValue(content, GroupData.class);
        
        ResourceGroup group = new ResourceGroup(data.name, data.securityLevel);
        group.setFilePath(file.getAbsolutePath());
        if (data.resources != null) {
            for (String resourceId : data.resources) {
                group.addResource(resourceId);
            }
        }
        
        return group;
    }

    /**
     * Save resource group to file
     */
    public void saveGroup(ResourceGroup group) throws IOException {
        Path groupsPath = Paths.get(GROUPS_DIR);
        if (!Files.exists(groupsPath)) {
            Files.createDirectories(groupsPath);
        }

        File file = new File(groupsPath.toFile(), group.getName() + ".json");
        GroupData data = new GroupData();
        data.name = group.getName();
        data.securityLevel = group.getSecurityLevel();
        data.resources = group.getResourceIds();
        
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        group.setFilePath(file.getAbsolutePath());
        groups.put(group.getName(), group);
    }

    /**
     * Get resource group
     */
    public ResourceGroup getGroup(String name) {
        return groups.get(name);
    }

    /**
     * Get all resource groups
     */
    public Map<String, ResourceGroup> getAllGroups() {
        return new HashMap<>(groups);
    }

    /**
     * Delete resource group
     */
    public void deleteGroup(String name) throws IOException {
        groups.remove(name);
        File file = new File(GROUPS_DIR, name + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    // Internal data class for JSON serialization
    private static class GroupData {
        public String name;
        public int securityLevel;
        public List<String> resources;
    }
}

