package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.enums.PointType;
import net.minecraft.world.entity.player.Player;

import static com.mceteams.xiidays.utils.DataManager.*;


public class PointsManager {
    public static void addPoints(int teamId, PointType type, int points, Player player) {
        reloadData();

        int currentPoints = dataReadInt("team_" + teamId + "_points", "total", 0);
        int newPoints = currentPoints + points;

        dataModify("team_" + teamId + "_points", "total", newPoints);
//        dataModify("team_" + teamId + "_points", "", ));
    }
}
