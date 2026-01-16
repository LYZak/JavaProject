# BigComp Access Control System

This is a comprehensive access control system designed for BigComp headquarters, developed in Java, featuring highly configurable permission management, real-time monitoring, and simulation verification.

## Project Overview

This system aims to solve complex personnel flow and resource access control issues within large office campuses. By organically combining Users, Resources, and Permission Profiles, it achieves fine-grained access admission management.

## Core Features

- **👤 User Management**: Supports CRUD operations for user information and manages Badge binding status.
- **🏢 Resource Management**: Hierarchical management of campus resources such as buildings, floors, specific rooms, and equipment.
- **📁 Resource Group Management**: Supports partitioning resources into logical groups to simplify large-scale permission allocation.
- **🔐 Permission Profiles**:
    - **Time Filtering**: Access restrictions based on week, day, and specific time slots.
    - **Usage Limits**: Supports access frequency control per user per day per resource.
    - **Priority Policies**: Handles conflicts during complex permission overlays.
- **🖥️ Real-time Monitoring**: Graphical display of campus floor plans with real-time reporting of access events and alarms.
- **🧪 Access Simulation**: Simulates card swiping behavior to verify if permission configurations meet expected logic.
- **📋 Audit Logs**: Records all access requests (granted/denied) and system operation logs.
- **🌐 Multi-language Support**: Supports dynamic switching between Chinese and English interfaces.

## Technology Stack

- **Language**: Java 21
- **Interface**: Java Swing + FlatLaf (Modern UI Theme)
- **Database**: SQLite (via JDBC driver)
- **Data Serialization**: Jackson (for Profile JSON storage and backup)
- **Build Tool**: Maven 3.x
- **Testing**: JUnit 5, Mockito

## Quick Start

### Prerequisites
- JDK 21 or higher
- Maven 3.6+

### Build and Package
Run the following command in the project root directory:
```bash
mvn clean package -DskipTests
```
After completion, the generated package is located at `target/access-control-system-1.0.0.jar`.

### Running the Program
```bash
java -jar target/access-control-system-1.0.0.jar
```

## Project Structure
- `src/main/java/com/bigcomp/accesscontrol/`
    - `core/`: Core system logic (access processors, database management, model classes).
    - `gui/`: Graphical User Interface implementation.
    - `profile/`: Permission profile management logic.
- `data/`: Stores SQLite database files and Profile JSON configuration files.
- `target/`: Compilation output and packaged files.

## Development Team
Group 2: ChenGong, ZhangZhao, LiangYizhuo
