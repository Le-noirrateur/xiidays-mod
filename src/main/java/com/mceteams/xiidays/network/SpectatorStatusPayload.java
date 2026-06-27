package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record SpectatorStatusPayload(int mode, String targetName) implements CustomPacketPayload {

    public static final int MODE_NONE = 0;
    public static final int MODE_TEAMMATE_WATCH = 1;
    public static final int MODE_BASE_SPECTATE = 2;
    public static final int MODE_FREE_SPECTATE = 3;

    public static final CustomPacketPayload.Type<SpectatorStatusPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MODID, "spectator_status"));

    public static final StreamCodec<FriendlyByteBuf, SpectatorStatusPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, SpectatorStatusPayload packet) {
            buf.writeInt(packet.mode);
            buf.writeUtf(packet.targetName);
        }

        @Override
        public @NotNull SpectatorStatusPayload decode(FriendlyByteBuf buf) {
            return new SpectatorStatusPayload(buf.readInt(), buf.readUtf());
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
