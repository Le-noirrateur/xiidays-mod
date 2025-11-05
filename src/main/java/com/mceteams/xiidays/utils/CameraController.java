package com.mceteams.xiidays.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;
import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID)
public class CameraController {

    // Mapping spectateur → joueur suivi
    private static final Map<UUID, UUID> spectatorTargets = new HashMap<>();

    // Spectateurs en mode cinématique
    private static final Set<UUID> cinematicMode = new HashSet<>();

    /**
     * Définit quel joueur le spectateur doit suivre
     */
    public static void setSpectatorTarget(ServerPlayer spectator, ServerPlayer target) {
        if (target == null) {
            spectatorTargets.remove(spectator.getUUID());
            return;
        }

        spectatorTargets.put(spectator.getUUID(), target.getUUID());

        // Téléporter immédiatement au joueur
        spectator.teleportTo(target.serverLevel(),
                target.getX(),
                target.getY(),
                target.getZ(),
                target.getYRot(),
                target.getXRot()
        );

        LOGGER.debug("Spectator {} now following {}",
                spectator.getName().getString(),
                target.getName().getString()
        );
    }

    /**
     * Trouve automatiquement un coéquipier à suivre
     */
    public static ServerPlayer findTeammateToFollow(ServerPlayer spectator) {
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

        // Retourner un coéquipier aléatoire
        return teammates.get(new Random().nextInt(teammates.size()));
    }

    /**
     * Change le joueur suivi (suivant dans la liste)
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
            spectatorTargets.remove(spectator.getUUID());
            return;
        }

        // Trouver l'index du joueur actuel
        int currentIndex = -1;
        for (int i = 0; i < teammates.size(); i++) {
            if (teammates.get(i).getUUID().equals(currentTarget)) {
                currentIndex = i;
                break;
            }
        }

        // Passer au suivant (boucle)
        int nextIndex = (currentIndex + 1) % teammates.size();
        setSpectatorTarget(spectator, teammates.get(nextIndex));
    }

    /**
     * Tick pour suivre automatiquement le joueur cible
     */
    @SubscribeEvent
    public static void onSpectatorTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer spectator)) return;
        if (spectator.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;

        // Si en mode cinématique, ne pas suivre de joueur
        if (cinematicMode.contains(spectator.getUUID())) return;

        UUID targetUUID = spectatorTargets.get(spectator.getUUID());
        if (targetUUID == null) return;

        // Trouver le joueur cible
        ServerPlayer target = getPlayerByUUID(targetUUID, spectator.serverLevel());

        if (target == null || target.hasDisconnected() || target.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            // Le joueur n'est plus disponible, trouver un autre coéquipier
            ServerPlayer newTarget = findTeammateToFollow(spectator);
            if (newTarget != null) {
                setSpectatorTarget(spectator, newTarget);
            } else {
                spectatorTargets.remove(spectator.getUUID());
            }
            return;
        }

        // Suivre le joueur (synchroniser position et rotation)
        spectator.teleportTo(target.serverLevel(),
                target.getX(),
                target.getY(),
                target.getZ(),
                target.getYRot(),
                target.getXRot()
        );
    }

    /**
     * Active le mode cinématique
     */
    public static void enableCinematicMode(ServerPlayer spectator) {
        cinematicMode.add(spectator.getUUID());
        spectatorTargets.remove(spectator.getUUID());
    }

    /**
     * Désactive le mode cinématique
     */
    public static void disableCinematicMode(ServerPlayer spectator) {
        cinematicMode.remove(spectator.getUUID());

        // Reprendre le suivi d'un coéquipier
        ServerPlayer target = findTeammateToFollow(spectator);
        if (target != null) {
            setSpectatorTarget(spectator, target);
        }
    }

    /**
     * Vérifie si un spectateur est en mode cinématique
     */
    public static boolean isInCinematicMode(ServerPlayer spectator) {
        return cinematicMode.contains(spectator.getUUID());
    }

    /**
     * Nettoie les données d'un spectateur
     */
    public static void cleanup(ServerPlayer spectator) {
        spectatorTargets.remove(spectator.getUUID());
        cinematicMode.remove(spectator.getUUID());
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid, ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }
}