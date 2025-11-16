package com.mceteams.xiidays.network;

import com.mceteams.xiidays.client.ScoreboardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

/**
 * Paquet envoyé du SERVEUR vers le CLIENT
 * Contient toutes les données du scoreboard et ouvre l'écran
 */
public record OpenScoreboardPacket(List<TeamData> teams, String playerTeam) implements CustomPacketPayload {

    // Identifiant unique du paquet
    public static final Type<OpenScoreboardPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_scoreboard"));

    // Codec pour encoder/décoder les données
    public static final StreamCodec<FriendlyByteBuf, OpenScoreboardPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, OpenScoreboardPacket packet) {
            // Écrire l'équipe du joueur
            buf.writeUtf(packet.playerTeam);

            // Écrire le nombre d'équipes
            buf.writeInt(packet.teams.size());

            // Écrire chaque équipe
            for (TeamData team : packet.teams) {
                buf.writeUtf(team.teamName);
                buf.writeUtf(team.uuid);
                buf.writeInt(team.points);
                buf.writeBoolean(team.coreAlive);
            }
        }

        @Override
        public OpenScoreboardPacket decode(FriendlyByteBuf buf) {
            // Lire l'équipe du joueur
            String playerTeam = buf.readUtf();

            // Lire le nombre d'équipes
            int size = buf.readInt();
            List<TeamData> teams = new ArrayList<>();

            // Lire chaque équipe
            for (int i = 0; i < size; i++) {
                teams.add(new TeamData(
                        buf.readUtf(),  // teamName
                        buf.readUtf(),  // uuid
                        buf.readInt(),  // points
                        buf.readBoolean() // coreAlive
                ));
            }

            return new OpenScoreboardPacket(teams, playerTeam);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Gère la réception du paquet côté CLIENT
     */
    public static void handle(OpenScoreboardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Convertir TeamData en TeamScore pour l'écran
            List<ScoreboardScreen.TeamScore> teamScores = new ArrayList<>();

            for (TeamData data : packet.teams) {
                teamScores.add(new ScoreboardScreen.TeamScore(
                        data.teamName,
                        data.uuid,
                        data.points,
                        data.coreAlive,
                        null // RankChange sera géré plus tard si nécessaire
                ));
            }

            // Ouvrir l'écran du scoreboard
            Minecraft.getInstance().setScreen(new ScoreboardScreen(teamScores, packet.playerTeam));
        });
    }

    /**
     * Classe pour transférer les données d'une équipe via le réseau
     */
    public record TeamData(String teamName, String uuid, int points, boolean coreAlive) {}
}