package com.mceteams.xiidays.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CinematicConfig {

    // Cinématiques prédéfinies par nom
    private static final Map<String, CinematicPath> PREDEFINED_CINEMATICS = new HashMap<>();

    /**
     * Initialise les cinématiques par défaut
     */
    public static void initDefaultCinematics() {
        // Cinématique 1 : Tour de la map principale
        CinematicPath mainTour = new CinematicPath(true, 15.0); // Transition fluide, 15 blocks/sec
        mainTour
                .addWaypoint(new Vec3(100, 80, 100), 0, -10, 0)
                .addWaypoint(new Vec3(200, 90, 150), 45, -15, 0)
                .addWaypoint(new Vec3(150, 100, 250), 90, -20, 0)
                .addWaypoint(new Vec3(50, 85, 200), 180, -10, 0)
                .addWaypoint(new Vec3(0, 75, 100), 270, -5, 0);

        PREDEFINED_CINEMATICS.put("main_tour", mainTour);

        // Cinématique 2 : Focus sur les points d'intérêt (avec cuts)
        CinematicPath pointsOfInterest = new CinematicPath(false, 0); // Cuts, durées custom
        pointsOfInterest
                .addWaypoint(new Vec3(100, 70, 100), 0, 0, 60)    // 3 secondes
                .addWaypoint(new Vec3(200, 80, 200), 90, -10, 60) // 3 secondes
                .addWaypoint(new Vec3(-100, 75, -100), 180, -5, 60); // 3 secondes

        PREDEFINED_CINEMATICS.put("poi", pointsOfInterest);

        // Cinématique 3 : Survol lent de la map
        CinematicPath slowFly = new CinematicPath(true, 5.0); // Très lent
        slowFly
                .addWaypoint(new Vec3(0, 120, 0), 0, -45, 0)
                .addWaypoint(new Vec3(100, 120, 0), 45, -45, 0)
                .addWaypoint(new Vec3(100, 120, 100), 90, -45, 0)
                .addWaypoint(new Vec3(0, 120, 100), 135, -45, 0);

        PREDEFINED_CINEMATICS.put("slow_fly", slowFly);
    }

    /**
     * Génère une cinématique circulaire automatique autour d'un point
     */
    public static CinematicPath generateCirclePath(BlockPos center, int radius, int height, int numPoints, double speed) {
        CinematicPath path = new CinematicPath(true, speed);

        for (int i = 0; i < numPoints; i++) {
            double angle = (i / (double) numPoints) * 2 * Math.PI;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            // Calculer yaw pour regarder vers le centre
            float yaw = (float) Math.toDegrees(Math.atan2(center.getZ() - z, center.getX() - x));
            float pitch = -20.0f;

            path.addWaypoint(new Vec3(x, height, z), yaw, pitch, 0);
        }

        return path;
    }

    /**
     * Génère une cinématique en ligne droite
     */
    public static CinematicPath generateLinearPath(Vec3 start, Vec3 end, float yaw, float pitch, double speed) {
        CinematicPath path = new CinematicPath(true, speed);
        path.addWaypoint(start, yaw, pitch, 0);
        path.addWaypoint(end, yaw, pitch, 0);
        return path;
    }

    /**
     * Récupère une cinématique prédéfinie
     */
    public static CinematicPath getPredefined(String name) {
        return PREDEFINED_CINEMATICS.get(name);
    }

    /**
     * Enregistre une cinématique personnalisée
     */
    public static void registerCinematic(String name, CinematicPath path) {
        PREDEFINED_CINEMATICS.put(name, path);
    }

    /**
     * Liste toutes les cinématiques disponibles
     */
    public static List<String> listCinematics() {
        return new ArrayList<>(PREDEFINED_CINEMATICS.keySet());
    }
}