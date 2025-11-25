package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.enums.PointType;
import com.mceteams.xiidays.events.PointsChangedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mceteams.xiidays.XIIDays.LOGGER;
import static com.mceteams.xiidays.utils.DataManager.*;

public class PointsManager {
    private static final Map<Integer, Integer> teamPoints = new HashMap<>();

    public static void addPoints(int teamId, PointType type, Player player, Object... args) {
        int oldPoints = dataReadInt("team_" + teamId + "_points", "total", 0);
        int newPoints;
        int playerPoints = dataReadInt("player_" + player.getUUID() + "_stats", "team_points", 0);
        int pointsAdded = 0;

        switch (type) {
            case KILL -> pointsAdded = 50; // Points for a kill
            case DEATH -> pointsAdded = -25; // Penalty for death
            case MINING -> { // Points for mining different ores
                String blockType = (String) args[0];
                switch (blockType) {
                    case "DIAMOND_ORE", "NETHERITE_ORE" -> pointsAdded = 100;
                    case "EMERALD_ORE" -> pointsAdded = 75;
                    case "GOLD_ORE" -> pointsAdded = 50;
                    case "IRON_ORE", "AMETHYST_ORE" -> pointsAdded = 25;
                    case "COAL_ORE", "LAPIS_ORE", "REDSTONE_ORE" -> pointsAdded = 10;
                    case "COPPER_ORE" -> pointsAdded = 5;
                }
            }

            case FIRST_BLOOD -> pointsAdded = 2000; // Points for first kill of the game
            case KILL_STREAK -> { // Points for kill streaks
                int streak = dataReadInt("team_" + teamId + "_stats", "kill_streak", 0);

                if (streak >= 3 && streak < 5) {
                    pointsAdded = 25;
                } else if (streak >= 5 && streak < 10) {
                    pointsAdded = 50;
                } else if (streak >= 10) {
                    pointsAdded = 100;
                }
            }

            case CRATE -> pointsAdded = 75; // Points for opening a crate
            case TOTEM -> pointsAdded = 200; // Points for finding a reviving totem
            case CORE_MAZE -> pointsAdded = 300; // Points for completing the core maze
            default -> LOGGER.error("PointType non géré: {}", type); // Log unhandled PointType
        }

        newPoints = oldPoints + pointsAdded; // Update team points
        playerPoints += pointsAdded; // Update player points

        if (pointsAdded != 0) {
            NeoForge.EVENT_BUS.post(new PointsChangedEvent(teamId, oldPoints, newPoints));
        }

        teamPoints.put(teamId, newPoints); // Update in-memory team points

        dataModify("team_" + teamId + "_points", "total", newPoints); // Update team's total points
        dataModify("player_" + player.getUUID() + "_stats", "team_points", playerPoints); // Update player's total points
        if ((pointsAdded >= 25 || type == PointType.DEATH) && type != PointType.KILL_STREAK) { // Log significant point changes
            dataModify("team_" + teamId + "_points", "list_3", dataRead("team_" + teamId + "_points", "list_2"));
            dataModify("team_" + teamId + "_points", "list_2", dataRead("team_" + teamId + "_points", "list_1"));
            dataModify("team_" + teamId + "_points", "list_1", player.getName().getString() + ":" + type + ":" + pointsAdded);
        }

        if (pointsAdded < 0) {
            dataModify("team_" + teamId + "_stats", "pointloss", dataReadInt("team_" + teamId + "_stats", "pointloss", 0) - pointsAdded);
        } else {
            dataModify("team_" + teamId + "_stats", "pointgain", dataReadInt("team_" + teamId + "_stats", "pointgain", 0) + pointsAdded);
        }

        if (player instanceof ServerPlayer srvp) {
            String addedStr = pointsAdded >= 0 ? "§r§2§l+" + pointsAdded : String.valueOf(pointsAdded);
            srvp.sendSystemMessage(Component.literal("Vous avez ajouté §4§l" + addedStr + "§r à votre équipe (§6§l" + type +"§r)"));
            TeamManager.sendMessageToTeam(teamId, Component.literal(player.getName().getString() + " à ajouté §4§l" + addedStr + "§r à votre équipe (§6§l" + type + "§r)"), player.getUUID());
        }
        NeoForge.EVENT_BUS.post(new PointsChangedEvent(teamId, oldPoints, newPoints)); // Trigger event for points change
    }

    // Ajoute cette méthode dans PointsManager.java

    /**
     * Initialise les points de toutes les équipes existantes
     * À appeler au démarrage du serveur
     */
    public static void initializeTeamPoints() {
        String[] allTeams = TeamManager.getAllTeams();

        for (String teamName : allTeams) {
            int teamId = TeamManager.getTeamId(teamName);
            if (teamId > 0) {
                // Charger les points depuis DataManager
                int points = dataReadInt("team_" + teamId + "_points", "total", 0);
                teamPoints.put(teamId, points);
                LOGGER.info("Loaded team {} (ID: {}) with {} points", teamName, teamId, points);
            }
        }

        LOGGER.info("Initialized {} teams in leaderboard", teamPoints.size());
    }

    /**
     * Recharge tous les points depuis DataManager (utile après création d'équipe)
     */
    public static void refreshTeamPoints() {
        teamPoints.clear();
        initializeTeamPoints();
    }

    public static List<Map.Entry<Integer, Integer>> getLeaderboard() {
        return teamPoints.entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // tri desc
                .toList();
    }

}
