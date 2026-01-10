package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.PriorityPolicy;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import com.bigcomp.accesscontrol.profile.UsageLimit;
import com.bigcomp.accesscontrol.util.SystemClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccessPolicyEnforcementTest {

    @AfterEach
    void tearDown() {
        SystemClock.clearCustomTime();
    }

    @Test
    void enforcesPriorityAndUsageLimits() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 1, 10, 9, 0);
        SystemClock.setCustomTime(now);

        DatabaseManager db = new DatabaseManager("jdbc:sqlite::memory:");
        Path dir = Files.createTempDirectory("profiles");
        ProfileManager pm = new ProfileManager(dir.toString());
        try {

        Profile profile = new Profile("P1");
        profile.addAccessRight("G_PUBLIC", new TimeFilter());
        profile.addAccessRight("G_OFFICE", new TimeFilter());
        profile.addAccessRight("G_EQUIP", new TimeFilter());

        UsageLimit drinkLimit = new UsageLimit();
        drinkLimit.setPerUserPerDayMax(1);
        drinkLimit.setPerUserPerDayPerResource(true);
        profile.setUsageLimitsByResourceType(Map.of(Resource.ResourceType.BEVERAGE_DISPENSER.name(), drinkLimit));

        PriorityPolicy priority = new PriorityPolicy();
        priority.setRequireGateWithinMinutesEnabled(true);
        priority.setRequireGateWithinMinutes(60);
        priority.setBuildings(List.of("Main Office Building"));
        profile.setPriorityPolicy(priority);

        pm.saveProfile(profile);

        String userId = "U1";
        String badgeId = UUID.randomUUID().toString();
        User user = new User(userId, User.Gender.MALE, "A", "B", User.UserType.EMPLOYEE);
        user.setBadgeId(badgeId);
        db.addUser(user);

        Badge badge = new Badge("CODE1234", userId, now.minusDays(1), now.plusDays(10), now, true);
        db.addBadge(badge, badgeId);
        db.linkBadgeToProfile(badgeId, "P1");

        Resource gate = new Resource("RGATE", "Gate", Resource.ResourceType.GATE, "Site", "Main Office Building", "1F");
        gate.setState(Resource.ResourceState.CONTROLLED);
        db.addResource(gate);
        db.linkResourceToGroup("RGATE", "G_PUBLIC");

        Resource door = new Resource("RDOOR", "Door", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "2F");
        door.setState(Resource.ResourceState.CONTROLLED);
        db.addResource(door);
        db.linkResourceToGroup("RDOOR", "G_OFFICE");

        Resource drink = new Resource("RDRINK", "Drink", Resource.ResourceType.BEVERAGE_DISPENSER, "Main Office Building", "Main Office Building", "2F");
        drink.setState(Resource.ResourceState.CONTROLLED);
        db.addResource(drink);
        db.linkResourceToGroup("RDRINK", "G_EQUIP");

        AccessRequestProcessor arp = new AccessRequestProcessor(db, pm);

        AccessResponse deniedDoor = arp.processRequest(new AccessRequest(badge.getCode(), "BR1", "RDOOR", now));
        assertFalse(deniedDoor.isGranted());
        assertTrue(deniedDoor.getMessage().toLowerCase().contains("priority"), deniedDoor.getMessage());

        AccessResponse grantGate = arp.processRequest(new AccessRequest(badge.getCode(), "BR2", "RGATE", now));
        assertTrue(grantGate.isGranted());

        AccessResponse grantDoor = arp.processRequest(new AccessRequest(badge.getCode(), "BR1", "RDOOR", now.plusMinutes(1)));
        assertTrue(grantDoor.isGranted());

        AccessResponse drink1 = arp.processRequest(new AccessRequest(badge.getCode(), "BR3", "RDRINK", now.plusMinutes(2)));
        assertTrue(drink1.isGranted());

        AccessResponse drink2 = arp.processRequest(new AccessRequest(badge.getCode(), "BR3", "RDRINK", now.plusMinutes(3)));
        assertFalse(drink2.isGranted());
        assertTrue(drink2.getMessage().toLowerCase().contains("usage limit"));
        } finally {
            db.close();
        }
    }
}
