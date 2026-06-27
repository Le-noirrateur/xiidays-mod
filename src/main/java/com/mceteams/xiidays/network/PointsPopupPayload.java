package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record PointsPopupPayload(int points, String typeName) implements CustomPacketPayload {

    public static final Type<PointsPopupPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MODID, "points_popup"));

    public static final StreamCodec<FriendlyByteBuf, PointsPopupPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, PointsPopupPayload packet) {
            buf.writeInt(packet.points);
            buf.writeUtf(packet.typeName);
        }

        @Override
        public @NotNull PointsPopupPayload decode(FriendlyByteBuf buf) {
            return new PointsPopupPayload(buf.readInt(), buf.readUtf());
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
