package com.mceteams.xiidays.utils.spectate;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class SpectateZone {

    // Zones par équipe
    private static final Map<String, SpectateZone> zones = new HashMap<>();

    private final BlockPos min;
    private final BlockPos max;
    private final int minY; // Limite basse (anti-wallhack minerais)

    public SpectateZone(BlockPos min, BlockPos max, int minY) {
        this.min = new BlockPos(
                Math.min(min.getX(), max.getX()),
                Math.min(min.getY(), max.getY()),
                Math.min(min.getZ(), max.getZ())
        );
        this.max = new BlockPos(
                Math.max(min.getX(), max.getX()),
                Math.max(min.getY(), max.getY()),
                Math.max(min.getZ(), max.getZ())
        );
        this.minY = minY;
    }

    /**
     * Vérifie si une position est dans la zone
     */
    public boolean isInside(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= minY && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * Récupère le point le plus proche à l'intérieur de la zone
     */
    public BlockPos getNearestPointInside(BlockPos pos) {
        int x = Math.max(min.getX(), Math.min(max.getX(), pos.getX()));
        int y = Math.max(minY, Math.min(max.getY(), pos.getY()));
        int z = Math.max(min.getZ(), Math.min(max.getZ(), pos.getZ()));

        return new BlockPos(x, y, z);
    }

    /**
     * Définit une zone pour une équipe
     */
    public static void setZone(String teamName, BlockPos pos1, BlockPos pos2, int minY) {
        zones.put(teamName, new SpectateZone(pos1, pos2, minY));
    }

    /**
     * Récupère la zone d'une équipe
     */
    public static SpectateZone getZone(String teamName) {
        return zones.get(teamName);
    }

    /**
     * Supprime une zone
     */
    public static void removeZone(String teamName) {
        zones.remove(teamName);
    }

    /**
     * Vérifie si une équipe a une zone définie
     */
    public static boolean hasZone(String teamName) {
        return zones.containsKey(teamName);
    }

    public BlockPos getMin() { return min; }
    public BlockPos getMax() { return max; }
    public int getMinY() { return minY; }
}