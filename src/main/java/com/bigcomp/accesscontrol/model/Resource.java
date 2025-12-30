// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.model;

/**
 * Resource class - Represents resources that need to be controlled (doors, elevators, printers, etc.)
 */
public class Resource {
    public enum ResourceType {
        DOOR,           // Door
        GATE,           // Gate
        ELEVATOR,       // Elevator
        STAIRWAY,       // Stairway
        PRINTER,        // Printer
        BEVERAGE_DISPENSER, // Beverage dispenser
        PARKING         // Parking
    }

    public enum ResourceState {
        CONTROLLED,     // Controlled state
        UNCONTROLLED    // Uncontrolled state
    }

    private String id; // Resource unique identifier
    private String name; // Resource name
    private ResourceType type; // Resource type
    private String location; // Location (building, floor, etc.)
    private String building; // Building
    private String floor; // Floor
    private ResourceState state; // Resource state
    private String badgeReaderId; // Associated badge reader ID

    public Resource(String id, String name, ResourceType type, String location, 
                   String building, String floor) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.building = building;
        this.floor = floor;
        this.state = ResourceState.CONTROLLED;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public ResourceState getState() {
        return state;
    }

    public void setState(ResourceState state) {
        this.state = state;
    }

    public String getBadgeReaderId() {
        return badgeReaderId;
    }

    public void setBadgeReaderId(String badgeReaderId) {
        this.badgeReaderId = badgeReaderId;
    }
}

