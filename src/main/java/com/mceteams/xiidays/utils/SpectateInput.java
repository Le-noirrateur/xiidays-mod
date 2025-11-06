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

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;

        boolean isSneaking = player.isShiftKeyDown();
        boolean wasSneakingBefore = wasSneaking.getOrDefault(player.getUUID(), false);

        // Détection du changement Shift OFF → ON
        if (isSneaking && !wasSneakingBefore) {
            NativeCameraController.switchToNextTeammate(player);
        }

        wasSneaking.put(player.getUUID(), isSneaking);
    }
}