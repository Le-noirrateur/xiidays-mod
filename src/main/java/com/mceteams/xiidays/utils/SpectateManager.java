package com.mceteams.xiidays.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;
import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID)
public class SpectateManager {

    // Joueurs actuellement en spectate
    private static final Set<UUID> spectatingPlayers = new HashSet<>();

    // Mapping joueur → dernière position de mort
    private static final Map<UUID, BlockPos> deathPositions = new HashMap<>();

    /**
     * Event déclenché lors de la mort d'un joueur
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Vérifier si un jour est en cours
        if (!DaysManager.isDayInProgress()) {
            return; // Respawn normal de Minecraft
        }

        // Vérifier si le joueur a une équipe
        String playerUUID = player.getUUID().toString();
        String teamName = TeamManager.getPlayerCurrentTeam(playerUUID);

        if (teamName == null) {
            LOGGER.warn("Player {} died without a team during active day", player.getName().getString());
            return; // Respawn normal
        }

        // Enregistrer la position de mort
        deathPositions.put(player.getUUID(), player.blockPosition());

        // Marquer le joueur comme spectateur
        spectatingPlayers.add(player.getUUID());

        // Attribution des points de mort
        int teamId = TeamManager.getTeamId(teamName);
        if (teamId > 0) {
            PointsManager.addPoints(teamId, com.mceteams.xiidays.enums.PointType.DEATH, player);
        }

        LOGGER.info("Player {} died and will be put in spectator mode", player.getName().getString());
    }

    /**
     * Event déclenché après le respawn du joueur
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Si le joueur doit être en spectate
        if (spectatingPlayers.contains(player.getUUID())) {
            // Passer en mode spectateur immédiatement
            player.setGameMode(GameType.SPECTATOR);

            // Téléporter à la position de mort si disponible
            BlockPos deathPos = deathPositions.get(player.getUUID());
            if (deathPos != null) {
                ServerLevel level = player.serverLevel();
                player.teleportTo(level, deathPos.getX() + 0.5, deathPos.getY() + 1, deathPos.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
            }

            LOGGER.info("Player {} put in spectator mode at death position", player.getName().getString());
        }
    }

    /**
     * Event tick pour gérer la visibilité des joueurs adverses
     * Appelé à chaque tick pour chaque joueur
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer spectator)) return;

        // Vérifier uniquement les joueurs en spectate
        if (!spectatingPlayers.contains(spectator.getUUID())) return;

        // Vérifier seulement toutes les 20 ticks (1 seconde) pour optimisation
        if (spectator.tickCount % 20 != 0) return;

        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return;

        // Parcourir tous les joueurs en ligne
        for (ServerPlayer otherPlayer : spectator.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (otherPlayer.getUUID().equals(spectator.getUUID())) continue;

            String otherTeam = TeamManager.getPlayerCurrentTeam(otherPlayer.getUUID().toString());

            // Gérer la visibilité selon l'équipe
            if (otherTeam != null && !otherTeam.equals(spectatorTeam)) {
                // Joueur adverse : rendre invisible pour le spectateur
                makeInvisibleFor(spectator, otherPlayer);
            } else {
                // Coéquipier : rendre visible
                makeVisibleFor(spectator, otherPlayer);
            }
        }
    }

    /**
     * Rend un joueur invisible pour un spectateur spécifique
     */
    private static void makeInvisibleFor(ServerPlayer spectator, ServerPlayer target) {
        // Utilise le système de pseudo-invisibilité en supprimant l'entité côté client
        // Note : Cette méthode nécessite un packet custom pour être complètement efficace
        // Pour l'instant, on utilise une approche simple

        // Si le spectateur est trop proche du joueur adverse, le téléporter légèrement
        if (spectator.distanceToSqr(target) < 25.0) { // < 5 blocs
            BlockPos targetPos = target.blockPosition();

            // Chercher un coéquipier proche pour téléporter le spectateur
            ServerPlayer nearestTeammate = findNearestTeammate(spectator, targetPos);
            if (nearestTeammate != null && !nearestTeammate.getUUID().equals(target.getUUID())) {
                BlockPos teammatePos = nearestTeammate.blockPosition();
                spectator.teleportTo(spectator.serverLevel(),
                        teammatePos.getX() + 0.5,
                        teammatePos.getY(),
                        teammatePos.getZ() + 0.5,
                        spectator.getYRot(),
                        spectator.getXRot());
            }
        }
    }


    //Rend un joueur visible pour un spectateur spécifique
    private static void makeVisibleFor(ServerPlayer spectator, ServerPlayer target) {
        // Rien à faire de spécial, la visibilité normale s'applique
    }

    // Trouve le coéquipier le plus proche d'une position donnée
    private static ServerPlayer findNearestTeammate(ServerPlayer spectator, BlockPos pos) {
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return null;

        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayer player : spectator.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(spectator.getUUID())) continue;
            if (spectatingPlayers.contains(player.getUUID())) continue; // Ignorer les autres spectateurs

            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            if (playerTeam != null && playerTeam.equals(spectatorTeam)) {
                double distance = player.blockPosition().distSqr(pos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = player;
                }
            }
        }

        return nearest;
    }


    // Respawn un joueur (commande admin ou fin de jour)
    public static boolean respawnPlayer(ServerPlayer player) {
        if (!spectatingPlayers.contains(player.getUUID())) {
            return false; // Le joueur n'est pas en spectate
        }

        // Retirer du mode spectateur
        spectatingPlayers.remove(player.getUUID());
        deathPositions.remove(player.getUUID());

        // Récupérer l'équipe et le spawn
        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        if (teamName == null) {
            // Pas d'équipe, spawn au monde
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            return true;
        }

        // Récupérer la position du spawn
        BlockPos spawnPos = getTeamSpawnPosition(teamName);
        if (spawnPos != null) {
            ServerLevel level = player.serverLevel();
            player.teleportTo(level,
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot());
        }

        // Restaurer le mode de jeu
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        LOGGER.info("Player {} respawned from spectator mode", player.getName().getString());
        return true;
    }

    /**
     * Respawn tous les joueurs en spectate (fin de jour)
     */
    public static void respawnAllSpectators() {
        Set<UUID> toRespawn = new HashSet<>(spectatingPlayers);

        for (UUID uuid : toRespawn) {
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) {
                respawnPlayer(player);
            }
        }

        spectatingPlayers.clear();
        deathPositions.clear();

        LOGGER.info("All spectators have been respawned");
    }

    /**
     * Vérifie si un joueur est en mode spectate
     */
    public static boolean isSpectating(ServerPlayer player) {
        return spectatingPlayers.contains(player.getUUID());
    }

    /**
     * Récupère le nombre de spectateurs
     */
    public static int getSpectatorCount() {
        return spectatingPlayers.size();
    }

    /**
     * Récupère la liste des spectateurs par équipe
     */
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

    /**
     * Nettoie les spectateurs déconnectés
     */
    public static void cleanupDisconnectedSpectators() {
        spectatingPlayers.removeIf(uuid -> {
            ServerPlayer player = getPlayerByUUID(uuid);
            return player == null || player.hasDisconnected();
        });

        deathPositions.keySet().removeIf(uuid -> {
            ServerPlayer player = getPlayerByUUID(uuid);
            return player == null || player.hasDisconnected();
        });
    }

    /**
     * Récupère un joueur par son UUID
     */
    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        assert net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null;
        for (ServerPlayer player : net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Récupère la position du spawn d'une équipe
     */
    private static BlockPos getTeamSpawnPosition(String teamName) {
        DataManager.reloadData();

        String spawnData = DataManager.dataRead(teamName, "spawn");
        if (spawnData == null) {
            return null;
        }

        try {
            String[] coords = spawnData.split(",");
            if (coords.length != 3) {
                LOGGER.error("Invalid spawn data format for team {}: {}", teamName, spawnData);
                return null;
            }

            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());
            int z = Integer.parseInt(coords[2].trim());

            return new BlockPos(x, y, z);

        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse spawn coordinates for team {}: {}", teamName, e.getMessage());
            return null;
        }
    }
}