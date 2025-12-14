package com.mceteams.xiidays.network;

import com.mceteams.xiidays.enums.PointType;
import com.mceteams.xiidays.utils.DataManager;
import com.mceteams.xiidays.utils.PointsManager;
import com.mceteams.xiidays.utils.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

/**
 * Packet envoyé du CLIENT vers le SERVEUR
 * Valide une réponse du Core Maze
 */
public record CoreMazeAnswerPacket(int teamId, int enigmaIndex, boolean correct) implements CustomPacketPayload {

    public static final Type<CoreMazeAnswerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "core_maze_answer"));

    public static final StreamCodec<FriendlyByteBuf, CoreMazeAnswerPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, CoreMazeAnswerPacket packet) {
            buf.writeInt(packet.teamId);
            buf.writeInt(packet.enigmaIndex);
            buf.writeBoolean(packet.correct);
        }

        @Override
        public @NotNull CoreMazeAnswerPacket decode(FriendlyByteBuf buf) {
            return new CoreMazeAnswerPacket(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean()
            );
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CoreMazeAnswerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Vérifier que le joueur appartient bien à cette équipe
            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            int playerTeamId = TeamManager.getTeamId(playerTeam);

            if (playerTeamId != packet.teamId) {
                return; // Tentative de triche
            }

            // Enregistrer la progression du puzzle
            String progressKey = "team_" + packet.teamId + "_maze";
            int currentProgress = DataManager.dataReadInt(progressKey, "progress", 0);

            // Si c'est la bonne énigme dans l'ordre et qu'elle est correcte
            if (packet.correct && packet.enigmaIndex == currentProgress) {
                currentProgress++;
                DataManager.dataModify(progressKey, "progress", String.valueOf(currentProgress));

                // Si les 3 énigmes sont résolues
                if (currentProgress >= 3) {
                    // Marquer le puzzle comme résolu
                    DataManager.dataModify(progressKey, "solved", "true");

                    // Attribuer les points
                    PointsManager.addPoints(packet.teamId, PointType.CORE_MAZE, player);

                    // Notifier l'équipe
                    TeamManager.sendMessageToTeam(
                            packet.teamId,
                            Component.literal("§6§l[CORE MAZE] §a" + player.getName().getString() + " §ea résolu le puzzle ! §6+300 points"),
                            null
                    );

                    // Reset la progression pour la prochaine fois
                    DataManager.dataModify(progressKey, "progress", "0");
                }
            }
        });
    }
}
