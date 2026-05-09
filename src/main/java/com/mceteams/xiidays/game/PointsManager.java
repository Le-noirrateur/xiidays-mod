package com.mceteams.xiidays.game;

import com.mceteams.xiidays.data.PlayerStatsData;
import com.mceteams.xiidays.data.TeamStatsData;
import com.mceteams.xiidays.network.PointsPopupPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mceteams.xiidays.XIIDays.LOGGER;

public class PointsManager {
    private static final Map<Integer, Integer> teamPoints = new HashMap<>();

    public static void addPoints(int teamId, PointType type, Player player, Object... args) {
        int oldPoints = TeamStatsData.getPoints(teamId);
        int newPoints;
        int playerPoints = PlayerStatsData.getTeamPoints(player.getUUID().toString());
        int pointsAdded = 0;

        switch (type) {
            case KILL -> pointsAdded = 50;
            case DEATH -> pointsAdded = -25;
            case MINING -> {
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
            case FIRST_BLOOD -> pointsAdded = 2000;
            case KILL_STREAK -> {
                int streak = TeamStatsData.getKillStreak(teamId);
                if (streak >= 3 && streak < 5) {
                    pointsAdded = 25;
                } else if (streak >= 5 && streak < 10) {
                    pointsAdded = 50;
                } else if (streak >= 10) {
                    pointsAdded = 100;
                }
            }
            case CRATE -> pointsAdded = 75;
            case TOTEM -> pointsAdded = 200;
            case CORE_MAZE -> pointsAdded = 300;
            default -> LOGGER.error("PointType non géré: {}", type);
        }

        newPoints = oldPoints + pointsAdded;
        playerPoints += pointsAdded;

        if (pointsAdded != 0) {
            NeoForge.EVENT_BUS.post(new PointsChangedEvent(teamId, oldPoints, newPoints));
        }

        teamPoints.put(teamId, newPoints);

        TeamStatsData.setPoints(teamId, newPoints);
        PlayerStatsData.setTeamPoints(player.getUUID().toString(), playerPoints);

        if ((pointsAdded >= 25 || type == PointType.DEATH) && type != PointType.KILL_STREAK) {
            TeamStatsData.pushPointHistory(teamId, player.getName().getString() + ":" + type + ":" + pointsAdded);
        }

        if (pointsAdded < 0) {
            TeamStatsData.addPointLoss(teamId, -pointsAdded);
        } else {
            TeamStatsData.addPointGain(teamId, pointsAdded);
        }

        if (player instanceof ServerPlayer srvp) {
            PointsPopupPayload popup = new PointsPopupPayload(pointsAdded, type.name());
            PacketDistributor.sendToPlayer(srvp, popup);
            TeamManager.sendMessageToTeam(teamId, Component.literal(player.getName().getString() + " à ajouté §4§l" + (pointsAdded >= 0 ? "+" : "") + pointsAdded + "§r à votre équipe (§6§l" + type + "§r)"), player.getUUID());
        }
        NeoForge.EVENT_BUS.post(new PointsChangedEvent(teamId, oldPoints, newPoints));
    }

    public static void initializeTeamPoints() {
        String[] allTeams = TeamManager.getAllTeams();
        for (String teamName : allTeams) {
            int teamId = TeamManager.getTeamId(teamName);
            if (teamId > 0) {
                int points = TeamStatsData.getPoints(teamId);
                teamPoints.put(teamId, points);
                LOGGER.info("Loaded team {} (ID: {}) with {} points", teamName, teamId, points);
            }
        }
        LOGGER.info("Initialized {} teams in leaderboard", teamPoints.size());
    }

    public static void refreshTeamPoints() {
        teamPoints.clear();
        initializeTeamPoints();
    }

    public static List<Map.Entry<Integer, Integer>> getLeaderboard() {
        return teamPoints.entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();
    }
}
