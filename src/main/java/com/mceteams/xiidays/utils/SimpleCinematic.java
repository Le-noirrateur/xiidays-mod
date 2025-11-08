package com.mceteams.xiidays.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public class SimpleCinematic {

    /**
     * Lance une cinématique avec la commande native /camera
     */
    public static void startCinematic(ServerPlayer player, BlockPos... points) {
        if (points == null || points.length == 0) {
            return; // Pas de points définis
        }

        CommandSourceStack source = player.createCommandSourceStack();

        // Activer la caméra libre
        Objects.requireNonNull(player.getServer()).getCommands().performPrefixedCommand(source,
                "camera " + player.getName().getString() + " set minecraft:free"
        );

        // Téléporter aux différents points avec délai
        int delay = 0;
        for (BlockPos point : points) {
            final BlockPos pos = point;
            TaskScheduler.schedule(delay, () -> {
                if (!player.hasDisconnected()) {
                    player.teleportTo(player.serverLevel(),
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            0, // yaw
                            -20 // pitch (regarde légèrement vers le bas)
                    );
                }
            });
            delay += 60; // 3 secondes par point (60 ticks)
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

    /**
     * Arrête une cinématique en cours
     */
    public static void stopCinematic(ServerPlayer player) {
        CommandSourceStack source = player.createCommandSourceStack();
        Objects.requireNonNull(player.getServer()).getCommands().performPrefixedCommand(source,
                "camera " + player.getName().getString() + " clear"
        );
    }
}