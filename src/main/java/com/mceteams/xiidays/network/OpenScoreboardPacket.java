package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

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
                        buf.readBoolean()
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
     * MÉTHODE SIMPLE SANS @OnlyIn
     * Délègue au handler client
     */
    public static void handle(OpenScoreboardPacket packet, IPayloadContext context) {
        // Déléguer au handler client (chargé uniquement côté client)
        context.enqueueWork(() -> ClientScoreboardHandler.openScoreboard(packet));
    }

    public record TeamData(String teamName, String uuid, int points, boolean coreAlive) {}
}