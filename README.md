# BigComp 门禁控制系统 (Access Control System)

这是一个为 BigComp 总部设计的综合门禁控制系统，采用 Java 开发，具备高度可配置的权限管理、实时监控和模拟验证功能。

## 项目概述

本系统旨在解决大型办公园区内复杂的人员流动与资源访问控制问题。通过将人员（User）、资源（Resource）、权限配置（Profile）进行有机结合，实现精细化的访问准入管理。

## 核心功能

- **👤 用户管理**：支持用户信息的增删改查，管理工卡（Badge）绑定状态。
- **🏢 资源管理**：对园区内的建筑物、楼层、具体房间/设备等资源进行分级管理。
- **📁 资源组管理**：支持将资源划分为逻辑组，简化大规模权限分配。
- **🔐 权限配置 (Profiles)**：
    - **时间过滤**：基于周、日、具体时间段的访问限制。
    - **次数限制**：支持每人每天/每资源的访问频次控制。
    - **优先级策略**：处理复杂权限叠加时的冲突。
- **🖥️ 实时监控**：图形化展示园区平面图，实时播报通行事件与警报。
- **🧪 访问模拟**：模拟刷卡行为，验证权限配置是否符合预期逻辑。
- **📋 日志审计**：记录所有通行申请（通过/拒绝）及系统操作日志。
- **🌐 多语言支持**：支持中英文界面动态切换。

## 技术栈

- **语言**：Java 21
- **界面**：Java Swing + FlatLaf (现代 UI 主题)
- **数据库**：SQLite (通过 JDBC 驱动)
- **数据序列化**：Jackson (用于 Profile 的 JSON 存储与备份)
- **构建工具**：Maven 3.x
- **测试**：JUnit 5, Mockito

## 快速开始

### 环境要求
- JDK 21 或更高版本
- Maven 3.6+

### 编译与打包
在项目根目录下执行以下命令：
```bash
mvn clean package -DskipTests
```
打包完成后，生成的包位于 `target/access-control-system-1.0.0.jar`。

### 运行程序
```bash
java -jar target/access-control-system-1.0.0.jar
```

## 项目结构
- `src/main/java/com/bigcomp/accesscontrol/`
    - `core/`: 系统核心逻辑（访问处理器、数据库管理、模型类）。
    - `gui/`: 图形用户界面实现。
    - `profile/`: 权限配置文件管理逻辑。
- `data/`: 存储 SQLite 数据库文件及 Profile JSON 配置文件。
- `target/`: 编译输出与打包文件。

## 开发团队
Group 2: ChenGong, ZhangZhao, LiangYizhuo
