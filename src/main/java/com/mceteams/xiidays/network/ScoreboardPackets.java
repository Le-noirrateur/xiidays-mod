package com.mceteams.xiidays.network;

import com.mceteams.xiidays.client.ScoreboardScreen;
import com.mceteams.xiidays.utils.DataManager;
import com.mceteams.xiidays.utils.TeamManager;
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
import java.util.Comparator;
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
            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            List<TeamData> teams = new ArrayList<>();

            // Récupérer toutes les équipes
            for (String teamName : TeamManager.getAllTeams()) {
                int teamId = TeamManager.getTeamId(teamName);
                if (teamId == 0) continue;

                int points = DataManager.dataReadInt("team_" + teamId + "_points", "total", 0);
                boolean coreAlive = !DataManager.dataReadBoolean("team_" + teamId + "_config", "core_destroyed", false);
                String uuid = "0" + teamId + "234567891"; // UUID fictif basé sur ID

                teams.add(new TeamData(teamName, uuid, points, coreAlive));
            }

            // Trier par points décroissants
            teams.sort(Comparator.comparingInt(t -> -t.points));

            // Envoyer au client
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
                    scores.add(new ScoreboardScreen.TeamScore(
                            data.name,
                            data.uuid,
                            data.points,
                            data.coreAlive
                    ));
                }

                Minecraft.getInstance().setScreen(
                        new ScoreboardScreen(scores, payload.playerTeam)
                );
            });
        }
    }

    /**
     * Classe de données pour une équipe (serializable)
     */
    public record TeamData(String name, String uuid, int points, boolean coreAlive) {

        public static final StreamCodec<ByteBuf, TeamData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                TeamData::name,
                ByteBufCodecs.STRING_UTF8,
                TeamData::uuid,
                ByteBufCodecs.INT,
                TeamData::points,
                ByteBufCodecs.BOOL,
                TeamData::coreAlive,
                TeamData::new
        );
    }
}