package com.mceteams.xiidays.game;

import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.data.TeamStatsData;
import com.mceteams.xiidays.network.OpenScoreboardPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = "xiidays")
public class ScoreboardManager {
    private static final Map<Integer, TeamRankingEntry> lastRankings = new HashMap<>();
    private static final long ARROW_DISPLAY_DURATION = 20000;

    public static List<TeamRankingEntry> getRankings() {
        List<Map.Entry<Integer, Integer>> leaderboard = PointsManager.getLeaderboard();
        return leaderboard.stream()
                .map(entry -> {
                    int teamId = entry.getKey();
                    int points = entry.getValue();
                    TeamRankingEntry storedEntry = lastRankings.get(teamId);
                    if (storedEntry != null) return storedEntry;

                    int currentRank = leaderboard.indexOf(entry) + 1;
                    return new TeamRankingEntry(
                            teamId,
                            TeamData.getTeamName(teamId),
                            points,
                            currentRank,
                            currentRank,
                            0,
                            RankChange.NONE
                    );
                })
                .collect(Collectors.toList());
    }

    public enum RankChange {
        UP, DOWN, NONE
    }

    public record TeamRankingEntry(int teamId, String teamName, int points, int currentRank, int previousRank,
                                   long lastChangeTime, RankChange change) {}

    @SubscribeEvent
    public static void onPointsChanged(PointsChangedEvent event) {
        updateRankings();
    }

    private static void updateRankings() {
        List<Map.Entry<Integer, Integer>> newLeaderboard = PointsManager.getLeaderboard();

        for (int i = 0; i < newLeaderboard.size(); i++) {
            int teamId = newLeaderboard.get(i).getKey();
            int newRank = i + 1;

            TeamRankingEntry oldEntry = lastRankings.get(teamId);
            int oldRank = (oldEntry != null) ? oldEntry.currentRank : newRank;

            RankChange change = RankChange.NONE;
            if (newRank < oldRank) change = RankChange.UP;
            if (newRank > oldRank) change = RankChange.DOWN;

            TeamRankingEntry newEntry = new TeamRankingEntry(
                    teamId,
                    TeamData.getTeamName(teamId),
                    newLeaderboard.get(i).getValue(),
                    newRank,
                    oldRank,
                    change != RankChange.NONE ? System.currentTimeMillis() : 0,
                    change
            );
            lastRankings.put(teamId, newEntry);
        }
    }

    public static boolean shouldShowArrow(TeamRankingEntry entry) {
        if (entry.change == RankChange.NONE) return false;
        long elapsed = System.currentTimeMillis() - entry.lastChangeTime;
        return elapsed < ARROW_DISPLAY_DURATION;
    }

    public static ScoreboardData getScoreboardData(ServerPlayer player) {
        List<OpenScoreboardPacket.TeamData> teams = new ArrayList<>();

        for (String teamName : TeamData.getAllTeamNames()) {
            int teamId = TeamData.getTeamId(teamName);
            if (teamId == 0) continue;

            String uuid = TeamData.getTeamUUID(teamId);
            int points = TeamStatsData.getPoints(teamId);
            boolean coreAlive = !TeamData.isEliminated(teamId);

            TeamRankingEntry rankEntry = lastRankings.get(teamId);
            int rankChange = 0;
            if (rankEntry != null && shouldShowArrow(rankEntry)) {
                rankChange = switch (rankEntry.change) {
                    case UP -> 1;
                    case DOWN -> -1;
                    case NONE -> 0;
                };
            }

            teams.add(new OpenScoreboardPacket.TeamData(teamName, uuid, points, coreAlive, rankChange));
        }

        teams.sort(Comparator.comparingInt(OpenScoreboardPacket.TeamData::points).reversed());

        String playerTeam = TeamData.getPlayerTeam(player.getUUID().toString());
        if (playerTeam == null || playerTeam.isEmpty()) {
            playerTeam = "Aucune";
        }

        return new ScoreboardData(teams, playerTeam);
    }

    public record ScoreboardData(List<OpenScoreboardPacket.TeamData> teams, String playerTeam) {}
}
