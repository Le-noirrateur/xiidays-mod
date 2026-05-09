package com.mceteams.xiidays.game;

import com.google.gson.JsonArray;
import com.mceteams.xiidays.data.PlayerStatsData;
import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.data.TeamStatsData;
import com.mceteams.xiidays.network.OpenEndScoreboardPacket;
import com.mceteams.xiidays.world.BlockRegistry;
import com.mceteams.xiidays.world.CoreBlockEntity;
import com.mceteams.xiidays.world.SpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.mceteams.xiidays.XIIDays.LOGGER;

public class TeamManager {

    public static int addMember(String teamName, Player player) {
        if (!TeamData.teamExists(teamName)) return 2;

        int teamId = TeamData.getTeamId(teamName);
        if (teamId == 0) return 3;

        String playerUUID = player.getUUID().toString();

        if (TeamData.isPlayerInTeam(playerUUID, teamId)) return 4;

        String currentTeam = TeamData.getPlayerTeam(playerUUID);
        if (currentTeam != null && !currentTeam.equals(teamName)) return 5;

        TeamData.addMember(teamId, playerUUID);
        return 1;
    }

    public static int remMember(String teamName, Player player) {
        if (!TeamData.teamExists(teamName)) return 2;

        int teamId = TeamData.getTeamId(teamName);
        if (teamId == 0) return 3;

        String playerUUID = player.getUUID().toString();

        if (!TeamData.isPlayerInTeam(playerUUID, teamId)) return 4;

        String currentTeam = TeamData.getPlayerTeam(playerUUID);
        if (currentTeam != null && !currentTeam.equals(teamName)) return 5;

        if (TeamData.removeMember(teamId, playerUUID)) return 1;

        return 6;
    }

    public static int remTeam(String teamName) {
        int teamId = TeamData.getTeamId(teamName);
        if (teamId == 0) return 2;

        TeamData.deleteEntry(teamId);
        return 1;
    }

    public static int placeTeamSpawn(String teamName, BlockPos pos, Level level) {
        BlockPos newPos = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());

        BlockState blockState = BlockRegistry.TEAM_SPAWNER.get().defaultBlockState();
        boolean blockPlaced = level.setBlock(newPos, blockState, 3);

        if (blockPlaced) {
            BlockEntity blockEntity = level.getBlockEntity(newPos);
            if (blockEntity instanceof SpawnerBlockEntity teamSpawn) {
                int teamId = TeamManager.getTeamId(teamName);
                teamSpawn.setTeamId(teamId);
                level.getChunkAt(newPos).setUnsaved(true);

                TeamData.setSpawn(teamId, pos.getX() + "," + (pos.getY() + 1) + "," + pos.getZ());
                return 1;
            } else {
                return 2;
            }
        } else {
            return 3;
        }
    }

    public static String getTeamName(int teamId) {
        return TeamData.getTeamName(teamId);
    }

    public static int placeTeamCore(String teamName, BlockPos pos, Level level) {
        BlockState blockState = BlockRegistry.TEAM_CORE.get().defaultBlockState();
        boolean blockPlaced = level.setBlock(pos, blockState, 3);

        if (blockPlaced) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CoreBlockEntity teamCore) {
                int teamId = TeamManager.getTeamId(teamName);
                teamCore.setTeamId(teamId);
                teamCore.setPuzzleSolved(false);
                level.getChunkAt(pos).setUnsaved(true);

                TeamData.setCore(teamId, pos.getX() + "," + pos.getY() + "," + pos.getZ());
                return 1;
            } else {
                return 2;
            }
        } else {
            return 3;
        }
    }

    public static boolean isPlayerInTeam(String playerUUID, int teamID) {
        return TeamData.isPlayerInTeam(playerUUID, teamID);
    }

    public static String[] getAllTeams() {
        return TeamData.getAllTeamNames();
    }

    public static void sendMessageToTeam(String teamName, Component message) {
        int teamId = TeamData.getTeamId(teamName);
        if (teamId == 0) return;

        sendMessageToTeam(teamId, message, null);
    }

    public static void sendMessageToTeam(int teamId, Component message, UUID exclude) {
        if (teamId == 0) return;

        var members = TeamData.getMembers(teamId);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (int i = 0; i < members.size(); i++) {
            String uuidStr = members.get(i).getAsString();
            if (uuidStr == null) continue;

            try {
                UUID uuid = UUID.fromString(uuidStr);
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);

                if (player == null) continue;
                if (exclude != null && uuid.equals(exclude)) continue;

                player.sendSystemMessage(message);
            } catch (IllegalArgumentException e) {
                // UUID invalide
            }
        }
    }

    public static boolean eliminateTeam(String teamName) {
        int teamId = TeamData.getTeamId(teamName);
        if (teamId == 0) return false;

        if (TeamData.isEliminated(teamId)) {
            LOGGER.warn("Team {} already eliminated!", teamName);
            return false;
        }

        TeamData.setEliminated(teamId, true);

        int totalTeams = TeamData.getAllTeamNames().length;
        int remainingTeams = countRemainingTeams();
        int finalPosition = totalTeams - remainingTeams;

        TeamData.setFinalPosition(teamId, finalPosition);

        LOGGER.info("Team {} eliminated - Position: {} ({} teams remaining)",
                teamName, finalPosition, remainingTeams);

        if (remainingTeams == 1) {
            String winnerTeam = getLastRemainingTeam();
            if (winnerTeam != null) {
                setWinningTeam(winnerTeam);

                // Stop the ongoing day first
                DaysManager.stop();

                // Compute and send end-screen data to all players
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    OpenEndScoreboardPacket packet = buildEndPacket(winnerTeam);
                    PacketDistributor.sendToAllPlayers(packet);
                }
            }
        }

        return true;
    }

    private static OpenEndScoreboardPacket buildEndPacket(String winnerTeam) {
        List<OpenEndScoreboardPacket.TeamEntry> teamStats = new ArrayList<>();
        for (String teamName : TeamData.getAllTeamNames()) {
            int tid = TeamData.getTeamId(teamName);
            if (tid == 0) continue;
            int deaths = 0;
            JsonArray members = TeamData.getMembers(tid);
            for (int i = 0; i < members.size(); i++) {
                String uuid = members.get(i).getAsString();
                deaths += PlayerStatsData.getDeaths(uuid);
            }
            teamStats.add(new OpenEndScoreboardPacket.TeamEntry(
                    teamName,
                    TeamStatsData.getPoints(tid),
                    TeamStatsData.getKills(tid),
                    deaths,
                    TeamStatsData.getBlocksMined(tid),
                    TeamStatsData.getDamageDealt(tid),
                    TeamData.getFinalPosition(tid)
            ));
        }
        teamStats.sort(Comparator.comparingInt(OpenEndScoreboardPacket.TeamEntry::finalPosition));

        // Compute MVP: player with highest weighted score
        String bestUUID = null;
        String bestName = null;
        String bestTeam = null;
        int bestPoints = -1;
        int bestKills = 0;
        int bestDeaths = 0;
        int bestScore = -1;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        for (String teamName : TeamData.getAllTeamNames()) {
            int tid = TeamData.getTeamId(teamName);
            if (tid == 0) continue;
            JsonArray members = TeamData.getMembers(tid);
            for (int i = 0; i < members.size(); i++) {
                String uuid = members.get(i).getAsString();
                int pPoints = PlayerStatsData.getTeamPoints(uuid);
                int pKills = PlayerStatsData.getKills(uuid);
                int pDeaths = PlayerStatsData.getDeaths(uuid);
                int pScore = pPoints + pKills * 50 - pDeaths * 10;
                if (pScore > bestScore) {
                    bestScore = pScore;
                    bestPoints = pPoints;
                    bestKills = pKills;
                    bestDeaths = pDeaths;
                    bestUUID = uuid;
                    bestTeam = teamName;
                    if (server != null) {
                        ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(uuid));
                        bestName = p != null ? p.getName().getString() : "Inconnu";
                    } else {
                        bestName = "Inconnu";
                    }
                }
            }
        }

        OpenEndScoreboardPacket.MvpData mvp = new OpenEndScoreboardPacket.MvpData(
                bestName != null ? bestName : "Personne",
                bestUUID != null ? bestUUID : "",
                bestTeam != null ? bestTeam : "",
                Math.max(bestPoints, 0),
                Math.max(bestKills, 0),
                Math.max(bestDeaths, 0),
                Math.max(bestScore, 0)
        );

        return new OpenEndScoreboardPacket(winnerTeam, mvp, teamStats);
    }

    public static boolean reviveTeam(String teamName) {
        int teamId = TeamData.getTeamId(teamName);
        if (teamId == 0) return false;
        if (!TeamData.isEliminated(teamId)) return false;

        TeamData.setEliminated(teamId, false);
        TeamData.setFinalPosition(teamId, 0);

        // Reset final position for all teams — the winner state is no longer valid
        for (String t : TeamData.getAllTeamNames()) {
            int tid = TeamData.getTeamId(t);
            if (tid > 0 && TeamData.getFinalPosition(tid) == 1) {
                TeamData.setFinalPosition(tid, 0);
            }
        }

        LOGGER.info("Team {} has been revived! Game reset.", teamName);
        return true;
    }

    public static boolean isGameOver() {
        return countRemainingTeams() <= 1 || getWinningTeam() != null;
    }

    private static int countRemainingTeams() {
        String[] allTeams = TeamData.getAllTeamNames();
        int count = 0;

        for (String teamName : allTeams) {
            int teamId = TeamData.getTeamId(teamName);
            if (teamId == 0) continue;

            if (!TeamData.isEliminated(teamId)) count++;
        }

        return count;
    }

    private static String getLastRemainingTeam() {
        String[] allTeams = TeamData.getAllTeamNames();

        for (String teamName : allTeams) {
            int teamId = TeamData.getTeamId(teamName);
            if (teamId == 0) continue;

            if (!TeamData.isEliminated(teamId)) return teamName;
        }

        return null;
    }

    private static void setWinningTeam(String teamName) {
        int teamId = TeamData.getTeamId(teamName);
        if (teamId == 0) return;

        TeamData.setFinalPosition(teamId, 1);
        LOGGER.info("🏆 Team {} wins the game!", teamName);
    }

    private static void triggerEndScreen(String winningTeam) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("§6§l=== FIN DE PARTIE ==="));
            player.sendSystemMessage(Component.literal("§eÉquipe gagnante : §6§l" + winningTeam));
        }
    }

    public static int getFinalPosition(String teamName) {
        int teamId = TeamData.getTeamId(teamName);
        if (teamId == 0) return 999;
        return TeamData.getFinalPosition(teamId);
    }

    public static String getWinningTeam() {
        String[] allTeams = TeamData.getAllTeamNames();

        for (String teamName : allTeams) {
            if (getFinalPosition(teamName) == 1) return teamName;
        }

        return null;
    }

    public static String getPlayerCurrentTeam(String playerUUID) {
        return TeamData.getPlayerTeam(playerUUID);
    }

    public static int getTeamId(String teamName) {
        return TeamData.getTeamId(teamName);
    }

    public static int createTeam(String teamName) {
        if (TeamData.teamExists(teamName)) return 2;

        TeamData.createEntry(teamName);
        PointsManager.refreshTeamPoints();

        return 1;
    }
}
