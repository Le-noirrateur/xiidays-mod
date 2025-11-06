package com.mceteams.xiidays.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;
import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID)
public class NativeCameraController {

    // Map spectateur → cible
    private static final Map<UUID, UUID> spectatorTargets = new HashMap<>();

    /**
     * Fait suivre un joueur par un spectateur (MÉTHODE NATIVE)
     */
    public static void spectatePlayer(ServerPlayer spectator, ServerPlayer target) {
        if (target == null) {
            stopSpectating(spectator);
            return;
        }

        // Utiliser la méthode NATIVE de Minecraft
        spectator.setCamera(target);

        // Enregistrer la cible
        spectatorTargets.put(spectator.getUUID(), target.getUUID());

        LOGGER.info("Spectator {} now watching {}",
                spectator.getName().getString(),
                target.getName().getString()
        );
    }

    /**
     * Arrête de spectater
     */
    public static void stopSpectating(ServerPlayer spectator) {
        // Réinitialiser la caméra sur soi-même
        spectator.setCamera(spectator);
        spectatorTargets.remove(spectator.getUUID());
    }

    /**
     * Trouve un coéquipier à spectater
     */
    public static ServerPlayer findTeammateToSpectate(ServerPlayer spectator) {
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return null;

        List<ServerPlayer> teammates = new ArrayList<>();

        for (ServerPlayer player : spectator.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(spectator.getUUID())) continue;
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) continue;

            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            if (spectatorTeam.equals(playerTeam)) {
                teammates.add(player);
            }
        }

        if (teammates.isEmpty()) return null;

        // Retourner un aléatoire
        return teammates.get(new Random().nextInt(teammates.size()));
    }

    /**
     * Passe au coéquipier suivant
     */
    public static void switchToNextTeammate(ServerPlayer spectator) {
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return;

        List<ServerPlayer> teammates = new ArrayList<>();
        UUID currentTarget = spectatorTargets.get(spectator.getUUID());

        for (ServerPlayer player : spectator.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(spectator.getUUID())) continue;
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) continue;

            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            if (spectatorTeam.equals(playerTeam)) {
                teammates.add(player);
            }
        }

        if (teammates.isEmpty()) {
            stopSpectating(spectator);
            return;
        }

        // Trouver l'index actuel
        int currentIndex = -1;
        for (int i = 0; i < teammates.size(); i++) {
            if (teammates.get(i).getUUID().equals(currentTarget)) {
                currentIndex = i;
                break;
            }
        }

        // Suivant
        int nextIndex = (currentIndex + 1) % teammates.size();
        spectatePlayer(spectator, teammates.get(nextIndex));
    }

    /**
     * Vérifie automatiquement si la cible est toujours valide
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer spectator)) return;
        if (spectator.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;

        // Vérifier seulement toutes les secondes
        if (spectator.tickCount % 20 != 0) return;

        UUID targetUUID = spectatorTargets.get(spectator.getUUID());
        if (targetUUID == null) return;

        // Vérifier que la cible existe toujours
        ServerPlayer target = getPlayerByUUID(targetUUID, spectator.serverLevel());

        if (target == null || target.hasDisconnected() || target.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            // Cible invalide, trouver un autre coéquipier
            ServerPlayer newTarget = findTeammateToSpectate(spectator);
            if (newTarget != null) {
                spectatePlayer(spectator, newTarget);
            } else {
                stopSpectating(spectator);
            }
        }
    }

    /**
     * Nettoie les données
     */
    public static void cleanup(ServerPlayer spectator) {
        stopSpectating(spectator);
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid, net.minecraft.server.level.ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }
}