package com.mceteams.xiidays.item;

import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.game.EnigmaGenerator;
import com.mceteams.xiidays.game.EnigmaGenerator.Enigma;
import com.mceteams.xiidays.game.TeamManager;
import com.mceteams.xiidays.network.CoreMazeOpenPacket;
import com.mceteams.xiidays.world.CoreBlock;
import com.mceteams.xiidays.world.CoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CoreDestroyerItem extends Item {

    public CoreDestroyerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.FAIL;

        BlockState blockState = level.getBlockState(pos);
        if (!(blockState.getBlock() instanceof CoreBlock)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof CoreBlockEntity core)) {
                return InteractionResult.FAIL;
            }

            int coreTeamId = core.getTeamId();
            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            int playerTeamId = TeamManager.getTeamId(playerTeam);

            if (playerTeamId == coreTeamId) {
                player.displayClientMessage(
                        Component.literal("§cVous ne pouvez pas utiliser ceci sur votre propre Coeur !"),
                        true
                );
                return InteractionResult.FAIL;
            }

            if (TeamData.isMazeSolved(coreTeamId)) {
                player.displayClientMessage(
                        Component.literal("§6Le Core Maze de cette équipe a déjà été résolu !"),
                        true
                );
                return InteractionResult.FAIL;
            }

            int currentProgress = TeamData.getMazeProgress(coreTeamId);
            if (currentProgress > 0) {
                player.displayClientMessage(
                        Component.literal("§eUn joueur est déjà en train de résoudre ce puzzle..."),
                        true
                );
                return InteractionResult.FAIL;
            }

            List<Enigma> enigmas = EnigmaGenerator.generateEnigmas(3);

            List<CoreMazeOpenPacket.EnigmaPayload> payloads = new ArrayList<>();
            for (Enigma enigma : enigmas) {
                payloads.add(new CoreMazeOpenPacket.EnigmaPayload(
                        enigma.type().name(),
                        enigma.question(),
                        enigma.answer(),
                        enigma.hint()
                ));
            }

            PacketDistributor.sendToPlayer(serverPlayer, new CoreMazeOpenPacket(payloads, coreTeamId));

            player.displayClientMessage(
                    Component.literal("§6§lCore Maze §7- Résolvez les 3 énigmes pour affaiblir le coeur !"),
                    true
            );

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
