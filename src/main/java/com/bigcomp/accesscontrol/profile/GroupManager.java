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
 * 资源组管理器 - 负责加载、保存和管理资源组
 */
public class GroupManager {
    private static final String GROUPS_DIR = "data/groups";
    private Map<String, ResourceGroup> groups; // 组名称 -> 资源组对象
    private ObjectMapper objectMapper;

    public GroupManager() {
        this.groups = new HashMap<>();
        this.objectMapper = new ObjectMapper();
        loadGroups();
    }

    /**
     * 从文件加载所有资源组
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
                        System.err.println("加载资源组失败: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("加载资源组目录失败: " + e.getMessage());
        }
    }

    /**
     * 从文件加载单个资源组
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
     * 保存资源组到文件
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
     * 获取资源组
     */
    public ResourceGroup getGroup(String name) {
        return groups.get(name);
    }

    /**
     * 获取所有资源组
     */
    public Map<String, ResourceGroup> getAllGroups() {
        return new HashMap<>(groups);
    }

    /**
     * 删除资源组
     */
    public void deleteGroup(String name) throws IOException {
        groups.remove(name);
        File file = new File(GROUPS_DIR, name + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    // 内部数据类用于JSON序列化
    private static class GroupData {
        public String name;
        public int securityLevel;
        public List<String> resources;
    }
}

