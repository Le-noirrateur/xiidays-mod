package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record EliminationNotificationPayload(String teamName, boolean gameOver) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EliminationNotificationPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "elimination_notification"));

    public static final StreamCodec<FriendlyByteBuf, EliminationNotificationPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, EliminationNotificationPayload packet) {
            buf.writeUtf(packet.teamName);
            buf.writeBoolean(packet.gameOver);
        }

        @Override
        public @NotNull EliminationNotificationPayload decode(FriendlyByteBuf buf) {
            return new EliminationNotificationPayload(buf.readUtf(), buf.readBoolean());
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EliminationNotificationPayload packet, IPayloadContext context) {
        context.enqueueWork(() ->
                com.mceteams.xiidays.visual.HUDOverlayHandler.onEliminationNotification(packet)
        );
    }
}
