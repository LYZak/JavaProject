#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script to add group name comment and translate Chinese to English in Java files
"""

import os
import re
from pathlib import Path

GROUP_COMMENT = "// Group 2 ChenGong ZhangZhao LiangYiKuo\n"

# Translation dictionary for common Chinese strings
TRANSLATIONS = {
    # Comments
    "数据库管理器": "Database Manager",
    "负责所有数据库操作": "Handles all database operations",
    "初始化数据库": "Initialize database",
    "创建所有必要的表": "Create all necessary tables",
    "创建所有数据库表": "Create all database tables",
    "用户表": "Users table",
    "徽章表": "Badges table",
    "资源表": "Resources table",
    "读卡器表": "Badge readers table",
    "资源组表": "Resource groups table",
    "配置文件表": "Profiles table",
    "徽章-配置文件关联表": "Badge-Profile association table",
    "资源-组关联表": "Resource-Group association table",
    "多对多": "Many-to-many",
    "访问请求处理器": "Access Request Processor",
    "核心访问控制逻辑": "Core access control logic",
    "必须在内存中高效处理请求": "Must process requests efficiently in memory",
    "不能访问数据库": "Cannot access database",
    "内存中的快速查找结构": "In-memory fast lookup structures",
    "通过徽章代码查找用户": "Find user by badge code",
    "用户ID -> 配置文件名称集合": "User ID -> Profile name set",
    "资源ID -> 资源对象": "Resource ID -> Resource object",
    "资源ID -> 组名称": "Resource ID -> Group name",
    "将数据加载到内存中以便快速访问": "Load data into memory for fast access",
    "从数据库加载所有数据到内存": "Load all data from database into memory",
    "处理访问请求": "Process access request",
    "访问请求": "Access request",
    "访问响应": "Access response",
    "查找用户": "Find user",
    "检查资源状态": "Check resource status",
    "获取用户的配置文件": "Get user profiles",
    "获取资源所属的组": "Get resource group",
    "检查访问权限": "Check access permissions",
    "记录日志": "Log event",
    "重新加载内存中的数据": "Reload data in memory",
    "构建拒绝原因的详细说明": "Build detailed denial reason",
    "主程序入口": "Main program entry point",
    "主窗口": "Main Window",
    "访问控制系统的GUI主界面": "GUI main interface for access control system",
    "创建共享的访问控制系统实例": "Create shared access control system instance",
    "添加各个功能标签页": "Add functional tabs",
    "传递共享的系统实例": "Pass shared system instance",
    "用户管理": "User Management",
    "资源管理": "Resource Management",
    "资源组管理": "Resource Group Management",
    "配置文件": "Profile Management",
    "实时监控": "Real-time Monitor",
    "日志查看": "Log Viewer",
    "事件模拟": "Event Simulation",
    "文件": "File",
    "退出": "Exit",
    
    # Error messages
    "数据库初始化失败": "Database initialization failed",
    "未找到用户": "User not found",
    "资源不存在": "Resource does not exist",
    "资源处于非受控状态": "Resource is in uncontrolled state",
    "用户没有配置访问权限": "User has no access permissions configured",
    "资源不属于任何组": "Resource does not belong to any group",
    "配置文件不存在": "Profile does not exist",
    "配置文件": "Profile",
    "未配置资源组": "Resource group not configured",
    "访问已授权": "Access granted",
    "访问被拒绝": "Access denied",
    "权限不足": "Insufficient permissions",
}

def add_group_comment(file_path):
    """Add group comment at the first line of the file"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if group comment already exists
    if content.startswith(GROUP_COMMENT.strip()):
        return False
    
    # Add group comment at the beginning
    new_content = GROUP_COMMENT + content
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    return True

def translate_chinese_in_file(file_path):
    """Translate Chinese comments and strings to English"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Translate comments (/** ... */ and // ...)
    for chinese, english in TRANSLATIONS.items():
        # In JavaDoc comments
        content = content.replace(f"* {chinese}", f"* {english}")
        content = content.replace(f"*{chinese}", f"* {english}")
        # In single-line comments
        content = content.replace(f"// {chinese}", f"// {english}")
        # In strings (be careful with quotes)
        content = re.sub(r'"' + re.escape(chinese) + r'"', f'"{english}"', content)
        content = re.sub(r"'" + re.escape(chinese) + r"'", f"'{english}'", content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def process_java_files():
    """Process all Java files in src/main/java"""
    java_files = list(Path("src/main/java").rglob("*.java"))
    print(f"Found {len(java_files)} Java files")
    
    for java_file in java_files:
        print(f"Processing {java_file}...")
        add_group_comment(java_file)
        translate_chinese_in_file(java_file)
    
    print("Done!")

if __name__ == "__main__":
    process_java_files()

