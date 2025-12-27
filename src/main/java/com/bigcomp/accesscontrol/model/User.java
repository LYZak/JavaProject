package com.bigcomp.accesscontrol.model;

/**
 * 用户类 - 表示系统中的用户
 */
public class User {
    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum UserType {
        EMPLOYEE,        // 正式员工
        CONTRACTOR,      // 承包商
        INTERN,          // 实习生
        VISITOR,         // 访客
        PROJECT_MANAGER  // 项目经理
    }

    private String id; // 用户ID
    private Gender gender;
    private String firstName;
    private String lastName;
    private UserType userType;
    private String badgeId; // 关联的徽章ID

    public User(String id, Gender gender, String firstName, String lastName, UserType userType) {
        this.id = id;
        this.gender = gender;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }
}

