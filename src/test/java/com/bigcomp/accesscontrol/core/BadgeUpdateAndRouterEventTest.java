package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.util.SystemClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BadgeUpdateAndRouterEventTest {

    @AfterEach
    void tearDown() {
        SystemClock.clearCustomTime();
    }

    @Test
    void updateBadgeRemovesOldCodeFromInMemoryIndex() throws Exception {
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite::memory:");
        ProfileManager profileManager = new ProfileManager();

        String userId = "U1";
        String badgeId = "B1";
        String oldCode = "OLD12345";
        String newCode = "NEW12345";
        String resourceId = "R1";
        String groupName = "access.right.office_area";
        String profileName = "profile.default.employee";

        LocalDateTime now = LocalDateTime.of(2026, 1, 5, 9, 0);
        SystemClock.setCustomTime(now);

        User user = new User(userId, User.Gender.FEMALE, "Jane", "Doe", User.UserType.EMPLOYEE);
        user.setBadgeId(badgeId);
        dbManager.addUser(user);

        Badge badgeOld = new Badge(oldCode, userId, now.minusDays(30), now.plusYears(1), now.minusMonths(7), true);
        dbManager.addBadge(badgeOld, badgeId);
        dbManager.linkBadgeToProfile(badgeId, profileName);

        Resource resource = new Resource(resourceId, "Office Door", Resource.ResourceType.DOOR, "Main Office Building", "Main Office Building", "2F");
        dbManager.addResource(resource);
        dbManager.linkResourceToGroup(resourceId, groupName);

        AccessRequestProcessor arp = new AccessRequestProcessor(dbManager, profileManager);

        AccessResponse before = arp.processRequest(new AccessRequest(oldCode, "BR1", resourceId, now));
        assertNotNull(before);

        Badge badgeNew = new Badge(newCode, userId, now.minusDays(30), now.plusYears(1), now, true);
        arp.updateBadge(oldCode, badgeNew);

        AccessResponse oldDenied = arp.processRequest(new AccessRequest(oldCode, "BR1", resourceId, now));
        assertFalse(oldDenied.isGranted());
        assertEquals("Badge not found", oldDenied.getMessage());

        AccessResponse newResp = arp.processRequest(new AccessRequest(newCode, "BR1", resourceId, now));
        assertNotNull(newResp);
    }

    @Test
    void badgeBecomesInvalidAfterUpdateGraceWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);
        SystemClock.setCustomTime(now);

        Badge badge = new Badge("CODE1234", "U1", now.minusYears(1), now.plusYears(1), now.minusMonths(6).minusDays(8), true);
        assertTrue(badge.isUpdateExpired());
        assertFalse(badge.needsUpdate());
        assertFalse(badge.isValid());
    }

    @Test
    void routerForwardsReaderMessageAndActivationEvents() throws Exception {
        AccessRequestProcessor arp = mock(AccessRequestProcessor.class);
        when(arp.processRequest(any())).thenReturn(new AccessResponse("BR1", true, "Access granted"));

        Router router = new Router(arp);
        BadgeReader reader = new BadgeReader("BR1", "R1");
        router.registerBadgeReader(reader);

        List<Router.ReaderEvent> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);
        router.addReaderEventListener(event -> {
            events.add(event);
            latch.countDown();
        });

        reader.handleAccessResponse(new AccessResponse("BR1", true, "Access granted"));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(events.stream().anyMatch(e -> e.getType() == Router.ReaderEventType.MESSAGE));
        assertTrue(events.stream().anyMatch(e -> e.getType() == Router.ReaderEventType.RESOURCE_ACTIVATED));
    }
}

