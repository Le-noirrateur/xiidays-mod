package com.mceteams.xiidays.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID)
public class SpectateInput {

    private static final java.util.Map<java.util.UUID, Boolean> wasSneaking = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Double> lastYPosition = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Long> lastToggleTime = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;

        // ===== SHIFT : Changer de coéquipier =====
        boolean isSneaking = player.isShiftKeyDown();
        boolean wasSneakingBefore = wasSneaking.getOrDefault(player.getUUID(), false);

        if (isSneaking && !wasSneakingBefore) {
            NativeCameraController.switchToNextTeammate(player);
        }

        wasSneaking.put(player.getUUID(), isSneaking);

        // ===== DÉTECTION MOUVEMENT VERTICAL : Toggle TEAMMATE/FREECAM =====
        // En spectateur, on détecte un mouvement vertical rapide (double-espace en créatif)
        // Alternative : détecter si le joueur se déplace vers le haut
        double currentY = player.getY();
        Double previousY = lastYPosition.get(player.getUUID());

        if (previousY != null && DaysManager.isDayInProgress()) {
            double deltaY = currentY - previousY;

            // Si mouvement vertical significatif (> 0.5 bloc) et pas trop récent
            long now = System.currentTimeMillis();
            Long lastToggle = lastToggleTime.get(player.getUUID());

            if (Math.abs(deltaY) > 0.5 && (lastToggle == null || now - lastToggle > 1000)) {
                SpectateManager.toggleSpectateMode(player);
                lastToggleTime.put(player.getUUID(), now);
            }
        }

        lastYPosition.put(player.getUUID(), currentY);
    }
}