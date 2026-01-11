package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AccessRequestProcessorCacheTest {
    private static final String CACHE_FILE_PROPERTY = "accesscontrol.cacheFile";
    private static final String CACHE_STRICT_PROPERTY = "accesscontrol.cacheStrict";

    @AfterEach
    void tearDown() {
        System.clearProperty(CACHE_FILE_PROPERTY);
        System.clearProperty(CACHE_STRICT_PROPERTY);
    }

    @Test
    void loadsFromCacheWhenPresent_evenIfDbWasCleared(@TempDir Path tempDir) throws Exception {
        Path cacheFile = tempDir.resolve("access_context.bin");
        System.setProperty(CACHE_FILE_PROPERTY, cacheFile.toString());
        System.setProperty(CACHE_STRICT_PROPERTY, "false");

        Path dbFile = tempDir.resolve("test.db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + dbFile.toString());
        ProfileManager profileManager = new ProfileManager(tempDir.resolve("profiles").toString());
        try {

        String userId = "U1";
        String badgeId = "B1";
        String badgeCode = "ABC12345";
        String resourceId = "R1";
        String groupName = "access.right.office_area";
        String profileName = "profile.default.employee";
        Profile profile = new Profile(profileName);
        profile.addAccessRight(groupName, new TimeFilter());
        profileManager.saveProfile(profile);

        User user = new User(userId, User.Gender.FEMALE, "Jane", "Doe", User.UserType.EMPLOYEE);
        user.setBadgeId(badgeId);
        dbManager.addUser(user);

        LocalDateTime now = LocalDateTime.of(2026, 1, 5, 9, 0);
        Badge badge = new Badge(badgeCode, userId, now.minusDays(1), now.plusYears(1), now.minusHours(1), true);
        dbManager.addBadge(badge, badgeId);
        dbManager.linkBadgeToProfile(badgeId, profileName);

        Resource resource = new Resource(resourceId, "Office Door", Resource.ResourceType.DOOR, "Office", "HQ", "1");
        dbManager.addResource(resource);
        dbManager.linkResourceToGroup(resourceId, groupName);

        AccessRequestProcessor arpFromDb = new AccessRequestProcessor(dbManager, profileManager);
        assertTrue(arpFromDb.processRequest(new AccessRequest(badgeCode, "BR1", resourceId, now)).isGranted());
        arpFromDb.persistContextToCache();
        assertTrue(Files.exists(cacheFile));

        dbManager.deleteUser(userId);
        dbManager.deleteResource(resourceId);

        AccessRequestProcessor arpFromCache = new AccessRequestProcessor(dbManager, profileManager);
        assertEquals(1, arpFromCache.getUsersByBadgeCode().size());
        assertEquals(1, arpFromCache.getResources().size());
        assertTrue(arpFromCache.processRequest(new AccessRequest(badgeCode, "BR1", resourceId, now)).isGranted());
        } finally {
            dbManager.close();
        }
    }

    @Test
    void fallsBackToDbWhenCacheIsCorrupt(@TempDir Path tempDir) throws Exception {
        Path cacheFile = tempDir.resolve("access_context.bin");
        System.setProperty(CACHE_FILE_PROPERTY, cacheFile.toString());
        Files.write(cacheFile, "not-a-valid-cache".getBytes());

        Path dbFile = tempDir.resolve("test.db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + dbFile.toString());
        ProfileManager profileManager = new ProfileManager(tempDir.resolve("profiles").toString());
        try {

        String userId = "U1";
        String badgeId = "B1";
        String badgeCode = "ABC12345";
        String resourceId = "R1";
        String groupName = "access.right.office_area";
        String profileName = "profile.default.employee";
        Profile profile = new Profile(profileName);
        profile.addAccessRight(groupName, new TimeFilter());
        profileManager.saveProfile(profile);

        User user = new User(userId, User.Gender.FEMALE, "Jane", "Doe", User.UserType.EMPLOYEE);
        user.setBadgeId(badgeId);
        dbManager.addUser(user);

        LocalDateTime now = LocalDateTime.of(2026, 1, 5, 9, 0);
        Badge badge = new Badge(badgeCode, userId, now.minusDays(1), now.plusYears(1), now.minusHours(1), true);
        dbManager.addBadge(badge, badgeId);
        dbManager.linkBadgeToProfile(badgeId, profileName);

        Resource resource = new Resource(resourceId, "Office Door", Resource.ResourceType.DOOR, "Office", "HQ", "1");
        dbManager.addResource(resource);
        dbManager.linkResourceToGroup(resourceId, groupName);

        AccessRequestProcessor arp = new AccessRequestProcessor(dbManager, profileManager);
        assertTrue(arp.processRequest(new AccessRequest(badgeCode, "BR1", resourceId, now)).isGranted());
        assertEquals(1, arp.getUsersByBadgeCode().size());
        assertEquals(1, arp.getResources().size());
        } finally {
            dbManager.close();
        }
    }
}
