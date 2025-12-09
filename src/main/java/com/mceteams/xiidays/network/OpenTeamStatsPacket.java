package com.mceteams.xiidays.network;

import com.mceteams.xiidays.menus.TeamStatsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mceteams.xiidays.XIIDays.MODID;

/**
 * Packet envoyé du SERVEUR vers le CLIENT
 * Ouvre l'écran des stats détaillées d'une équipe
 */
public record OpenTeamStatsPacket(TeamStatsData teamStats, List<PlayerStatsData> playerStats) implements CustomPacketPayload {

    public static final Type<OpenTeamStatsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_team_stats"));

    public static final StreamCodec<FriendlyByteBuf, OpenTeamStatsPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, OpenTeamStatsPacket packet) {
            // Team stats
            TeamStatsData ts = packet.teamStats;
            buf.writeUtf(ts.teamName);
            buf.writeInt(ts.points);
            buf.writeInt(ts.kills);
            buf.writeInt(ts.deaths);
            buf.writeInt(ts.blocksMined);
            buf.writeInt(ts.damageDealt);
            buf.writeInt(ts.damageReceived);
            buf.writeInt(ts.pointGain);
            buf.writeInt(ts.pointLoss);

            // Player stats
            buf.writeInt(packet.playerStats.size());
            for (PlayerStatsData ps : packet.playerStats) {
                buf.writeUtf(ps.playerName);
                buf.writeUtf(ps.playerUUID);
                buf.writeInt(ps.kills);
                buf.writeInt(ps.deaths);
                buf.writeInt(ps.points);
                buf.writeInt(ps.blocksMined);
                buf.writeInt(ps.damageDealt);
            }
        }

        @Override
        public @NotNull OpenTeamStatsPacket decode(FriendlyByteBuf buf) {
            // Team stats
            TeamStatsData teamStats = new TeamStatsData(
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            );

            // Player stats
            int playerCount = buf.readInt();
            List<PlayerStatsData> playerStats = new ArrayList<>();
            for (int i = 0; i < playerCount; i++) {
                playerStats.add(new PlayerStatsData(
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt()
                ));
            }

            return new OpenTeamStatsPacket(teamStats, playerStats);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTeamStatsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new TeamStatsScreen(packet.teamStats, packet.playerStats));
        });
    }

    public record TeamStatsData(
            String teamName,
            int points,
            int kills,
            int deaths,
            int blocksMined,
            int damageDealt,
            int damageReceived,
            int pointGain,
            int pointLoss
    ) {}

    public record PlayerStatsData(
            String playerName,
            String playerUUID,
            int kills,
            int deaths,
            int points,
            int blocksMined,
            int damageDealt
    ) {}
}
