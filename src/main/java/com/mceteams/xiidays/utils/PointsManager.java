package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.enums.PointType;
import net.minecraft.world.entity.player.Player;

import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;
import static com.mceteams.xiidays.utils.DataManager.*;

public class PointsManager {
    public static void addPoints(int teamId, PointType type, Player player) {
        reloadData();

        int newPoints = dataReadInt("team_" + teamId + "_points", "total", 0);
        int pointsAdded = 0;

        switch (type) {
            case KILL -> pointsAdded = 50;
            case DEATH -> pointsAdded = -25;
            case MINING -> pointsAdded = 10;
            case FIRST_BLOOD -> pointsAdded = 150;
            case KILL_STREAK -> {
                int streak = dataReadInt("team_" + teamId + "_stats", "kill_streak", 0);
                if (streak < 5) {
                    pointsAdded = 10*streak;
                } else {
                    pointsAdded = 125;
                }
            }
            case CRATE -> pointsAdded = 75;
            case TOTEM -> pointsAdded = 200;
            case CORE_MAZE -> pointsAdded = 300;
            case BLOCKS -> pointsAdded = 5;
            case DISTANCE -> pointsAdded = 1;
            default -> LOGGER.error("PointType non géré: {}", type);
        }

        newPoints += pointsAdded;

        dataModify("team_" + teamId + "_points", "total", newPoints);
        if (type != PointType.DISTANCE && type != PointType.BLOCKS && pointsAdded != 0 && type != PointType.KILL_STREAK) {
            dataModify("team_" + teamId + "_points", "from_3", dataRead("team_" + teamId + "_points", "from_2"));
            dataModify("team_" + teamId + "_points", "from_2", dataRead("team_" + teamId + "_points", "from_1"));
            dataModify("team_" + teamId + "_points", "from_1", player.getName().getString() + ":" + type + ":" + pointsAdded);
        }
    }
}
