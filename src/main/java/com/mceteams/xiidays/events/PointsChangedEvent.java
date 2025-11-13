package com.mceteams.xiidays.events;

import net.neoforged.bus.api.Event;

public class PointsChangedEvent extends Event {
    private final int teamId;
    private final int oldPoints;
    private final int newPoints;
    private final int deltaPoints;

    public PointsChangedEvent(int teamId, int oldPoints, int newPoints) {
        this.teamId = teamId;
        this.oldPoints = oldPoints;
        this.newPoints = newPoints;
        this.deltaPoints = newPoints - oldPoints;
    }

    public int getTeamId() { return teamId; }
    public int getOldPoints() { return oldPoints; }
    public int getNewPoints() { return newPoints; }
    public int getDeltaPoints() { return deltaPoints; }
}