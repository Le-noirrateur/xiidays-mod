package com.mceteams.xiidays.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;

public class CinematicPath {

    private final List<Waypoint> waypoints = new ArrayList<>();
    private final boolean smoothTransitions; // true = transition, false = cut
    private final double speed; // blocks par seconde (0 = temps personnalisé)

    /**
     * Point de passage de la cinématique
     *
     * @param customDuration En ticks (0 = auto)
     */
        public record Waypoint(Vec3 position, float yaw, float pitch, int customDuration) {

        public Waypoint(BlockPos pos, float yaw, float pitch) {
                this(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), yaw, pitch, 0);
            }
        }

    /**
     * Constructeur
     * @param smoothTransitions true pour transitions fluides, false pour cuts
     * @param speed vitesse en blocks/seconde (0 pour utiliser les durées custom)
     */
    public CinematicPath(boolean smoothTransitions, double speed) {
        this.smoothTransitions = smoothTransitions;
        this.speed = speed;
    }

    /**
     * Ajoute un point de passage
     */
    public CinematicPath addWaypoint(Vec3 position, float yaw, float pitch, int customDuration) {
        waypoints.add(new Waypoint(position, yaw, pitch, customDuration));
        return this;
    }

    public CinematicPath addWaypoint(BlockPos pos, float yaw, float pitch) {
        waypoints.add(new Waypoint(pos, yaw, pitch));
        return this;
    }

    /**
     * Lance la cinématique pour un joueur
     */
    public void play(ServerPlayer player) {
        if (waypoints.isEmpty()) {
            LOGGER.error("Cannot play cinematic with no waypoints");
            return;
        }

        CameraController.enableCinematicMode(player);

        int currentTick = 0;

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint current = waypoints.get(i);

            if (smoothTransitions && i < waypoints.size() - 1) {
                // Transition fluide vers le prochain point
                Waypoint next = waypoints.get(i + 1);
                int duration = calculateDuration(current, next);

                animateTransition(player, current, next, currentTick, duration);
                currentTick += duration;

            } else {
                // Cut instantané
                final int delay = currentTick;
                final Vec3 pos = current.position;
                final float yaw = current.yaw;
                final float pitch = current.pitch;

                TaskScheduler.schedule(delay, () -> {
                    if (player.hasDisconnected()) return;
                    player.teleportTo(player.serverLevel(),
                            pos.x, pos.y, pos.z,
                            yaw, pitch
                    );
                });

                if (current.customDuration > 0) {
                    currentTick += current.customDuration;
                } else {
                    currentTick += 40; // 2 secondes par défaut
                }
            }
        }

        // Désactiver le mode cinématique à la fin
        TaskScheduler.schedule(currentTick, () -> {
            if (!player.hasDisconnected()) {
                CameraController.disableCinematicMode(player);
            }
        });

        LOGGER.info("Started cinematic for {} with {} waypoints over {} ticks",
                player.getName().getString(),
                waypoints.size(),
                currentTick
        );
    }

    /**
     * Calcule la durée entre deux points
     */
    private int calculateDuration(Waypoint from, Waypoint to) {
        // Si durée custom définie sur le point de destination
        if (to.customDuration > 0) {
            return to.customDuration;
        }

        // Si vitesse définie, calculer selon la distance
        if (speed > 0) {
            double distance = from.position.distanceTo(to.position);
            double timeSeconds = distance / speed;
            return (int) (timeSeconds * 20); // Convertir en ticks
        }

        // Durée par défaut : 2 secondes
        return 40;
    }

    /**
     * Anime une transition fluide entre deux points
     */
    private void animateTransition(ServerPlayer player, Waypoint from, Waypoint to, int startTick, int duration) {
        int steps = Math.max(duration / 2, 1); // Un step tous les 2 ticks minimum

        for (int step = 0; step <= steps; step++) {
            final float progress = (float) step / steps; // 0.0 à 1.0
            final int delay = startTick + (int) (step * ((float) duration / steps));

            // Interpolation linéaire de la position
            final Vec3 interpolatedPos = new Vec3(
                    lerp(from.position.x, to.position.x, progress),
                    lerp(from.position.y, to.position.y, progress),
                    lerp(from.position.z, to.position.z, progress)
            );

            // Interpolation de la rotation
            final float interpolatedYaw = lerpAngle(from.yaw, to.yaw, progress);
            final float interpolatedPitch = lerpAngle(from.pitch, to.pitch, progress);

            TaskScheduler.schedule(delay, () -> {
                if (player.hasDisconnected()) return;
                if (!CameraController.isInCinematicMode(player)) return;

                player.teleportTo(player.serverLevel(),
                        interpolatedPos.x,
                        interpolatedPos.y,
                        interpolatedPos.z,
                        interpolatedYaw,
                        interpolatedPitch
                );
            });
        }
    }

    /**
     * Interpolation linéaire
     */
    private double lerp(double start, double end, float progress) {
        return start + (end - start) * progress;
    }

    /**
     * Interpolation d'angle (gère les 360°)
     */
    private float lerpAngle(float start, float end, float progress) {
        float diff = end - start;

        // Normaliser pour prendre le chemin le plus court
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;

        return start + diff * progress;
    }

    /**
     * Arrête la cinématique pour un joueur
     */
    public static void stop(ServerPlayer player) {
        CameraController.disableCinematicMode(player);
    }
}