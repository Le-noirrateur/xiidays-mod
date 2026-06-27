package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record DeathNotificationPayload(int respawnDelay) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DeathNotificationPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MODID, "death_notification"));

    public static final StreamCodec<FriendlyByteBuf, DeathNotificationPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, DeathNotificationPayload packet) {
            buf.writeInt(packet.respawnDelay);
        }

        @Override
        public @NotNull DeathNotificationPayload decode(FriendlyByteBuf buf) {
            return new DeathNotificationPayload(buf.readInt());
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
