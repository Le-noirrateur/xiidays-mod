package com.mceteams.xiidays.network;

import com.mceteams.xiidays.utils.ScoreboardManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

/**
 * Paquet envoyé du CLIENT vers le SERVEUR
 * Pas de référence client ici, donc safe pour le serveur
 */
public record RequestScoreboardPacket() implements CustomPacketPayload {

    public static final Type<RequestScoreboardPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "request_scoreboard"));

    public static final StreamCodec<FriendlyByteBuf, RequestScoreboardPacket> CODEC =
            StreamCodec.unit(new RequestScoreboardPacket());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Gère la réception du paquet côté SERVEUR
     * Pas besoin de @OnlyIn ici, car c'est du code serveur
     */
    public static void handle(RequestScoreboardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ScoreboardManager.ScoreboardData data = ScoreboardManager.getScoreboardData(player);

                PacketHandler.sendToClient(
                        new OpenScoreboardPacket(data.teams(), data.playerTeam()),
                        player
                );
            }
        });
    }
}