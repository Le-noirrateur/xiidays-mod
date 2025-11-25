package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.enums.PointType;
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

    // Joueurs en spectate
    private static final Set<UUID> spectatingPlayers = new HashSet<>();

    // Map spectateur → cible actuelle
    private static final Map<UUID, UUID> spectatorTargets = new HashMap<>();

    // Zones de free cam par équipe
    private static final Map<String, FreeCamZone> freeCamZones = new HashMap<>();

    // Joueurs en mode free cam (autorisé uniquement hors jour actif)
    private static final Set<UUID> freeCamPlayers = new HashSet<>();

    /**
     * Classe représentant une zone de free cam
     */
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

    /**
     * Event mort du joueur
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Vérifier si un jour est actif
        if (!DaysManager.isDayInProgress()) {
            return; // Respawn normal
        }

        String playerUUID = player.getUUID().toString();
        String teamName = TeamManager.getPlayerCurrentTeam(playerUUID);

        if (teamName == null) {
            LOGGER.warn("Player {} died without a team", player.getName().getString());
            return;
        }

        // Marquer comme spectateur
        spectatingPlayers.add(player.getUUID());

        // Attribution des points
        int teamId = TeamManager.getTeamId(teamName);
        if (teamId > 0) {
            DataManager.dataModify("player_" + player.getUUID() + "_stats", "deaths",
                    DataManager.dataReadInt("player_" + player.getUUID() + "_stats", "deaths", 0) + 1);
            DataManager.dataModify("team_" + teamId + "_stats", "kill_streak", 0);
            PointsManager.addPoints(teamId, PointType.DEATH, player);
        }

        // Gestion de l'attaquant
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            String attackerTeamName = TeamManager.getPlayerCurrentTeam(attacker.getUUID().toString());
            int attackerTeamId = TeamManager.getTeamId(attackerTeamName);

            if (attackerTeamId > 0) {
                DataManager.dataModify("team_" + attackerTeamId + "_stats", "kills",
                        DataManager.dataReadInt("team_" + attackerTeamId + "_stats", "kills", 0) + 1);
                DataManager.dataModify("team_" + attackerTeamId + "_stats", "kill_streak",
                        DataManager.dataReadInt("team_" + attackerTeamId + "_stats", "kill_streak", 0) + 1);
                DataManager.dataModify("team_" + attackerTeamId + "_stats", "max_kill_streak",
                        Math.max(
                                DataManager.dataReadInt("team_" + attackerTeamId + "_stats", "max_kill_streak", 0),
                                DataManager.dataReadInt("team_" + attackerTeamId + "_stats", "kill_streak", 0)
                        ));

                PointsManager.addPoints(attackerTeamId, PointType.KILL, attacker);
                PointsManager.addPoints(attackerTeamId, PointType.KILL_STREAK, attacker);
            }
        }

        LOGGER.info("Player {} died, will spectate teammates only", player.getName().getString());
    }

    /**
     * Event respawn
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!spectatingPlayers.contains(player.getUUID())) return;

        // Passer en spectateur
        player.setGameMode(GameType.SPECTATOR);

        // Trouver un coéquipier à spectater
        ServerPlayer teammate = findTeammateToSpectate(player);
        if (teammate != null) {
            spectatePlayer(player, teammate);
        }

        int currentDay = DaysManager.getCurrentDay();

        if (currentDay <= 6) {
            // Phase préparation : respawn avec délai
            int deaths = DataManager.dataReadInt("player_" + player.getUUID() + "_stats", "deaths", 0);
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
            // Phase combat : spec jusqu'au totem
            String message = "§c§lVous êtes mort pendant la phase de combat !\n" +
                    "§7Votre équipe doit utiliser un §6Totem de Revivalité§7 pour vous faire respawn.\n" +
                    "§7Utilisez §e[Shift]§7 pour changer de coéquipier.";
            player.sendSystemMessage(Component.literal(message));
        }
    }

    /**
     * Vérifie automatiquement si la cible est toujours valide
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer spectator)) return;
        if (spectator.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;
        if (!spectatingPlayers.contains(spectator.getUUID())) return;

        // Vérifier toutes les secondes
        if (spectator.tickCount % 20 != 0) return;

        UUID targetUUID = spectatorTargets.get(spectator.getUUID());
        if (targetUUID == null) return;

        ServerPlayer target = getPlayerByUUID(targetUUID);

        // Vérifier validité + équipe
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        String targetTeam = target != null ? TeamManager.getPlayerCurrentTeam(target.getUUID().toString()) : null;

        if (target == null ||
                target.hasDisconnected() ||
                target.gameMode.getGameModeForPlayer() != GameType.SPECTATOR ||
                !spectatorTeam.equals(targetTeam)) {

            // Cible invalide, trouver un autre coéquipier
            ServerPlayer newTarget = findTeammateToSpectate(spectator);
            if (newTarget != null) {
                spectatePlayer(spectator, newTarget);
            }
        }
    }

    // ===== MÉTHODES PUBLIQUES =====

    /**
     * Fait spectater un joueur (UNIQUEMENT coéquipiers)
     */
    public static void spectatePlayer(ServerPlayer spectator, ServerPlayer target) {
        if (target == null) {
            spectator.setCamera(spectator);
            spectatorTargets.remove(spectator.getUUID());
            return;
        }

        // VÉRIFICATION ÉQUIPE
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        String targetTeam = TeamManager.getPlayerCurrentTeam(target.getUUID().toString());

        if (spectatorTeam == null || !spectatorTeam.equals(targetTeam)) {
            LOGGER.warn("Spectator {} tried to watch enemy player {}",
                    spectator.getName().getString(),
                    target.getName().getString());
            spectator.sendSystemMessage(Component.literal("§cVous ne pouvez pas spectater un joueur ennemi !"));
            return;
        }

        // Spectater
        spectator.setCamera(target);
        spectatorTargets.put(spectator.getUUID(), target.getUUID());

        spectator.sendSystemMessage(Component.literal("§eSpectate : §a" + target.getName().getString()), true);
    }

    /**
     * Change de coéquipier (appelé par les flèches ou Shift)
     */
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

    /**
     * Trouve un coéquipier à spectater
     */
    private static ServerPlayer findTeammateToSpectate(ServerPlayer spectator) {
        List<ServerPlayer> teammates = getTeammates(spectator);
        return teammates.isEmpty() ? null : teammates.get(0);
    }

    /**
     * Récupère la liste des coéquipiers vivants
     */
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

    /**
     * Respawn un joueur
     */
    public static boolean respawnPlayer(ServerPlayer player) {
        if (!spectatingPlayers.contains(player.getUUID())) return false;

        spectatingPlayers.remove(player.getUUID());
        spectatorTargets.remove(player.getUUID());

        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        if (teamName != null) {
            BlockPos spawnPos = getTeamSpawnPosition(teamName);
            if (spawnPos != null) {
                player.teleportTo(player.serverLevel(),
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            }
        }

        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        return true;
    }

    /**
     * Respawn tous les spectateurs
     */
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

    private static BlockPos getTeamSpawnPosition(String teamName) {
        String spawnData = DataManager.dataRead(teamName, "spawn");
        if (spawnData == null) return null;

        try {
            String[] coords = spawnData.split(",");
            return new BlockPos(
                    Integer.parseInt(coords[0].trim()),
                    Integer.parseInt(coords[1].trim()),
                    Integer.parseInt(coords[2].trim())
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}