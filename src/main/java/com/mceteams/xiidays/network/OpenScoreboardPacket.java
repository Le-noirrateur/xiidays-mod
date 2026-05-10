package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mceteams.xiidays.XIIDays.MODID;

/**
 * Paquet envoyé du SERVEUR vers le CLIENT
 * Contient toutes les données du scoreboard et ouvre l'écran
 */
public record OpenScoreboardPacket(List<TeamData> teams, String playerTeam) implements CustomPacketPayload {

    public static final Type<OpenScoreboardPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_scoreboard"));

    public static final StreamCodec<FriendlyByteBuf, OpenScoreboardPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, OpenScoreboardPacket packet) {
            buf.writeUtf(packet.playerTeam != null ? packet.playerTeam : "Aucune");
            buf.writeInt(packet.teams.size());

            for (TeamData team : packet.teams) {
                buf.writeUtf(team.teamName != null ? team.teamName : "Unknown");
                buf.writeUtf(team.uuid != null ? team.uuid : UUID.randomUUID().toString());
                buf.writeInt(team.points);
                buf.writeBoolean(team.coreAlive);
                buf.writeInt(team.rankChange); // 0=NONE, 1=UP, -1=DOWN
            }
        }

        @Override
        public @NotNull OpenScoreboardPacket decode(FriendlyByteBuf buf) {
            String playerTeam = buf.readUtf();
            int size = buf.readInt();
            List<TeamData> teams = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                teams.add(new TeamData(
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readInt(),
                        buf.readBoolean(),
                        buf.readInt()
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
     * @param rankChange 1=UP (monté), -1=DOWN (descendu), 0=NONE (pas de changement)
     */
    public record TeamData(String teamName, String uuid, int points, boolean coreAlive, int rankChange) {}
}