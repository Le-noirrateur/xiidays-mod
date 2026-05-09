package com.mceteams.xiidays.item;

import com.mceteams.xiidays.game.PointType;
import com.mceteams.xiidays.game.PointsManager;
import com.mceteams.xiidays.game.TeamManager;
import com.mceteams.xiidays.network.PointsPopupPayload;
import com.mceteams.xiidays.spectator.SpectateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class TotemRevivalityItem extends Item {

    public TotemRevivalityItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(target instanceof ServerPlayer targetPlayer) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (!SpectateManager.isSpectating(targetPlayer)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§cCe joueur n'est pas mort !"), true);
            return InteractionResult.FAIL;
        }

        String userTeam = TeamManager.getPlayerCurrentTeam(serverPlayer.getUUID().toString());
        String targetTeam = TeamManager.getPlayerCurrentTeam(targetPlayer.getUUID().toString());

        if (userTeam == null || !userTeam.equals(targetTeam)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§cVous ne pouvez revivre qu'un coéquipier !"), true);
            return InteractionResult.FAIL;
        }

        boolean success = SpectateManager.respawnPlayer(targetPlayer);
        if (!success) {
            return InteractionResult.FAIL;
        }

        PointsManager.addPoints(TeamManager.getTeamId(userTeam), PointType.TOTEM, serverPlayer);

        if (!serverPlayer.hasInfiniteMaterials()) {
            stack.shrink(1);
        }

        level.playSound(null, targetPlayer.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        PacketDistributor.sendToPlayer(targetPlayer, new PointsPopupPayload(0, "REVIVED"));
        serverPlayer.sendSystemMessage(Component.literal(
                "§aVous avez revivifié §e" + targetPlayer.getName().getString() + "§a !"));

        return InteractionResult.SUCCESS;
    }
}
