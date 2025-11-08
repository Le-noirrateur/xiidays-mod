package com.mceteams.xiidays.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;

public class CinematicManager {

    // Cinématiques enregistrées
    private static final Map<String, Cinematic> cinematics = new HashMap<>();

    // Joueurs en cinématique
    private static final Set<UUID> playingCinematics = new HashSet<>();

    /**
     * Classe représentant une cinématique
     */
    public static class Cinematic {
        private final List<Waypoint> waypoints = new ArrayList<>();
        private final String name;
        private final boolean loop;
        private final double speed; // blocks/seconde (0 = durée custom par point)

        public Cinematic(String name, boolean loop, double speed) {
            this.name = name;
            this.loop = loop;
            this.speed = speed;
        }

        public void addWaypoint(Vec3 pos, float yaw, float pitch, int duration) {
            waypoints.add(new Waypoint(pos, yaw, pitch, duration));
        }

        public List<Waypoint> getWaypoints() {
            return waypoints;
        }

        public boolean isLoop() {
            return loop;
        }

        public double getSpeed() {
            return speed;
        }
    }

    /**
     * Point de passage
     */
    public static class Waypoint {
        public final Vec3 position;
        public final float yaw;
        public final float pitch;
        public final int duration; // En ticks (0 = auto)

        public Waypoint(Vec3 position, float yaw, float pitch, int duration) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.duration = duration;
        }
    }

    /**
     * Enregistre une cinématique
     */
    public static void registerCinematic(String name, Cinematic cinematic) {
        cinematics.put(name, cinematic);
        LOGGER.info("Registered cinematic: {}", name);
    }

    /**
     * Démarre une cinématique pour un joueur
     */
    public static void startCinematic(ServerPlayer player, String cinematicName) {
        Cinematic cinematic = cinematics.get(cinematicName);
        if (cinematic == null) {
            LOGGER.warn("Cinematic not found: {}", cinematicName);
            return;
        }

        playingCinematics.add(player.getUUID());
        playCinematic(player, cinematic);
    }

    /**
     * Joue une cinématique
     */
    private static void playCinematic(ServerPlayer player, Cinematic cinematic) {
        List<Waypoint> waypoints = cinematic.getWaypoints();
        if (waypoints.isEmpty()) return;

        int currentTick = 0;

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint current = waypoints.get(i);
            Waypoint next = (i < waypoints.size() - 1) ? waypoints.get(i + 1) : null;

            if (next != null) {
                // Transition vers le prochain point
                int duration = calculateDuration(current, next, cinematic.getSpeed());
                animateTransition(player, current, next, currentTick, duration);
                currentTick += duration;
            } else {
                // Dernier point
                final int delay = currentTick;
                final Vec3 pos = current.position;
                final float yaw = current.yaw;
                final float pitch = current.pitch;

                TaskScheduler.schedule(delay, () -> {
                    if (player.hasDisconnected()) return;
                    if (!playingCinematics.contains(player.getUUID())) return;

                    player.teleportTo(player.serverLevel(),
                            pos.x, pos.y, pos.z,
                            yaw, pitch
                    );
                });

                currentTick += current.duration > 0 ? current.duration : 40;
            }
        }

        // Si en boucle, relancer
        if (cinematic.isLoop()) {
            TaskScheduler.schedule(currentTick + 20, () -> {
                if (!player.hasDisconnected() && playingCinematics.contains(player.getUUID())) {
                    playCinematic(player, cinematic);
                }
            });
        } else {
            // Arrêter à la fin
            TaskScheduler.schedule(currentTick, () -> {
                stopCinematic(player);
            });
        }
    }

    /**
     * Calcule la durée entre deux points
     */
    private static int calculateDuration(Waypoint from, Waypoint to, double speed) {
        if (to.duration > 0) {
            return to.duration;
        }

        if (speed > 0) {
            double distance = from.position.distanceTo(to.position);
            double timeSeconds = distance / speed;
            return Math.max((int) (timeSeconds * 20), 1);
        }

        return 60; // 3 secondes par défaut
    }

    /**
     * Anime une transition fluide
     */
    private static void animateTransition(ServerPlayer player, Waypoint from, Waypoint to, int startTick, int duration) {
        int steps = Math.max(duration / 2, 1);

        for (int step = 0; step <= steps; step++) {
            final float progress = (float) step / steps;
            final int delay = startTick + (int) (step * ((float) duration / steps));

            final Vec3 pos = new Vec3(
                    lerp(from.position.x, to.position.x, progress),
                    lerp(from.position.y, to.position.y, progress),
                    lerp(from.position.z, to.position.z, progress)
            );

            final float yaw = lerpAngle(from.yaw, to.yaw, progress);
            final float pitch = lerpAngle(from.pitch, to.pitch, progress);

            TaskScheduler.schedule(delay, () -> {
                if (player.hasDisconnected()) return;
                if (!playingCinematics.contains(player.getUUID())) return;

                player.teleportTo(player.serverLevel(),
                        pos.x, pos.y, pos.z,
                        yaw, pitch
                );
            });
        }
    }

    /**
     * Arrête la cinématique pour un joueur
     */
    public static void stopCinematic(ServerPlayer player) {
        playingCinematics.remove(player.getUUID());
    }

    /**
     * Vérifie si un joueur est en cinématique
     */
    public static boolean isInCinematic(ServerPlayer player) {
        return playingCinematics.contains(player.getUUID());
    }

    private static double lerp(double start, double end, float progress) {
        return start + (end - start) * progress;
    }

    private static float lerpAngle(float start, float end, float progress) {
        float diff = end - start;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return start + diff * progress;
    }
}