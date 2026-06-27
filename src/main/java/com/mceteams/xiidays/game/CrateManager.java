package com.mceteams.xiidays.game;

import com.mceteams.xiidays.data.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID)
public class CrateManager {
    private static final Random RANDOM = new Random();

    public static void startDrops() {
        scheduleNextDrop();
    }

    public static void stopDrops() {
    }

    private static void scheduleNextDrop() {
        int delay = 20 * (120 + RANDOM.nextInt(181));
        TaskScheduler.schedule(delay, CrateManager::dropCrate);
    }

    private static void dropCrate() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !DaysManager.isDayInProgress()) return;

        List<ServerPlayer> alivePlayers = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String team = TeamManager.getPlayerCurrentTeam(p.getUUID().toString());
            if (team != null && !TeamData.isEliminated(TeamData.getTeamId(team))) {
                alivePlayers.add(p);
            }
        }
        if (alivePlayers.isEmpty()) return;

        ServerPlayer target = alivePlayers.get(RANDOM.nextInt(alivePlayers.size()));
        ServerLevel level = target.level();

        BlockPos dropPos = target.blockPosition().offset(
                RANDOM.nextInt(21) - 10,
                5,
                RANDOM.nextInt(21) - 10
        );

        level.setBlock(dropPos, Blocks.BARREL.defaultBlockState(), 3);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.playSound(SoundEvents.AMBIENT_CAVE.value(), 1f, 1f);
            p.sendSystemMessage(Component.literal("§6§lUn colis est tombé quelque part !"));
        }

        if (DaysManager.isDayInProgress()) {
            scheduleNextDrop();
        }
    }

    @SubscribeEvent
    public static void onCrateOpen(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        var level = event.getLevel();
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        if (state.getBlock() != Blocks.BARREL) return;

        if (!event.getEntity().isShiftKeyDown()) return;

        var player = event.getEntity();
        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        if (teamName == null) return;
        int teamId = TeamData.getTeamId(teamName);
        if (teamId <= 0) return;

        level.destroyBlock(pos, true);
        PointsManager.addPoints(teamId, PointType.CRATE, player);

        for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            p.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1f, 1f);
            p.sendSystemMessage(Component.literal("§6L'équipe §e" + teamName + " §6a ouvert un colis !"));
        }
    }
}
