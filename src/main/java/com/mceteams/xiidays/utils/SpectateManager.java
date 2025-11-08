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

import java.util.*;

import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;
import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID)
public class SpectateManager {

    // Joueurs en spectate
    private static final Set<UUID> spectatingPlayers = new HashSet<>();

    // Position de mort
    private static final Map<UUID, BlockPos> deathPositions = new HashMap<>();

    // Mode actuel : TEAMMATE (spec équipe) ou FREECAM (libre avec zone)
    private static final Map<UUID, SpectateMode> spectateModes = new HashMap<>();

    // Zones de free cam par équipe (min, max)
    private static final Map<String, FreeCamZone> freeCamZones = new HashMap<>();

    public enum SpectateMode {
        TEAMMATE,  // Spec un coéquipier (jour actif)
        FREECAM,   // Free cam limité (jour actif)
        CINEMATIC  // Cinématique (jour inactif)
    }

    public static class FreeCamZone {
        public BlockPos min, max;

        public FreeCamZone(BlockPos min, BlockPos max) {
            this.min = min;
            this.max = max;
        }

        public boolean isInside(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                    pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                    pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
    }

    /**
     * Définit la zone de free cam pour une équipe
     */
    public static void setFreeCamZone(String teamName, BlockPos corner1, BlockPos corner2) {
        BlockPos min = new BlockPos(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ())
        );

        freeCamZones.put(teamName, new FreeCamZone(min, max));
        DataManager.dataModify(teamName, "fcz_min", min.getX() + "," + min.getY() + "," + min.getZ());
        DataManager.dataModify(teamName, "fcz_max", max.getX() + "," + max.getY() + "," + max.getZ());

        LOGGER.info("Free cam zone set for team {}: {} to {}", teamName, min, max);
    }

    /**
     * Charge les zones de free cam depuis les données
     */
    public static void loadFreeCamZones() {
        for (String teamName : TeamManager.getAllTeams()) {
            String minData = DataManager.dataRead(teamName, "fcz_min");
            String maxData = DataManager.dataRead(teamName, "fcz_max");

            if (minData != null && maxData != null) {
                try {
                    String[] minCoords = minData.split(",");
                    String[] maxCoords = maxData.split(",");

                    BlockPos min = new BlockPos(
                            Integer.parseInt(minCoords[0]),
                            Integer.parseInt(minCoords[1]),
                            Integer.parseInt(minCoords[2])
                    );
                    BlockPos max = new BlockPos(
                            Integer.parseInt(maxCoords[0]),
                            Integer.parseInt(maxCoords[1]),
                            Integer.parseInt(maxCoords[2])
                    );

                    freeCamZones.put(teamName, new FreeCamZone(min, max));
                } catch (Exception e) {
                    LOGGER.error("Failed to load free cam zone for team {}", teamName);
                }
            }
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

        // Enregistrer position de mort
        deathPositions.put(player.getUUID(), player.blockPosition());
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

        LOGGER.info("Player {} died and will enter spectator mode", player.getName().getString());
    }

    /**
     * Event respawn
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!spectatingPlayers.contains(player.getUUID())) return;

        player.setGameMode(GameType.SPECTATOR);

        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        int currentDay = DaysManager.getCurrentDay();

        if (DaysManager.isDayInProgress()) {
            // JOUR ACTIF : Mode TEAMMATE par défaut
            spectateModes.put(player.getUUID(), SpectateMode.TEAMMATE);

            ServerPlayer teammate = NativeCameraController.findTeammateToSpectate(player);
            if (teammate != null) {
                NativeCameraController.spectatePlayer(player, teammate);
            }

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
                        NativeCameraController.cleanup(player);
                        player.sendSystemMessage(Component.literal("§aVous avez été respawn !"));
                    }
                });
            } else {
                // Phase combat : spec jusqu'au totem
                player.sendSystemMessage(Component.literal(
                        "§c§lVous êtes mort pendant la phase de combat !\n" +
                                "§7Utilisez §e[Shift]§7 pour changer de coéquipier\n" +
                                "§7Utilisez §e[F]§7 pour passer en free cam limité"
                ));
            }
        } else {
            // JOUR INACTIF : Mode CINEMATIC
            spectateModes.put(player.getUUID(), SpectateMode.CINEMATIC);

            // Démarrer une cinématique
            BlockPos[] cinematicPoints = getCinematicPoints(teamName);
            if (cinematicPoints != null && cinematicPoints.length > 0) {
                SimpleCinematic.startCinematic(player, cinematicPoints);
            }

            player.sendSystemMessage(Component.literal(
                    "§e§lMode Cinématique activé\n" +
                            "§7La journée est terminée, profitez de la vue !"
            ));
        }
    }

    /**
     * Récupère les points de cinématique d'une équipe
     */
    private static BlockPos[] getCinematicPoints(String teamName) {
        // Récupérer depuis la config ou définir des points par défaut
        String cinematicData = DataManager.dataRead(teamName, "cinematic_points");
        if (cinematicData == null) return null;

        String[] points = cinematicData.split(";");
        BlockPos[] result = new BlockPos[points.length];

        for (int i = 0; i < points.length; i++) {
            String[] coords = points[i].split(",");
            result[i] = new BlockPos(
                    Integer.parseInt(coords[0]),
                    Integer.parseInt(coords[1]),
                    Integer.parseInt(coords[2])
            );
        }

        return result;
    }

    /**
     * Change le mode de spectate (appelé par SpectateInput)
     */
    public static void toggleSpectateMode(ServerPlayer player) {
        if (!DaysManager.isDayInProgress()) return; // Pas de toggle hors jour actif

        SpectateMode current = spectateModes.getOrDefault(player.getUUID(), SpectateMode.TEAMMATE);

        if (current == SpectateMode.TEAMMATE) {
            // Passer en FREE CAM
            String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            FreeCamZone zone = freeCamZones.get(teamName);

            if (zone == null) {
                player.sendSystemMessage(Component.literal("§cAucune zone de free cam définie pour votre équipe !"));
                return;
            }

            spectateModes.put(player.getUUID(), SpectateMode.FREECAM);
            NativeCameraController.stopSpectating(player);

            player.sendSystemMessage(Component.literal(
                    "§a§lMode Free Cam activé\n" +
                            "§7Zone limitée : " + zone.min + " → " + zone.max
            ));
        } else {
            // Retour en TEAMMATE
            spectateModes.put(player.getUUID(), SpectateMode.TEAMMATE);

            ServerPlayer teammate = NativeCameraController.findTeammateToSpectate(player);
            if (teammate != null) {
                NativeCameraController.spectatePlayer(player, teammate);
            }

            player.sendSystemMessage(Component.literal("§a§lMode Coéquipier activé"));
        }
    }

    /**
     * Vérifie les limites de free cam
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;

        SpectateMode mode = spectateModes.get(player.getUUID());
        if (mode != SpectateMode.FREECAM) return;

        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        FreeCamZone zone = freeCamZones.get(teamName);

        if (zone == null) return;

        BlockPos pos = player.blockPosition();

        if (!zone.isInside(pos)) {
            // Téléporter au bord de la zone
            BlockPos clamped = new BlockPos(
                    Math.max(zone.min.getX(), Math.min(zone.max.getX(), pos.getX())),
                    Math.max(zone.min.getY(), Math.min(zone.max.getY(), pos.getY())),
                    Math.max(zone.min.getZ(), Math.min(zone.max.getZ(), pos.getZ()))
            );

            player.teleportTo(player.serverLevel(),
                    clamped.getX() + 0.5,
                    clamped.getY(),
                    clamped.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot());

            player.sendSystemMessage(Component.literal("§cVous avez atteint la limite de la zone !"), true);
        }
    }

    // ===== MÉTHODES EXISTANTES (inchangées) =====

    public static boolean respawnPlayer(ServerPlayer player) {
        if (!spectatingPlayers.contains(player.getUUID())) return false;

        spectatingPlayers.remove(player.getUUID());
        deathPositions.remove(player.getUUID());
        spectateModes.remove(player.getUUID());

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

    public static void respawnAllSpectators() {
        Set<UUID> toRespawn = new HashSet<>(spectatingPlayers);
        for (UUID uuid : toRespawn) {
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) respawnPlayer(player);
        }
        spectatingPlayers.clear();
        deathPositions.clear();
        spectateModes.clear();
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