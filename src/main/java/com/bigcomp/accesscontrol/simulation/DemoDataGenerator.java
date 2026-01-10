package com.bigcomp.accesscontrol.simulation;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;

import java.util.Random;
import java.util.UUID;

public class DemoDataGenerator {

    public interface ProgressListener {
        void onProgress(int percent, String message);
    }

    public static final class Result {
        private final int usersCreated;
        private final int resourcesCreated;
        private final int readersCreated;

        public Result(int usersCreated, int resourcesCreated, int readersCreated) {
            this.usersCreated = usersCreated;
            this.resourcesCreated = resourcesCreated;
            this.readersCreated = readersCreated;
        }

        public int getUsersCreated() {
            return usersCreated;
        }

        public int getResourcesCreated() {
            return resourcesCreated;
        }

        public int getReadersCreated() {
            return readersCreated;
        }
    }

    public Result generate(AccessControlSystem system, int userCount, int resourceCount, long seed, ProgressListener progress) {
        DatabaseManager dbManager = system.getDatabaseManager();
        var router = system.getRouter();
        Random random = new Random(seed);

        String profileName = "profile.default.employee";
        String groupPublic = "access.right.public_area";
        String groupOffice = "access.right.office_area";
        String groupEquipment = "access.right.equipment_resources";

        int usersCreated = 0;
        for (int i = 1; i <= userCount; i++) {
            String userId = String.format("SIMU%04d", i);
            User.Gender gender = (i % 2 == 0) ? User.Gender.MALE : User.Gender.FEMALE;
            User.UserType userType = User.UserType.EMPLOYEE;
            User user = new User(userId, gender, "User", String.valueOf(i), userType);
            String badgeId = UUID.randomUUID().toString();
            user.setBadgeId(badgeId);
            try {
                dbManager.addUser(user);
                Badge badge = new Badge(userId);
                dbManager.addBadge(badge, badgeId);
                dbManager.linkBadgeToProfile(badgeId, profileName);
                usersCreated++;
                if (progress != null && i % 25 == 0) {
                    int percent = (int) Math.min(50, (usersCreated * 50L) / Math.max(1, userCount));
                    progress.onProgress(percent, "Users: " + usersCreated + "/" + userCount);
                }
            } catch (Exception ex) {
                if (progress != null) {
                    progress.onProgress(0, "Failed to create user " + userId + ": " + ex.getMessage());
                }
            }
        }

        int resourcesCreated = 0;
        int readersCreated = 0;
        for (int i = 1; i <= resourceCount; i++) {
            String resourceId = String.format("SIMR%04d", i);
            String readerId = String.format("SIMBR%04d", i);

            Resource.ResourceType type;
            String name;
            String location;
            String building;
            String floor;
            String groupName;

            int mod = i % 10;
            if (mod == 1) {
                type = Resource.ResourceType.GATE;
                name = "Gate " + i;
                location = "Site";
                building = "Main Office Building";
                floor = "1F";
                groupName = groupPublic;
            } else if (mod == 2) {
                type = Resource.ResourceType.PARKING;
                name = "Parking " + i;
                location = "Parking";
                building = "Parking Lot";
                floor = "Ground";
                groupName = groupPublic;
            } else if (mod == 3) {
                type = Resource.ResourceType.PRINTER;
                name = "Printer " + i;
                location = "Main Office Building";
                building = "Main Office Building";
                floor = (i % 2 == 0) ? "2F" : "3F";
                groupName = groupEquipment;
            } else if (mod == 4) {
                type = Resource.ResourceType.BEVERAGE_DISPENSER;
                name = "Beverage Dispenser " + i;
                location = "Main Office Building";
                building = "Main Office Building";
                floor = (i % 2 == 0) ? "2F" : "3F";
                groupName = groupEquipment;
            } else if (mod == 5) {
                type = Resource.ResourceType.ELEVATOR;
                name = "Elevator " + i;
                location = "Main Office Building";
                building = "Main Office Building";
                floor = "1F";
                groupName = groupOffice;
            } else if (mod == 6) {
                type = Resource.ResourceType.STAIRWAY;
                name = "Stairway " + i;
                location = "Main Office Building";
                building = "Main Office Building";
                floor = "1F";
                groupName = groupOffice;
            } else {
                type = Resource.ResourceType.DOOR;
                name = (mod % 2 == 0) ? ("Office " + i) : ("Meeting Room " + i);
                location = "Main Office Building";
                building = "Main Office Building";
                floor = (i % 3 == 0) ? "3F" : "2F";
                groupName = groupOffice;
            }

            Resource resource = new Resource(resourceId, name, type, location, building, floor);
            resource.setState(Resource.ResourceState.CONTROLLED);
            resource.setBadgeReaderId(readerId);

            try {
                dbManager.addResource(resource);
                resourcesCreated++;
                BadgeReader reader = new BadgeReader(readerId, resourceId);
                dbManager.addBadgeReader(reader);
                router.registerBadgeReader(reader);
                readersCreated++;
                dbManager.linkResourceToGroup(resourceId, groupName);

                if (progress != null && i % 25 == 0) {
                    int percent = 50 + (int) Math.min(50, (resourcesCreated * 50L) / Math.max(1, resourceCount));
                    progress.onProgress(percent, "Resources: " + resourcesCreated + "/" + resourceCount);
                }
            } catch (Exception ex) {
                if (progress != null) {
                    progress.onProgress(0, "Failed to create resource " + resourceId + ": " + ex.getMessage());
                }
            }
        }

        system.getAccessRequestProcessor().reloadData();
        if (progress != null) {
            progress.onProgress(100, "Done");
        }
        return new Result(usersCreated, resourcesCreated, readersCreated);
    }
}
