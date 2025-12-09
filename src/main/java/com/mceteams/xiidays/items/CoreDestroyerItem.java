package com.mceteams.xiidays.items;

import com.mceteams.xiidays.blocks.teamCore.teamCore;
import com.mceteams.xiidays.blocks.teamCore.teamCoreSettings;
import com.mceteams.xiidays.network.CoreMazeOpenPacket;
import com.mceteams.xiidays.utils.DataManager;
import com.mceteams.xiidays.utils.EnigmaGenerator;
import com.mceteams.xiidays.utils.EnigmaGenerator.Enigma;
import com.mceteams.xiidays.utils.TeamManager;
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

/**
 * Item Core Destroyer - Permet d'ouvrir le Core Maze sur un Team Core
 * Utilisable uniquement en Phase 2
 */
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

        // Vérifier si c'est un Team Core
        BlockState blockState = level.getBlockState(pos);
        if (!(blockState.getBlock() instanceof teamCoreSettings)) {
            return InteractionResult.PASS;
        }

        // Côté serveur uniquement
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof teamCore core)) {
                return InteractionResult.FAIL;
            }

            int coreTeamId = core.getTeamId();

            // Vérifier que le joueur n'attaque pas son propre core
            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            int playerTeamId = TeamManager.getTeamId(playerTeam);

            if (playerTeamId == coreTeamId) {
                player.displayClientMessage(
                        Component.literal("§cVous ne pouvez pas utiliser ceci sur votre propre Coeur !"),
                        true
                );
                return InteractionResult.FAIL;
            }

            // Vérifier si le puzzle est déjà résolu pour ce core
            String mazeKey = "team_" + coreTeamId + "_maze";
            boolean alreadySolved = DataManager.dataReadBoolean(mazeKey, "solved", false);

            if (alreadySolved) {
                player.displayClientMessage(
                        Component.literal("§6Le Core Maze de cette équipe a déjà été résolu !"),
                        true
                );
                return InteractionResult.FAIL;
            }

            // Vérifier si quelqu'un est déjà en train de résoudre le puzzle
            int currentProgress = DataManager.dataReadInt(mazeKey, "progress", 0);
            if (currentProgress > 0) {
                player.displayClientMessage(
                        Component.literal("§eUn joueur est déjà en train de résoudre ce puzzle..."),
                        true
                );
                return InteractionResult.FAIL;
            }

            // Générer 3 énigmes aléatoires
            List<Enigma> enigmas = EnigmaGenerator.generateEnigmas(3);

            // Convertir en payload pour le packet
            List<CoreMazeOpenPacket.EnigmaPayload> payloads = new ArrayList<>();
            for (Enigma enigma : enigmas) {
                payloads.add(new CoreMazeOpenPacket.EnigmaPayload(
                        enigma.type().name(),
                        enigma.question(),
                        enigma.answer(),
                        enigma.hint()
                ));
            }

            // Envoyer le packet pour ouvrir l'écran
            PacketDistributor.sendToPlayer(serverPlayer, new CoreMazeOpenPacket(payloads, coreTeamId));

            // Message de feedback
            player.displayClientMessage(
                    Component.literal("§6§lCore Maze §7- Résolvez les 3 énigmes pour affaiblir le coeur !"),
                    true
            );

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
