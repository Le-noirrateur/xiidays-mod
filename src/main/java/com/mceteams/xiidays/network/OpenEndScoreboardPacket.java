package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mceteams.xiidays.XIIDays.MODID;

public record OpenEndScoreboardPacket(
        String winningTeamName,
        MvpData mvp,
        List<TeamEntry> teamStats
) implements CustomPacketPayload {

    public static final Type<OpenEndScoreboardPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_end_scoreboard"));

    public static final StreamCodec<FriendlyByteBuf, OpenEndScoreboardPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, OpenEndScoreboardPacket packet) {
            buf.writeUtf(packet.winningTeamName);
            buf.writeUtf(packet.mvp.playerName());
            buf.writeUtf(packet.mvp.playerUUID());
            buf.writeUtf(packet.mvp.teamName());
            buf.writeInt(packet.mvp.pointsContributed());
            buf.writeInt(packet.mvp.kills());
            buf.writeInt(packet.mvp.deaths());
            buf.writeInt(packet.mvp.score());
            buf.writeInt(packet.teamStats.size());
            for (TeamEntry team : packet.teamStats) {
                buf.writeUtf(team.teamName());
                buf.writeInt(team.points());
                buf.writeInt(team.kills());
                buf.writeInt(team.deaths());
                buf.writeInt(team.blocksMined());
                buf.writeInt(team.damageDealt());
                buf.writeInt(team.finalPosition());
            }
        }

        @Override
        public @NotNull OpenEndScoreboardPacket decode(FriendlyByteBuf buf) {
            String winningTeam = buf.readUtf();
            MvpData mvp = new MvpData(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            );
            int size = buf.readInt();
            List<TeamEntry> teamStats = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                teamStats.add(new TeamEntry(
                        buf.readUtf(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt()
                ));
            }
            return new OpenEndScoreboardPacket(winningTeam, mvp, teamStats);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenEndScoreboardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientEndScoreboardHandler.openEndScoreboard(packet));
    }

    public record MvpData(String playerName, String playerUUID, String teamName, int pointsContributed, int kills, int deaths, int score) {}

    public record TeamEntry(String teamName, int points, int kills, int deaths, int blocksMined, int damageDealt, int finalPosition) {}
}
