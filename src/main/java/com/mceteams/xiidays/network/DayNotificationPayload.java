package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record DayNotificationPayload(NotificationType notificationType, int newDay, int oldDay) implements CustomPacketPayload {

    public enum NotificationType { START, END }

    public static final CustomPacketPayload.Type<DayNotificationPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "day_notification"));

    public static final StreamCodec<FriendlyByteBuf, DayNotificationPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, DayNotificationPayload packet) {
            buf.writeEnum(packet.notificationType);
            buf.writeInt(packet.newDay);
            buf.writeInt(packet.oldDay);
        }

        @Override
        public @NotNull DayNotificationPayload decode(FriendlyByteBuf buf) {
            return new DayNotificationPayload(buf.readEnum(NotificationType.class), buf.readInt(), buf.readInt());
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
