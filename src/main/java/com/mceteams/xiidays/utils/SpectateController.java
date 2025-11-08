package com.mceteams.xiidays.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID)
public class SpectateController {

    // Mode de spectate actuel par joueur
    public enum SpectateMode {
        NONE,              // Pas en spectate
        TEAMMATE,          // Suit un coéquipier (jour actif)
        FREECAM_LIMITED,   // Free cam limité (jour inactif)
        CINEMATIC          // Cinématique (jour inactif)
    }

    private static final Map<UUID, SpectateMode> spectatorModes = new HashMap<>();
    private static final Map<UUID, UUID> spectatorTargets = new HashMap<>(); // spectateur → cible
    private static final Map<UUID, Boolean> wasSneaking = new HashMap<>();

    /**
     * Active le mode spectate pour un joueur
     */
    public static void enableSpectate(ServerPlayer player) {
        player.setGameMode(GameType.SPECTATOR);

        if (DaysManager.isDayInProgress()) {
            // Jour actif : spectate coéquipier uniquement
            setMode(player, SpectateMode.TEAMMATE);
            ServerPlayer teammate = findTeammateToSpectate(player);
            if (teammate != null) {
                spectatePlayer(player, teammate);
            }
        } else {
            // Jour inactif : free cam limité par défaut
            setMode(player, SpectateMode.FREECAM_LIMITED);
            player.setCamera(player); // Se voir soi-même
        }
    }

    /**
     * Désactive le spectate
     */
    public static void disableSpectate(ServerPlayer player) {
        player.setCamera(player);
        spectatorModes.remove(player.getUUID());
        spectatorTargets.remove(player.getUUID());
        player.setGameMode(GameType.SURVIVAL);
    }

    /**
     * Change le mode de spectate
     */
    public static void setMode(ServerPlayer player, SpectateMode mode) {
        spectatorModes.put(player.getUUID(), mode);

        switch (mode) {
            case TEAMMATE -> {
                ServerPlayer teammate = findTeammateToSpectate(player);
                if (teammate != null) {
                    spectatePlayer(player, teammate);
                    player.sendSystemMessage(Component.literal("§aMode : Spectate coéquipier"), true);
                }
            }
            case FREECAM_LIMITED -> {
                player.setCamera(player);
                player.sendSystemMessage(Component.literal("§eMode : Free cam (zone limitée)"), true);
            }
            case CINEMATIC -> {
                player.setCamera(player);
                player.sendSystemMessage(Component.literal("§bMode : Cinématique"), true);
            }
        }
    }

    /**
     * Fait suivre un coéquipier
     */
    public static void spectatePlayer(ServerPlayer spectator, ServerPlayer target) {
        spectator.setCamera(target);
        spectatorTargets.put(spectator.getUUID(), target.getUUID());
    }

    /**
     * Passe au coéquipier suivant
     */
    public static void switchToNextTeammate(ServerPlayer spectator) {
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return;

        List<ServerPlayer> teammates = getTeammates(spectator, spectatorTeam);

        if (teammates.isEmpty()) {
            spectator.setCamera(spectator);
            spectatorTargets.remove(spectator.getUUID());
            return;
        }

        UUID currentTarget = spectatorTargets.get(spectator.getUUID());
        int currentIndex = -1;

        for (int i = 0; i < teammates.size(); i++) {
            if (teammates.get(i).getUUID().equals(currentTarget)) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % teammates.size();
        spectatePlayer(spectator, teammates.get(nextIndex));

        spectator.sendSystemMessage(Component.literal(
                "§7Vue : §f" + teammates.get(nextIndex).getName().getString()
        ), true);
    }

    /**
     * Trouve un coéquipier à spectater
     */
    public static ServerPlayer findTeammateToSpectate(ServerPlayer spectator) {
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return null;

        List<ServerPlayer> teammates = getTeammates(spectator, spectatorTeam);
        return teammates.isEmpty() ? null : teammates.get(new Random().nextInt(teammates.size()));
    }

    /**
     * Récupère la liste des coéquipiers vivants
     */
    private static List<ServerPlayer> getTeammates(ServerPlayer spectator, String teamName) {
        List<ServerPlayer> teammates = new ArrayList<>();

        for (ServerPlayer player : spectator.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(spectator.getUUID())) continue;
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) continue;

            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            if (teamName.equals(playerTeam)) {
                teammates.add(player);
            }
        }

        return teammates;
    }

    /**
     * Tick principal pour gérer les restrictions
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;

        SpectateMode mode = spectatorModes.get(player.getUUID());
        if (mode == null) return;

        // Vérifier toutes les 5 ticks
        if (player.tickCount % 5 != 0) return;

        switch (mode) {
            case TEAMMATE -> handleTeammateMode(player);
            case FREECAM_LIMITED -> handleFreecamLimited(player);
            case CINEMATIC -> handleCinematicMode(player);
        }

        // Détection Shift pour changer de vue
        handleShiftInput(player, mode);
    }

    /**
     * Gère le mode spectate coéquipier
     */
    private static void handleTeammateMode(ServerPlayer player) {
        UUID targetUUID = spectatorTargets.get(player.getUUID());
        if (targetUUID == null) {
            // Pas de cible, chercher un coéquipier
            ServerPlayer teammate = findTeammateToSpectate(player);
            if (teammate != null) {
                spectatePlayer(player, teammate);
            }
            return;
        }

        // Vérifier que la cible est toujours valide
        ServerPlayer target = getPlayerByUUID(targetUUID, player.serverLevel());
        if (target == null || target.hasDisconnected() || target.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            // Cible invalide, chercher un autre coéquipier
            ServerPlayer newTarget = findTeammateToSpectate(player);
            if (newTarget != null) {
                spectatePlayer(player, newTarget);
            } else {
                player.setCamera(player);
                spectatorTargets.remove(player.getUUID());
            }
        }

        // IMPORTANT : Empêcher de spectater un ennemi
        String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        String targetTeam = TeamManager.getPlayerCurrentTeam(targetUUID.toString());

        if (playerTeam != null && !playerTeam.equals(targetTeam)) {
            // Le joueur essaie de spectater un ennemi, le ramener sur un coéquipier
            player.setCamera(player);
            ServerPlayer teammate = findTeammateToSpectate(player);
            if (teammate != null) {
                spectatePlayer(player, teammate);
            }
            player.sendSystemMessage(Component.literal("§cVous ne pouvez pas spectater un adversaire !"), true);
        }
    }

    /**
     * Gère le mode free cam limité
     */
    private static void handleFreecamLimited(ServerPlayer player) {
        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        if (teamName == null) return;

        SpectateZone zone = SpectateZone.getZone(teamName);
        if (zone == null) return;

        BlockPos playerPos = player.blockPosition();

        // Vérifier les limites
        if (!zone.isInside(playerPos)) {
            // Téléporter au point le plus proche à l'intérieur de la zone
            BlockPos nearestInside = zone.getNearestPointInside(playerPos);
            player.teleportTo(player.serverLevel(),
                    nearestInside.getX() + 0.5,
                    nearestInside.getY(),
                    nearestInside.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot()
            );

            player.sendSystemMessage(Component.literal("§cLimite de zone atteinte"), true);
        }
    }

    /**
     * Gère le mode cinématique
     */
    private static void handleCinematicMode(ServerPlayer player) {
        // Rien de spécial, la cinématique gère la caméra
    }

    /**
     * Détecte l'appui sur Shift pour changer de vue
     */
    private static void handleShiftInput(ServerPlayer player, SpectateMode mode) {
        boolean isSneaking = player.isShiftKeyDown();
        boolean wasSneakingBefore = wasSneaking.getOrDefault(player.getUUID(), false);

        if (isSneaking && !wasSneakingBefore) {
            if (mode == SpectateMode.TEAMMATE) {
                switchToNextTeammate(player);
            } else if (mode == SpectateMode.FREECAM_LIMITED && !DaysManager.isDayInProgress()) {
                // Passer en mode cinématique
                setMode(player, SpectateMode.CINEMATIC);
                CinematicManager.startCinematic(player, "default");
            } else if (mode == SpectateMode.CINEMATIC) {
                // Repasser en free cam
                setMode(player, SpectateMode.FREECAM_LIMITED);
                CinematicManager.stopCinematic(player);
            }
        }

        wasSneaking.put(player.getUUID(), isSneaking);
    }

    /**
     * Récupère le mode actuel d'un joueur
     */
    public static SpectateMode getMode(ServerPlayer player) {
        return spectatorModes.getOrDefault(player.getUUID(), SpectateMode.NONE);
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