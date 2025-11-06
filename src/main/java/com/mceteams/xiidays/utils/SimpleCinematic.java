package com.mceteams.xiidays.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class SimpleCinematic {

    /**
     * Lance une cinématique avec la commande native
     */
    public static void startCinematic(ServerPlayer player, BlockPos... points) {
        CommandSourceStack source = player.createCommandSourceStack();

        // Utiliser la commande native /camera pour une vue libre
        player.getServer().getCommands().performPrefixedCommand(source,
                "camera " + player.getName().getString() + " set minecraft:free"
        );

        // Téléporter aux points
        int delay = 0;
        for (BlockPos point : points) {
            final BlockPos pos = point;
            TaskScheduler.schedule(delay, () -> {
                if (!player.hasDisconnected()) {
                    player.teleportTo(player.serverLevel(),
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            0, -20 // yaw, pitch
                    );
                }
            });
            delay += 60; // 3 secondes par point
        }

        // Libérer la caméra à la fin
        TaskScheduler.schedule(delay, () -> {
            if (!player.hasDisconnected()) {
                player.getServer().getCommands().performPrefixedCommand(source,
                        "camera " + player.getName().getString() + " clear"
                );
            }
        });
    }
}