package com.bigcomp.accesscontrol.model;

/**
 * 资源类 - 表示需要控制的资源（门、电梯、打印机等）
 */
public class Resource {
    public enum ResourceType {
        DOOR,           // 门
        GATE,           // 大门
        ELEVATOR,       // 电梯
        STAIRWAY,       // 楼梯
        PRINTER,        // 打印机
        BEVERAGE_DISPENSER, // 饮料机
        PARKING         // 停车场
    }

    public enum ResourceState {
        CONTROLLED,     // 受控状态
        UNCONTROLLED    // 非受控状态
    }

    private String id; // 资源唯一标识
    private String name; // 资源名称
    private ResourceType type; // 资源类型
    private String location; // 位置（建筑、楼层等）
    private String building; // 所属建筑
    private String floor; // 所在楼层
    private ResourceState state; // 资源状态
    private String badgeReaderId; // 关联的读卡器ID

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

