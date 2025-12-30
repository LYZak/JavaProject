// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.model;

/**
 * User class - Represents a user in the system
 */
public class User {
    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum UserType {
        EMPLOYEE,        // Full-time employee
        CONTRACTOR,      // Contractor
        INTERN,          // Intern
        VISITOR,         // Visitor
        PROJECT_MANAGER  // Project manager
    }

    private String id; // User ID
    private Gender gender;
    private String firstName;
    private String lastName;
    private UserType userType;
    private String badgeId; // Associated badge ID

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

