package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.enums.PointType;
import com.mceteams.xiidays.utils.data.TeamStatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import static com.mceteams.xiidays.utils.NotifyOptions.notifyPlayer;

public class PlayersHandler {

    private static String getBlockName(Block block) {
        if (block == Blocks.MEDIUM_AMETHYST_BUD) return "AMETHYST_ORE";
        if (block == Blocks.LARGE_AMETHYST_BUD) return "AMETHYST_ORE";
        if (block == Blocks.SMALL_AMETHYST_BUD) return "AMETHYST_ORE";
        if (block == Blocks.NETHERITE_BLOCK) return "NETHERITE_ORE";
        if (block == Blocks.ANCIENT_DEBRIS) return "NETHERITE_ORE";
        if (block == Blocks.REDSTONE_ORE) return "REDSTONE_ORE";
        if (block == Blocks.DIAMOND_ORE) return "DIAMOND_ORE";
        if (block == Blocks.EMERALD_ORE) return "EMERALD_ORE";
        if (block == Blocks.COPPER_ORE) return "COPPER_ORE";
        if (block == Blocks.LAPIS_ORE) return "LAPIS_ORE";
        if (block == Blocks.GOLD_ORE) return "GOLD_ORE";
        if (block == Blocks.IRON_ORE) return "IRON_ORE";
        if (block == Blocks.COAL_ORE) return "COAL_ORE";
        if (block == Blocks.DEEPSLATE_REDSTONE_ORE) return "REDSTONE_ORE";
        if (block == Blocks.DEEPSLATE_DIAMOND_ORE) return "DIAMOND_ORE";
        if (block == Blocks.DEEPSLATE_EMERALD_ORE) return "EMERALD_ORE";
        if (block == Blocks.DEEPSLATE_COPPER_ORE) return "COPPER_ORE";
        if (block == Blocks.DEEPSLATE_LAPIS_ORE) return "LAPIS_ORE";
        if (block == Blocks.DEEPSLATE_GOLD_ORE) return "GOLD_ORE";
        if (block == Blocks.DEEPSLATE_IRON_ORE) return "IRON_ORE";
        if (block == Blocks.DEEPSLATE_COAL_ORE) return "COAL_ORE";
        return null;
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer) {
            if (DaysManager.isDayInProgress()) {
                serverPlayer.removeEffectNoUpdate(net.minecraft.world.effect.MobEffects.DARKNESS);
                serverPlayer.removeEffectNoUpdate(net.minecraft.world.effect.MobEffects.BLINDNESS);
                serverPlayer.sendSystemMessage(Component.literal("Bienvenu(e) §l" + serverPlayer.getName().getString() + "§r, le jour §l" + DaysManager.getCurrentDay() + "§r est en cours !"));
                serverPlayer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.MASTER, 1.0f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!DaysManager.isDayInProgress()) return;

        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        if (teamName == null) return;

        int teamId = TeamManager.getTeamId(teamName);
        if (teamId <= 0) return;

        Block block = event.getState().getBlock();
        String blockName = getBlockName(block);

        if (blockName != null) {
            TeamStatsData.incrementBlocksMined(teamId);
            switch (blockName) {
                case "DIAMOND_ORE", "NETHERITE_ORE", "EMERALD_ORE",
                     "GOLD_ORE", "IRON_ORE", "COAL_ORE",
                     "LAPIS_ORE", "REDSTONE_ORE", "COPPER_ORE", "AMETHYST_ORE" -> PointsManager.addPoints(teamId, PointType.MINING, player, blockName);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerAttackPlayer(AttackEntityEvent event) {
        Entity attacker = event.getEntity();
        Entity target = event.getTarget();

        if (attacker instanceof ServerPlayer serverAttacker && target instanceof ServerPlayer serverTarget) {
            String attackerTeam = TeamManager.getPlayerCurrentTeam(serverAttacker.getUUID().toString());
            String targetTeam = TeamManager.getPlayerCurrentTeam(serverTarget.getUUID().toString());

            if (!DaysManager.isDayInProgress()) {
                event.setCanceled(true);
                return;
            }

            if (attackerTeam != null && attackerTeam.equals(targetTeam)) {
                event.setCanceled(true);
                notifyPlayer(serverAttacker, "§cVous ne pouvez pas attaquer un membre de votre équipe !", new NotifyOptions().sound(SoundEvents.LAVA_EXTINGUISH, SoundSource.MASTER, 1.0f, 1.0f).actionBar(true));
                serverAttacker.playNotifySound(SoundEvents.LAVA_EXTINGUISH, SoundSource.MASTER, 1.0f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!DaysManager.isDayInProgress()) return;

        float damage = event.getNewDamage();

        String attackerTeam = TeamManager.getPlayerCurrentTeam(attacker.getUUID().toString());
        String targetTeam = TeamManager.getPlayerCurrentTeam(target.getUUID().toString());

        if (attackerTeam == null) return;
        if (attackerTeam.equals(targetTeam)) return;

        int teamId = TeamManager.getTeamId(attackerTeam);
        int victimTeamId = TeamManager.getTeamId(targetTeam);
        if (teamId == 0) return;

        TeamStatsData.addDamageDealt(teamId, (int) damage);
        TeamStatsData.addDamageReceived(victimTeamId, (int) damage);
    }
}
