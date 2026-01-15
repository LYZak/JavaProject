package com.bigcomp.accesscontrol.integration;

import com.bigcomp.accesscontrol.core.AccessRequestProcessor;
import com.bigcomp.accesscontrol.core.Router;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SystemIntegrationTest {

    private DatabaseManager dbManager;
    private ProfileManager profileManager;
    private AccessRequestProcessor arp;
    private Router router;

    @TempDir
    Path tempProfileDir;

    @BeforeEach
    void setUp() {
        // 1. Initialize DB (In-Memory)
        dbManager = new DatabaseManager("jdbc:sqlite::memory:");
        
        // 2. Initialize ProfileManager (Temp Dir)
        profileManager = new ProfileManager(tempProfileDir.toString());
        
        // 3. Initialize ARP and Router
        arp = new AccessRequestProcessor(dbManager, profileManager);
        router = new Router(arp);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    void testFullAccessFlow() throws SQLException, IOException {
        // === Setup Data ===
        
        // 1. Create User & Badge
        User user = new User("U1", User.Gender.FEMALE, "Alice", "Test", User.UserType.EMPLOYEE);
        String badgeId = "BID-1";
        user.setBadgeId(badgeId);
        dbManager.addUser(user);
        
        Badge badge = new Badge(user.getId());
        badge.setCode("CARD-12345");
        dbManager.addBadge(badge, badgeId);
        
        // 2. Create Resource & BadgeReader
        String resourceId = "RES-1";
        Resource resource = new Resource(resourceId, "Front Door", Resource.ResourceType.DOOR, "Entry", "Main", "1F");
        resource.setState(Resource.ResourceState.CONTROLLED);
        String readerId = "READER-1";
        resource.setBadgeReaderId(readerId);
        dbManager.addResource(resource);
        
        BadgeReader reader = new BadgeReader(readerId, resourceId);
        dbManager.addBadgeReader(reader);
        router.registerBadgeReader(reader);
        
        // 3. Create Resource Group and Link Resource
        String groupName = "General Access";
        dbManager.linkResourceToGroup(resourceId, groupName);
        
        // 4. Create Profile and Assign to User
        Profile profile = new Profile("Employee Access");
        TimeFilter filter = new TimeFilter(); // Allow all
        profile.addAccessRight(groupName, filter);
        profileManager.saveProfile(profile);
        
        dbManager.linkBadgeToProfile(badgeId, "Employee Access");
        
        // === Reload Data ===
        arp.reloadData();
        
        // === Simulate Access ===
        
        // We use a listener to capture the router response
        AtomicReference<AccessResponse> capturedResponse = new AtomicReference<>();
        router.addAccessEventListener((req, res) -> capturedResponse.set(res));
        
        // Swipe badge
        reader.swipeBadge(badge);
        
        assertNotNull(capturedResponse.get());
        assertTrue(capturedResponse.get().isGranted());
        assertEquals("Access granted", capturedResponse.get().getMessage());
        
        // === Test Denied Access (Invalid Time) ===
        // Update profile to exclude current time (only allow year 2020)
        TimeFilter strictFilter = new TimeFilter();
        strictFilter.setYears(java.util.Set.of(2020)); 
        profile.addAccessRight(groupName, strictFilter);
        profileManager.saveProfile(profile);
        arp.reloadData(); // Reload to pick up new profile logic
        
        // Reset capture and ensure reader is active (it might be temporarily inactive after first swipe)
        capturedResponse.set(null);
        reader.setActive(true);
        
        reader.swipeBadge(badge);
        
        assertNotNull(capturedResponse.get());
        assertFalse(capturedResponse.get().isGranted());
        // Verify denial reason
        String msg = capturedResponse.get().getMessage();
        assertTrue(msg.contains("Time filter does not allow access") || 
                   msg.contains("is not in allowed list"), 
                   "Unexpected message: " + msg);
    }
}
