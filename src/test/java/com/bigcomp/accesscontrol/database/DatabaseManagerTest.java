package com.bigcomp.accesscontrol.database;

import com.bigcomp.accesscontrol.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private DatabaseManager dbManager;

    @BeforeEach
    void setUp() {
        // Use in-memory database for testing
        dbManager = new DatabaseManager("jdbc:sqlite::memory:");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    void testAddAndLoadUser() throws SQLException {
        User user = new User("USER1", User.Gender.FEMALE, "Jane", "Doe", User.UserType.PROJECT_MANAGER);
        // Note: badge_id is initially null in constructor, we can set it
        user.setBadgeId("BADGE1");
        
        dbManager.addUser(user);
        
        Map<String, User> users = dbManager.loadAllUsers();
        assertTrue(users.containsKey("USER1"));
        User loadedUser = users.get("USER1");
        assertEquals("Jane", loadedUser.getFirstName());
        assertEquals("Doe", loadedUser.getLastName());
        assertEquals(User.UserType.PROJECT_MANAGER, loadedUser.getUserType());
        assertEquals("BADGE1", loadedUser.getBadgeId());
    }

    @Test
    void testDeleteUser() throws SQLException {
        User user = new User("USER1", User.Gender.MALE, "John", "Smith", User.UserType.EMPLOYEE);
        dbManager.addUser(user);
        
        Map<String, User> usersBefore = dbManager.loadAllUsers();
        assertEquals(1, usersBefore.size());
        
        dbManager.deleteUser("USER1");
        
        Map<String, User> usersAfter = dbManager.loadAllUsers();
        assertTrue(usersAfter.isEmpty());
    }
}
