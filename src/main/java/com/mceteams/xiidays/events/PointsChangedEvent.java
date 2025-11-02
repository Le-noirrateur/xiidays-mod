package com.mceteams.xiidays.events;

import net.neoforged.bus.api.Event;

public class PointsChangedEvent extends Event {
    private final int teamId;
    private final int newPoints;

    public PointsChangedEvent(int teamId, int newPoints) {
        this.teamId = teamId;
        this.newPoints = newPoints;
    }

    public int getTeamId() {
        return teamId;
    }

    public int getNewPoints() {
        return newPoints;
    }
}
