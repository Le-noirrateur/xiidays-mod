package com.mceteams.xiidays.network;

import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.game.PointType;
import com.mceteams.xiidays.game.PointsManager;
import com.mceteams.xiidays.game.TeamManager;
import com.mceteams.xiidays.world.CoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record CoreMazeAnswerPacket(int teamId, int enigmaIndex, boolean correct) implements CustomPacketPayload {

    public static final Type<CoreMazeAnswerPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MODID, "core_maze_answer"));

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

            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            int playerTeamId = TeamManager.getTeamId(playerTeam);

            if (playerTeamId != packet.teamId) return;

            int currentProgress = TeamData.getMazeProgress(packet.teamId);

            if (packet.correct && packet.enigmaIndex == currentProgress) {
                currentProgress++;
                TeamData.setMazeProgress(packet.teamId, currentProgress);

                if (currentProgress >= 3) {
                    TeamData.setMazeSolved(packet.teamId, true);

                    PointsManager.addPoints(packet.teamId, PointType.CORE_MAZE, player);

                    TeamManager.sendMessageToTeam(
                            packet.teamId,
                            Component.literal("§6§l[CORE MAZE] §a" + player.getName().getString() + " §ea résolu le puzzle ! §6+300 points"),
                            null
                    );

                    TeamData.setMazeProgress(packet.teamId, 0);

                    // Destroy the core block
                    String coreCoords = TeamData.getCore(packet.teamId);
                    if (coreCoords != null) {
                        String[] parts = coreCoords.split(",");
                        BlockPos corePos = new BlockPos(
                                Integer.parseInt(parts[0]),
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2])
                        );
                        Level level = player.level();
                        BlockEntity be = level.getBlockEntity(corePos);
                        if (be instanceof CoreBlockEntity) {
                            level.destroyBlock(corePos, false);
                            TeamData.setCoreDestroyed(packet.teamId, true);
                        }
                    }
                }
            }
        });
    }
}
