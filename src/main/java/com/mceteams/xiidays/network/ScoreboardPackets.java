package com.mceteams.xiidays.network;

import com.mceteams.xiidays.client.ScoreboardScreen;
import com.mceteams.xiidays.utils.ScoreboardManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

public class ScoreboardPackets {

    /**
     * Packet client → serveur : Demande des données de classement
     */
    public record RequestScoreboardPayload() implements CustomPacketPayload {

        public static final Type<RequestScoreboardPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "request_scoreboard"));

        public static final StreamCodec<ByteBuf, RequestScoreboardPayload> CODEC =
                StreamCodec.unit(new RequestScoreboardPayload());

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RequestScoreboardPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    sendScoreboardData(player);
                }
            });
        }

        private static void sendScoreboardData(ServerPlayer player) {
            String playerTeam = com.mceteams.xiidays.utils.TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            List<TeamData> teams = new ArrayList<>();

            // Récupérer le classement avec les flèches depuis ScoreboardManager
            List<ScoreboardManager.TeamRankingEntry> rankings = ScoreboardManager.getRankings();

            for (ScoreboardManager.TeamRankingEntry entry : rankings) {
                int teamId = entry.teamId();
                boolean coreAlive = !com.mceteams.xiidays.utils.DataManager.dataReadBoolean(
                        "team_" + teamId + "_config",
                        "core_destroyed",
                        false
                );

                String uuid = "0" + teamId + "234567891";

                // Convertir l'enum pour le packet
                RankChange change = switch (entry.change()) {
                    case UP -> RankChange.UP;
                    case DOWN -> RankChange.DOWN;
                    case NONE -> RankChange.NONE;
                };

                teams.add(new TeamData(
                        entry.teamName(),
                        uuid,
                        entry.points(),
                        coreAlive,
                        change
                ));
            }

            PacketDistributor.sendToPlayer(player,
                    new ScoreboardDataPayload(teams, playerTeam != null ? playerTeam : ""));
        }
    }

    /**
     * Packet serveur → client : Données du classement
     */
    public record ScoreboardDataPayload(List<TeamData> teams, String playerTeam) implements CustomPacketPayload {

        public static final Type<ScoreboardDataPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "scoreboard_data"));

        public static final StreamCodec<ByteBuf, ScoreboardDataPayload> CODEC = StreamCodec.composite(
                TeamData.STREAM_CODEC.apply(ByteBufCodecs.list()),
                ScoreboardDataPayload::teams,
                ByteBufCodecs.STRING_UTF8,
                ScoreboardDataPayload::playerTeam,
                ScoreboardDataPayload::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ScoreboardDataPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                List<ScoreboardScreen.TeamScore> scores = new ArrayList<>();

                for (TeamData data : payload.teams) {
                    // Convertir l'enum pour le client
                    ScoreboardScreen.RankChange clientChange = switch (data.change) {
                        case UP -> ScoreboardScreen.RankChange.UP;
                        case DOWN -> ScoreboardScreen.RankChange.DOWN;
                        case NONE -> ScoreboardScreen.RankChange.NONE;
                    };

                    scores.add(new ScoreboardScreen.TeamScore(
                            data.name,
                            data.uuid,
                            data.points,
                            data.coreAlive,
                            clientChange
                    ));
                }

                Minecraft.getInstance().setScreen(
                        new ScoreboardScreen(scores, payload.playerTeam)
                );
            });
        }
    }

    /**
     * Enum pour les changements de rang (compatible réseau)
     */
    public enum RankChange {
        UP, DOWN, NONE
    }

    // Codec personnalisé pour RankChange
    private static final StreamCodec<ByteBuf, RankChange> RANK_CHANGE_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull RankChange decode(@NotNull ByteBuf buffer) {
            int ordinal = buffer.readByte();
            return RankChange.values()[ordinal];
        }

        @Override
        public void encode(@NotNull ByteBuf buffer, RankChange value) {
            buffer.writeByte(value.ordinal());
        }
    };

    /**
     * Classe de données pour une équipe (serializable)
     */
    public record TeamData(String name, String uuid, int points, boolean coreAlive, RankChange change) {

        public static final StreamCodec<ByteBuf, TeamData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                TeamData::name,
                ByteBufCodecs.STRING_UTF8,
                TeamData::uuid,
                ByteBufCodecs.INT,
                TeamData::points,
                ByteBufCodecs.BOOL,
                TeamData::coreAlive,
                RANK_CHANGE_CODEC,
                TeamData::change,
                TeamData::new
        );

    }
}