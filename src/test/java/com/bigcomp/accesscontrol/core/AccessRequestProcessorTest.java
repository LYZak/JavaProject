package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.profile.Profile;
import com.bigcomp.accesscontrol.profile.ProfileManager;
import com.bigcomp.accesscontrol.profile.TimeFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessRequestProcessorTest {

    @Mock
    private DatabaseManager dbManager;

    @Mock
    private ProfileManager profileManager;

    private AccessRequestProcessor processor;

    @BeforeEach
    void setUp() {
        // Mock data loading - return empty maps by default to avoid NPE in constructor
        lenient().when(dbManager.loadUsersByBadgeCode()).thenReturn(new HashMap<>());
        lenient().when(dbManager.loadUserProfiles()).thenReturn(new HashMap<>());
        lenient().when(dbManager.loadAllResources()).thenReturn(new HashMap<>());
        lenient().when(dbManager.loadResourceGroups()).thenReturn(new HashMap<>());
        lenient().when(dbManager.loadAllBadges()).thenReturn(new HashMap<>());

        processor = new AccessRequestProcessor(dbManager, profileManager);
    }

    @Test
    void testProcessRequest_BadgeNotFound() {
        AccessRequest request = new AccessRequest("UNKNOWN_BADGE", "READER1", "RES1", LocalDateTime.now());
        
        AccessResponse response = processor.processRequest(request);
        
        assertFalse(response.isGranted());
        assertEquals("Badge not found", response.getMessage());
    }

    @Test
    void testProcessRequest_Success() {
        // Prepare data
        String badgeCode = "BADGE123";
        String userId = "USER1";
        String resourceId = "RES1";
        String groupName = "GROUP1";
        String profileName = "PROFILE1";
        
        // 1. Mock Badge
        Badge badge = new Badge(userId);
        badge.setCode(badgeCode);
        Map<String, Badge> badges = new HashMap<>();
        badges.put(userId, badge);
        when(dbManager.loadAllBadges()).thenReturn(badges);
        
        // 2. Mock User
        User user = new User(userId, User.Gender.MALE, "John", "Doe", User.UserType.EMPLOYEE);
        Map<String, User> users = new HashMap<>();
        users.put(badgeCode, user);
        when(dbManager.loadUsersByBadgeCode()).thenReturn(users);
        
        // 3. Mock Resource
        Resource resource = new Resource(resourceId, "Door", Resource.ResourceType.DOOR, "Loc", "Bldg", "1F");
        resource.setState(Resource.ResourceState.CONTROLLED);
        Map<String, Resource> resources = new HashMap<>();
        resources.put(resourceId, resource);
        when(dbManager.loadAllResources()).thenReturn(resources);
        
        // 4. Mock Resource Group
        Map<String, String> resourceGroups = new HashMap<>();
        resourceGroups.put(resourceId, groupName);
        when(dbManager.loadResourceGroups()).thenReturn(resourceGroups);
        
        // 5. Mock User Profiles
        Map<String, Set<String>> userProfiles = new HashMap<>();
        userProfiles.put(userId, Set.of(profileName));
        when(dbManager.loadUserProfiles()).thenReturn(userProfiles);
        
        // 6. Mock Profile Manager logic
        Profile profile = new Profile(profileName);
        // Use a permissive filter (defaults to all allowed)
        TimeFilter filter = new TimeFilter(); 
        profile.addAccessRight(groupName, filter);
        when(profileManager.getProfile(profileName)).thenReturn(profile);

        // Reload to apply mocks
        processor.reloadData();

        // Execute
        AccessRequest request = new AccessRequest(badgeCode, "READER1", resourceId, LocalDateTime.now());
        AccessResponse response = processor.processRequest(request);

        // Verify
        assertTrue(response.isGranted());
        assertEquals("Access granted", response.getMessage());
    }
    
    @Test
    void testProcessRequest_UncontrolledResource() {
        // Test that uncontrolled resource grants access even without user/profile
        String badgeCode = "BADGE123";
        String userId = "USER1";
        String resourceId = "RES1";
        
        // 1. Mock Badge (Still need badge to be valid for initial check)
        Badge badge = new Badge(userId);
        badge.setCode(badgeCode);
        Map<String, Badge> badges = new HashMap<>();
        badges.put(userId, badge);
        when(dbManager.loadAllBadges()).thenReturn(badges);
        
        // 2. Mock User
        User user = new User(userId, User.Gender.MALE, "John", "Doe", User.UserType.EMPLOYEE);
        Map<String, User> users = new HashMap<>();
        users.put(badgeCode, user);
        when(dbManager.loadUsersByBadgeCode()).thenReturn(users);
        
        // 3. Mock Resource - UNCONTROLLED
        Resource resource = new Resource(resourceId, "Door", Resource.ResourceType.DOOR, "Loc", "Bldg", "1F");
        resource.setState(Resource.ResourceState.UNCONTROLLED);
        Map<String, Resource> resources = new HashMap<>();
        resources.put(resourceId, resource);
        when(dbManager.loadAllResources()).thenReturn(resources);
        
        // Reload
        processor.reloadData();
        
        // Execute
        AccessRequest request = new AccessRequest(badgeCode, "READER1", resourceId, LocalDateTime.now());
        AccessResponse response = processor.processRequest(request);
        
        // Verify
        assertTrue(response.isGranted());
        assertEquals("Resource is in uncontrolled state", response.getMessage());
    }
}
