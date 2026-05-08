package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.enums.PointType;
import com.mceteams.xiidays.utils.data.PlayerStatsData;
import com.mceteams.xiidays.utils.data.TeamData;
import com.mceteams.xiidays.utils.data.TeamStatsData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

import static com.mceteams.xiidays.XIIDays.LOGGER;
import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID)
public class SpectateManager {
    private static final Set<UUID> spectatingPlayers = new HashSet<>();
    private static final Map<UUID, UUID> spectatorTargets = new HashMap<>();
    private static final Map<String, FreeCamZone> freeCamZones = new HashMap<>();
    private static final Set<UUID> freeCamPlayers = new HashSet<>();

    public static class FreeCamZone {
        public final BlockPos min;
        public final BlockPos max;

        public FreeCamZone(BlockPos min, BlockPos max) {
            this.min = min;
            this.max = max;
        }

        public boolean isInside(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                    pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                    pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }

        public BlockPos clamp(BlockPos pos) {
            return new BlockPos(
                    Math.max(min.getX(), Math.min(max.getX(), pos.getX())),
                    Math.max(min.getY(), Math.min(max.getY(), pos.getY())),
                    Math.max(min.getZ(), Math.min(max.getZ(), pos.getZ()))
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!DaysManager.isDayInProgress()) return;

        String playerUUID = player.getUUID().toString();
        String teamName = TeamManager.getPlayerCurrentTeam(playerUUID);

        if (teamName == null) {
            LOGGER.warn("Player {} died without a team", player.getName().getString());
            return;
        }

        spectatingPlayers.add(player.getUUID());

        int teamId = TeamManager.getTeamId(teamName);
        if (teamId > 0) {
            PlayerStatsData.incrementDeaths(player.getUUID().toString());
            TeamStatsData.setKillStreak(teamId, 0);
            PointsManager.addPoints(teamId, PointType.DEATH, player);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            String attackerTeamName = TeamManager.getPlayerCurrentTeam(attacker.getUUID().toString());
            int attackerTeamId = TeamManager.getTeamId(attackerTeamName);

            if (attackerTeamId > 0) {
                TeamStatsData.incrementKills(attackerTeamId);
                int newStreak = TeamStatsData.getKillStreak(attackerTeamId) + 1;
                TeamStatsData.setKillStreak(attackerTeamId, newStreak);
                TeamStatsData.updateMaxKillStreak(attackerTeamId);

                PointsManager.addPoints(attackerTeamId, PointType.KILL, attacker);
                PointsManager.addPoints(attackerTeamId, PointType.KILL_STREAK, attacker);
            }
        }

        LOGGER.info("Player {} died, will spectate teammates only", player.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!spectatingPlayers.contains(player.getUUID())) return;

        player.setGameMode(GameType.SPECTATOR);

        ServerPlayer teammate = findTeammateToSpectate(player);
        if (teammate != null) {
            spectatePlayer(player, teammate);
        }

        int currentDay = DaysManager.getCurrentDay();

        if (currentDay <= 6) {
            int deaths = PlayerStatsData.getDeaths(player.getUUID().toString());
            int delaySeconds = Math.min(deaths * 3, 10);

            for (int i = 0; i < delaySeconds; i++) {
                final int secondsLeft = delaySeconds - i;
                TaskScheduler.schedule(i * 20, () -> {
                    if (player.hasDisconnected()) return;
                    player.sendSystemMessage(Component.literal(
                            "§eRespawn dans §c" + secondsLeft + "§e seconde" + (secondsLeft > 1 ? "s" : "")
                    ), true);
                });
            }

            TaskScheduler.schedule(delaySeconds * 20, () -> {
                if (player.hasDisconnected()) return;
                if (spectatingPlayers.contains(player.getUUID())) {
                    respawnPlayer(player);
                    player.sendSystemMessage(Component.literal("§aVous avez été respawn !"));
                }
            });
        } else {
            String message = "§c§lVous êtes mort pendant la phase de combat !\n" +
                    "§7Votre équipe doit utiliser un §6Totem de Revivalité§7 pour vous faire respawn.\n" +
                    "§7Utilisez §e[Shift]§7 pour changer de coéquipier.";
            player.sendSystemMessage(Component.literal(message));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer spectator)) return;
        if (spectator.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;
        if (!spectatingPlayers.contains(spectator.getUUID())) return;

        if (spectator.tickCount % 20 != 0) return;

        UUID targetUUID = spectatorTargets.get(spectator.getUUID());
        if (targetUUID == null) return;

        ServerPlayer target = getPlayerByUUID(targetUUID);
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        String targetTeam = target != null ? TeamManager.getPlayerCurrentTeam(target.getUUID().toString()) : null;

        if (target == null ||
                target.hasDisconnected() ||
                target.gameMode.getGameModeForPlayer() != GameType.SPECTATOR ||
                !spectatorTeam.equals(targetTeam)) {

            ServerPlayer newTarget = findTeammateToSpectate(spectator);
            if (newTarget != null) {
                spectatePlayer(spectator, newTarget);
            }
        }
    }

    public static void spectatePlayer(ServerPlayer spectator, ServerPlayer target) {
        if (target == null) {
            spectator.setCamera(spectator);
            spectatorTargets.remove(spectator.getUUID());
            return;
        }

        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        String targetTeam = TeamManager.getPlayerCurrentTeam(target.getUUID().toString());

        if (spectatorTeam == null || !spectatorTeam.equals(targetTeam)) {
            LOGGER.warn("Spectator {} tried to watch enemy player {}",
                    spectator.getName().getString(),
                    target.getName().getString());
            spectator.sendSystemMessage(Component.literal("§cVous ne pouvez pas spectater un joueur ennemi !"));
            return;
        }

        spectator.setCamera(target);
        spectatorTargets.put(spectator.getUUID(), target.getUUID());
        spectator.sendSystemMessage(Component.literal("§eSpectate : §a" + target.getName().getString()), true);
    }

    public static void switchTeammate(ServerPlayer spectator, boolean next) {
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return;

        List<ServerPlayer> teammates = getTeammates(spectator);
        if (teammates.isEmpty()) return;

        UUID currentTarget = spectatorTargets.get(spectator.getUUID());
        int currentIndex = -1;

        for (int i = 0; i < teammates.size(); i++) {
            if (teammates.get(i).getUUID().equals(currentTarget)) {
                currentIndex = i;
                break;
            }
        }

        int newIndex;
        if (next) {
            newIndex = (currentIndex + 1) % teammates.size();
        } else {
            newIndex = (currentIndex - 1 + teammates.size()) % teammates.size();
        }

        spectatePlayer(spectator, teammates.get(newIndex));
    }

    private static ServerPlayer findTeammateToSpectate(ServerPlayer spectator) {
        List<ServerPlayer> teammates = getTeammates(spectator);
        return teammates.isEmpty() ? null : teammates.get(0);
    }

    private static List<ServerPlayer> getTeammates(ServerPlayer spectator) {
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return Collections.emptyList();

        List<ServerPlayer> teammates = new ArrayList<>();
        for (ServerPlayer player : spectator.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(spectator.getUUID())) continue;
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) continue;

            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            if (spectatorTeam.equals(playerTeam)) {
                teammates.add(player);
            }
        }
        return teammates;
    }

    public static boolean respawnPlayer(ServerPlayer player) {
        if (!spectatingPlayers.contains(player.getUUID())) return false;

        spectatingPlayers.remove(player.getUUID());
        spectatorTargets.remove(player.getUUID());

        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        if (teamName != null) {
            int teamId = TeamManager.getTeamId(teamName);
            String spawnData = TeamData.getSpawn(teamId);
            if (spawnData != null) {
                try {
                    String[] coords = spawnData.split(",");
                    BlockPos spawnPos = new BlockPos(
                            Integer.parseInt(coords[0].trim()),
                            Integer.parseInt(coords[1].trim()),
                            Integer.parseInt(coords[2].trim())
                    );
                    player.teleportTo(player.serverLevel(),
                            spawnPos.getX() + 0.5,
                            spawnPos.getY(),
                            spawnPos.getZ() + 0.5,
                            player.getYRot(),
                            player.getXRot());
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid spawn data for team {}: {}", teamName, spawnData);
                }
            }
        }

        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        return true;
    }

    public static void respawnAllSpectators() {
        Set<UUID> toRespawn = new HashSet<>(spectatingPlayers);
        for (UUID uuid : toRespawn) {
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) respawnPlayer(player);
        }
        spectatingPlayers.clear();
        spectatorTargets.clear();
    }

    public static boolean isSpectating(ServerPlayer player) {
        return spectatingPlayers.contains(player.getUUID());
    }

    public static int getSpectatorCount() {
        return spectatingPlayers.size();
    }

    public static Map<String, Integer> getSpectatorsByTeam() {
        Map<String, Integer> counts = new HashMap<>();
        for (UUID uuid : spectatingPlayers) {
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) {
                String team = TeamManager.getPlayerCurrentTeam(uuid.toString());
                if (team != null) {
                    counts.put(team, counts.getOrDefault(team, 0) + 1);
                }
            }
        }
        return counts;
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        assert net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null;
        for (ServerPlayer player : net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(uuid)) return player;
        }
        return null;
    }
}
