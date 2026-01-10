package com.bigcomp.accesscontrol.profile;

import java.util.List;

public class PriorityPolicy {
    private boolean requireGateWithinMinutesEnabled;
    private int requireGateWithinMinutes = 60;
    private List<String> buildings;

    public boolean isRequireGateWithinMinutesEnabled() {
        return requireGateWithinMinutesEnabled;
    }

    public void setRequireGateWithinMinutesEnabled(boolean requireGateWithinMinutesEnabled) {
        this.requireGateWithinMinutesEnabled = requireGateWithinMinutesEnabled;
    }

    public int getRequireGateWithinMinutes() {
        return requireGateWithinMinutes;
    }

    public void setRequireGateWithinMinutes(int requireGateWithinMinutes) {
        this.requireGateWithinMinutes = Math.max(1, requireGateWithinMinutes);
    }

    public List<String> getBuildings() {
        return buildings;
    }

    public void setBuildings(List<String> buildings) {
        this.buildings = buildings;
    }
}

