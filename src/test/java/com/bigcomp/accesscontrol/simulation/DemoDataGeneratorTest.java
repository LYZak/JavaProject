package com.bigcomp.accesscontrol.simulation;

import com.bigcomp.accesscontrol.core.AccessRequestProcessor;
import com.bigcomp.accesscontrol.core.Router;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoDataGeneratorTest {

    @Test
    void generatesUsersAndResources() {
        DatabaseManager db = new DatabaseManager("jdbc:sqlite::memory:");
        ProfileManager profiles = new ProfileManager();
        AccessRequestProcessor arp = new AccessRequestProcessor(db, profiles);
        Router router = new Router(arp);

        com.bigcomp.accesscontrol.core.AccessControlSystem system = mock(com.bigcomp.accesscontrol.core.AccessControlSystem.class);
        when(system.getDatabaseManager()).thenReturn(db);
        when(system.getRouter()).thenReturn(router);
        when(system.getAccessRequestProcessor()).thenReturn(arp);

        DemoDataGenerator generator = new DemoDataGenerator();
        DemoDataGenerator.Result result = generator.generate(system, 10, 12, 1L, null);

        assertEquals(10, result.getUsersCreated());
        assertEquals(12, result.getResourcesCreated());
        assertEquals(12, result.getReadersCreated());

        assertTrue(db.loadAllUsers().size() >= 10);
        assertTrue(db.loadAllResources().size() >= 12);
        assertTrue(router.getBadgeReaders().size() >= 12);
    }
}

