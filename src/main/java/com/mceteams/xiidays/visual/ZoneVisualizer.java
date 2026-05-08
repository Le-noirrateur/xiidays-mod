package com.mceteams.xiidays.visual;

import com.mceteams.xiidays.data.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID)
public class ZoneVisualizer {

    private static final int UPDATE_INTERVAL = 5;
    private static final double PARTICLE_SPACING = 1.5;

    private static final Vector3f[] TEAM_COLORS = {
            new Vector3f(1.00F, 0.27F, 0.27F),  // Red
            new Vector3f(0.27F, 0.53F, 1.00F),  // Blue
            new Vector3f(0.27F, 1.00F, 0.27F),  // Green
            new Vector3f(1.00F, 1.00F, 0.27F),  // Yellow
            new Vector3f(1.00F, 0.27F, 1.00F),  // Pink
            new Vector3f(0.27F, 1.00F, 1.00F),  // Cyan
            new Vector3f(1.00F, 0.55F, 0.00F),  // Orange
            new Vector3f(0.67F, 0.27F, 1.00F),  // Purple
    };

    private static final Map<UUID, Integer> viewers = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % UPDATE_INTERVAL != 0) return;

        Integer targetTeamId = viewers.get(player.getUUID());
        if (targetTeamId == null) return;

        ServerLevel level = player.serverLevel();

        for (String teamName : TeamData.getAllTeamNames()) {
            int teamId = TeamData.getTeamId(teamName);
            if (targetTeamId != -1 && targetTeamId != teamId) continue;

            String minStr = TeamData.getFreeCamZoneMin(teamId);
            String maxStr = TeamData.getFreeCamZoneMax(teamId);
            if (minStr == null || maxStr == null) continue;

            BlockPos min = parseBlockPos(minStr);
            BlockPos max = parseBlockPos(maxStr);
            if (min == null || max == null) continue;

            Vector3f color = TEAM_COLORS[Math.abs(teamId - 1) % TEAM_COLORS.length];
            DustParticleOptions particle = new DustParticleOptions(color, 1.5F);

            drawBoxEdges(level, min, max, particle);
        }
    }

    public static void startViewing(ServerPlayer player, int teamId) {
        viewers.put(player.getUUID(), teamId);
    }

    public static void stopViewing(ServerPlayer player) {
        viewers.remove(player.getUUID());
    }

    public static boolean isViewing(ServerPlayer player) {
        return viewers.containsKey(player.getUUID());
    }

    public static int getViewingTarget(ServerPlayer player) {
        return viewers.getOrDefault(player.getUUID(), -1);
    }

    // ─── Drawing ─────────────────────────────────────────────────

    private static void drawBoxEdges(ServerLevel level, BlockPos min, BlockPos max, DustParticleOptions particle) {
        int x1 = min.getX(), y1 = min.getY(), z1 = min.getZ();
        int x2 = max.getX(), y2 = max.getY(), z2 = max.getZ();

        drawLine(level, x1, y1, z1, x2, y1, z1, particle);
        drawLine(level, x1, y1, z1, x1, y2, z1, particle);
        drawLine(level, x1, y1, z1, x1, y1, z2, particle);

        drawLine(level, x2, y1, z1, x2, y2, z1, particle);
        drawLine(level, x2, y1, z1, x2, y1, z2, particle);

        drawLine(level, x1, y2, z1, x2, y2, z1, particle);
        drawLine(level, x1, y2, z1, x1, y2, z2, particle);

        drawLine(level, x2, y2, z1, x2, y2, z2, particle);

        drawLine(level, x1, y1, z2, x2, y1, z2, particle);
        drawLine(level, x1, y1, z2, x1, y2, z2, particle);

        drawLine(level, x2, y1, z2, x2, y2, z2, particle);

        drawLine(level, x1, y2, z2, x2, y2, z2, particle);
    }

    private static void drawLine(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2, DustParticleOptions particle) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(len / PARTICLE_SPACING));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            level.sendParticles(particle,
                    x1 + dx * t + 0.5,
                    y1 + dy * t + 0.5,
                    z1 + dz * t + 0.5,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static BlockPos parseBlockPos(String coords) {
        try {
            String[] parts = coords.split(",");
            return new BlockPos(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (Exception e) {
            return null;
        }
    }
}
