BigComp Access Control System
=============================

Project Overview
----------------
This is a complete access control system prototype for managing and monitoring access permissions to various resources (doors, elevators, printers, etc.) within buildings. The system supports user management, permission configuration, real-time monitoring, logging, and more.

Quick Start
-----------

Prerequisites
- Java 21 or higher
- Maven 3.6 or higher

Installation Steps

1. Check Java Version
   java -version
   Should display Java 21 or higher

2. Check Maven Version
   mvn -version
   Should display Maven 3.6 or higher

3. Compile Project
   mvn compile

4. Run Project
   mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.Main"
   
   Or package and run:
   mvn package
   java -jar target/access-control-system-1.0.0.jar

User Guide
-----------

Main Interface Overview
After starting the program, you will see a main window with 6 tabs:

1. User Management - Manage users and badges
2. Resource Management - Manage resources and badge readers
3. Profile Configuration - Manage access permission configurations
4. Real-time Monitor - View real-time access events
5. Log Viewer - Search and view historical logs
6. Event Simulation - Simulate access events for testing

Detailed Feature Description

1. User Management

Add User
1. In the "User Management" tab, fill in user information:
   - First Name: User's first name
   - Last Name: User's last name
   - Gender: Select gender (Male/Female/Other)
   - Type: Select user type
     * EMPLOYEE - Full-time employee
     * CONTRACTOR - Contractor
     * INTERN - Intern
     * VISITOR - Visitor
     * PROJECT_MANAGER - Project manager

2. Click "Add User" button
3. User will appear in the user list below

Create Badge
1. Select a user from the user list
2. Click "Create Badge" button
3. System will automatically generate a unique badge code and associate it with the user

Note: Only users with badges can perform access control.

Assign Profile
1. Ensure user has created a badge
2. Select user from user list
3. Click "Assign Profile" button
4. Select profile to assign from dropdown list
5. Profile defines which resource groups the user can access and when

Delete User
1. Select user to delete from user list
2. Click "Delete User" button
3. Confirm deletion
4. Note: Deleting user will also delete the user's badge and related configurations

2. Resource Management

Add Resource
1. In the "Resource Management" tab, fill in resource information:
   - Name: Resource name (e.g., "Main Entrance", "Elevator 1", etc.)
   - Type: Select resource type
     * DOOR - Door
     * GATE - Gate
     * ELEVATOR - Elevator
     * STAIRWAY - Stairway
     * PRINTER - Printer
     * BEVERAGE_DISPENSER - Beverage dispenser
     * PARKING - Parking lot
   - Location: Resource location description
   - Building: Building name
   - Floor: Floor number

2. Click "Add Resource" button

Create Badge Reader
1. Select a resource from resource list
2. Click "Create Badge Reader" button
3. System will create a badge reader and associate it with the resource
4. Badge reader is used to receive badge swipe requests from users

Delete Resource
1. Select resource to delete from resource list
2. Click "Delete Resource" button
3. Confirm deletion
4. Note: Deleting resource will also delete associated badge readers and resource group associations

3. Profile Configuration Management

Profiles define which resource groups users can access and when they can access them.

Create Profile
1. In the "Profile Configuration" tab, click "New Profile"
2. Enter profile name (e.g., "Employee Access", "Visitor Access", etc.)
3. Profile will appear in the left list

Add Access Right
1. Select a profile from the left list
2. Select resource group from "Resource Group" dropdown
3. Click "Add Access Right"
4. Right will appear in the middle table

Edit Time Filter
1. Select a right from the access rights table
2. Click "Edit Time Filter" button
3. In the popup dialog, configure:
   - Allowed Days: Check days allowed for access (Monday to Sunday)
   - Time Range: Set allowed time period (e.g., 8:00 to 18:00)
   - Exclusion Mode:
     * Check "Exclude Selected Days": Means access is NOT allowed on selected days
     * Check "Exclude Time Range": Means access is NOT allowed during the set time range

4. Click "OK" to save settings

Time Filter Examples:
- Weekday Access: Select Monday to Friday, time range 8:00-18:00
- Weekend Access: Select Saturday and Sunday, no time range (means all day)
- Exclude Lunch Break: Select all days, time range 12:00-13:00, check "Exclude Time Range"

Save Profile
1. After editing profile, click "Save Profile" button
2. Profile will be automatically saved to data/profiles/ directory

4. Resource Group Management

Resource groups are used to group resources with the same security level for management.

Create Resource Group
1. In the "Resource Group Management" tab, click "New Resource Group"
2. Enter resource group name and security level
3. Select resources from available resource list to add to group
4. Click "Save Resource Group" button

Associate Resources to Group
1. Select a resource group from resource group list
2. Double-click resources in available resource list, or select multiple resources and click "Add to Resource Group"
3. Resources will be added to the resource group

5. Real-time Monitor

View Site Layout
1. In the "Real-time Monitor" tab, select "Site Layout" from "View" dropdown
2. System will display site layout map
3. Blue dots represent badge reader positions
4. When access attempts occur:
   - Green flashing dot = Access granted
   - Red flashing dot = Access denied

View Floor Plan
1. Select "Office Layout" from "View" dropdown
2. System will display floor layout map
3. You can view positions of offices, meeting rooms, elevators, and other resources

View Real-time Event Log
Right panel will display all access events in real-time, including:
- Timestamp
- Badge Reader ID
- Resource ID
- Authorization status

6. Log Viewer

Search Logs
1. In the "Log Viewer" tab, set search criteria:
   - Start Date: Format yyyy-MM-dd (e.g., 2024-01-01)
   - End Date: Format yyyy-MM-dd
   - Badge Code: Optional, filter by specific badge
   - Resource ID: Optional, filter by specific resource
   - User ID: Optional, filter by specific user
   - Status: All/Granted/Denied

2. Click "Search" button
3. Results will be displayed in the table below

Export Logs
1. First perform search to get logs to export
2. Click "Export Logs" button
3. Select save location and filename
4. Logs will be exported in CSV format, can be opened with Excel

7. Event Simulation

Used to test system functionality by simulating user badge swipe behavior.

Add Simulated Users
1. In the "Event Simulation" tab, all users are displayed on the left
2. Select one or more users
3. Click "Add User" button
4. Users will be added to simulated user list

Start Simulation
1. Ensure at least one simulated user has been added
2. Ensure there are available badge readers
3. Click "Start Simulation" button
4. System will randomly generate an access event every 2 seconds
5. You can view simulated access attempts in the "Real-time Monitor" tab

Stop Simulation
1. Click "Stop Simulation" button
2. Simulation will stop immediately

Remove Simulated Users
1. Click "Remove User" button
2. Select user to remove from list
3. User will be removed from simulation list

Data Storage
-----------

System data is stored in the following locations:

- Database: data/access_control.db (SQLite database)
  Stores user, badge, resource, badge reader data
  
- Profile Files: data/profiles/ directory
  Each profile is saved as a JSON file
  
- Resource Groups: data/groups/ directory
  Each resource group is saved as a JSON file
  
- Log Files: data/logs/year/month/date.csv
  Organized by year/month/day, e.g.: data/logs/2024/01/2024-01-15.csv

Typical Workflows
-----------------

Scenario 1: Add New Employee and Grant Access

1. Add User
   - Go to "User Management" tab
   - Fill in employee information, select type EMPLOYEE
   - Click "Add User"

2. Create Badge
   - Select the newly added user
   - Click "Create Badge"

3. Create Profile (if not already exists)
   - Go to "Profile Configuration" tab
   - Click "New Profile", name it "Employee Access"
   - Add access rights, select resource group
   - Edit time filter (e.g., weekdays 8:00-18:00)
   - Save profile

4. Assign Profile
   - Return to "User Management" tab
   - Select user, click "Assign Profile"
   - Select "Employee Access" profile

5. Complete: Employee can now access specified resources at specified times

Scenario 2: Add New Resource and Configure Access Control

1. Add Resource
   - Go to "Resource Management" tab
   - Fill in resource information (e.g., name="Server Room", type=DOOR)
   - Click "Add Resource"

2. Create Badge Reader
   - Select the newly added resource
   - Click "Create Badge Reader"

3. Create Resource Group (if needed)
   - Go to "Resource Group Management" tab
   - Create new resource group
   - Add resource ID to resource group

4. Configure Access Rights
   - Go to "Profile Configuration" tab
   - Edit relevant profile, add access rights for new resource group
   - Set time filter (e.g., weekdays only, business hours only)

5. Complete: Resource access control has been configured

Scenario 3: View Access Records

1. View Real-time Access
   - Go to "Real-time Monitor" tab
   - Observe real-time feedback of access attempts (green/red flashing dots)

2. Search Historical Logs
   - Go to "Log Viewer" tab
   - Set date range and other filter criteria
   - Click "Search"

3. Export Logs
   - After search completes, click "Export Logs"
   - Select save location
   - Open CSV file with Excel for analysis

Important Notes
---------------

1. First Run: System will automatically create database and necessary directory structure

2. Data Backup: Regularly backup data/ directory, especially access_control.db file

3. Profile File Format: Profile files use JSON format, ensure correct format when manually editing

4. Time Filter:
   - If no days are selected, means all days are allowed
   - If no time range is set, means all times are allowed
   - Exclusion mode reverses selection logic

5. Access Control Flow:
   - User must have a badge
   - Badge must be associated with a profile
   - Resource must belong to a resource group
   - Profile must include access rights for that resource group
   - Access time must comply with time filter rules

Frequently Asked Questions
--------------------------

Q: User not visible after adding?
A: Ensure user has created a badge. System by default only displays users with badges.

Q: Access always denied?
A: Check the following:
1. Does user have a badge?
2. Is badge associated with a profile?
3. Does resource belong to a resource group?
4. Does profile include access rights for that resource group?
5. Does current time comply with time filter rules?

Q: No search results in logs?
A: 
1. Check if date format is correct (yyyy-MM-dd)
2. Confirm access events occurred on that date
3. Check if filter criteria are too strict

Q: Profile save failed?
A: 
1. Check if data/profiles/ directory exists and has write permissions
2. Ensure profile name does not contain special characters

Q: Simulator not working?
A: 
1. Ensure simulated users have been added
2. Ensure there are available badge readers
3. Check if simulator has been started

Technical Architecture
----------------------

System Components

- Model Layer (model/): Data models (User, Badge, Resource, etc.)
- Core Layer (core/): Business logic (access control, routing, etc.)
- Database Layer (database/): Data persistence
- Profile Layer (profile/): Permission configuration management
- Logging Layer (logging/): Access log recording
- GUI Layer (gui/): Graphical user interface
- Simulation Layer (simulation/): Event simulation

Data Flow

1. Access Request: User badge swipe -> Badge reader -> Router
2. Permission Verification: Router -> Access request processor -> Check permissions
3. Response Processing: Processor -> Router -> Badge reader -> Control resource
4. Log Recording: All access events automatically recorded to log files

Development Notes
-----------------

Compile Project
mvn clean compile

Run Tests
mvn test

Package Project
mvn clean package

Project Structure
chenlab/
├── pom.xml                          # Maven configuration
├── README.txt                        # This file
├── data/                            # Data directory (auto-created)
│   ├── access_control.db           # SQLite database
│   ├── profiles/                    # Profile directory
│   ├── groups/                     # Resource group directory
│   └── logs/                       # Log directory
└── src/main/java/...               # Source code

Version Information
-------------------

- Version: 1.0.0
- Java Version: 21
- Database: SQLite 3.44.1.0
- JSON Processing: Jackson 2.16.0

Support
-------

For questions or suggestions, please refer to code comments or contact the development team.

License
-------

This project is a prototype system, for learning and demonstration purposes only.
