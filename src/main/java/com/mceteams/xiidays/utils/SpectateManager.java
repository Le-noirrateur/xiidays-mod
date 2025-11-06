package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.enums.PointType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
        ServerPlayer attacker = null;


        // Vérifier si un jour est en cours
        if (!DaysManager.isDayInProgress()) {
            return; // Respawn normal de Minecraft
        }

        // Récupérer l'attaquant s'il y en a un
        String attackerUUID = null;
        String attackerTeamName = null;
        int attackerTeamId = 0;

        // Vérifier si la source de la mort est un joueur
        if (event.getSource().getEntity() instanceof ServerPlayer) {
            attacker = (ServerPlayer) event.getSource().getEntity();
            attackerUUID = attacker.getUUID().toString();
            attackerTeamName = TeamManager.getPlayerCurrentTeam(attackerUUID);

            attackerTeamId = TeamManager.getTeamId(attackerTeamName); // 0 si pas d'équipe
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
            DataManager.dataModify("player_" + player.getUUID() + "_stats", "deaths", DataManager.dataReadInt("player_" + player.getUUID() + "_stats", "deaths", 0) + 1);
            DataManager.dataModify("team_" + teamId + "_stats", "kill_streak", 0); // Reset kill streak de l'équipe tuée
            PointsManager.addPoints(teamId, PointType.DEATH, player);
        }

        // Attribution des points de kill à l'attaquant
        if (attackerTeamId > 0) {
            DataManager.dataModify("team_" + attackerTeamId + "_stats", "kills", DataManager.dataReadInt("team_" + attackerTeamId + "_stats", "kills", 0) + 1);
            DataManager.dataModify("team_" + attackerTeamId + "_stats", "kill_streak", DataManager.dataReadInt("team_" + attackerTeamId + "_stats", "kill_streak", 0) + 1);
            DataManager.dataModify("team_" + attackerTeamId + "_stats", "max_kill_streak", Math.max(
                    DataManager.dataReadInt("team_" + attackerTeamId + "_stats", "max_kill_streak", 0),
                    DataManager.dataReadInt("team_" + attackerTeamId + "_stats", "kill_streak", 0)
            ));

            PointsManager.addPoints(attackerTeamId, PointType.KILL, attacker);
            PointsManager.addPoints(attackerTeamId, PointType.KILL_STREAK, attacker);
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

            int currentDay = DaysManager.getCurrentDay();

            if (currentDay <= 6) {
                // Phase de préparation : respawn avec délai
                int deaths = DataManager.dataReadInt("player_" + player.getUUID() + "_stats", "deaths", 0);
                int delaySeconds = Math.min(deaths * 3, 10);

                // Trouver un coéquipier à spectater
                ServerPlayer teammate = NativeCameraController.findTeammateToSpectate(player);
                if (teammate != null) {
                    NativeCameraController.spectatePlayer(player, teammate);
                }

                // Compte à rebours
                for (int i = 0; i < delaySeconds; i++) {
                    final int secondsLeft = delaySeconds - i;
                    TaskScheduler.schedule(i * 20, () -> {
                        if (player.hasDisconnected()) return;
                        player.sendSystemMessage(Component.literal(
                                "§eRespawn dans §c" + secondsLeft + "§e seconde" + (secondsLeft > 1 ? "s" : "")
                        ), true);
                    });
                }

                // Respawn à la fin
                TaskScheduler.schedule(delaySeconds * 20, () -> {
                    if (player.hasDisconnected()) return;
                    if (spectatingPlayers.contains(player.getUUID())) {
                        SpectateManager.respawnPlayer(player);
                        NativeCameraController.cleanup(player);
                        player.sendSystemMessage(Component.literal("§aVous avez été respawn !"));
                    }
                });

            } else {
                // Phase de combat : spectate jusqu'au totem
                ServerPlayer teammate = NativeCameraController.findTeammateToSpectate(player);
                if (teammate != null) {
                    NativeCameraController.spectatePlayer(player, teammate);
                }

                player.sendSystemMessage(Component.literal(
                        "§c§lVous êtes mort pendant la phase de combat !\n" +
                                "§7Votre équipe doit utiliser un §6Totem de Revivalité§7 pour vous faire respawn.\n" +
                                "§7Appuyez sur §e[Shift]§7 pour changer de vue."
                ));
            }

            LOGGER.info("Player {} put in spectator mode", player.getName().getString());
        }
    }

    /**
     * Récupère la position du core d'une équipe
     */
    private static BlockPos getTeamCorePosition(String teamName) {
        String coreData = DataManager.dataRead(teamName, "core");
        if (coreData == null) return null;

        try {
            String[] coords = coreData.split(",");
            if (coords.length != 3) return null;

            return new BlockPos(
                    Integer.parseInt(coords[0].trim()),
                    Integer.parseInt(coords[1].trim()),
                    Integer.parseInt(coords[2].trim())
            );
        } catch (NumberFormatException e) {
            return null;
        }
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