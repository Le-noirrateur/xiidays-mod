package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record GameOverPayload(boolean isWinner) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GameOverPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MODID, "game_over"));

    public static final StreamCodec<FriendlyByteBuf, GameOverPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, GameOverPayload packet) {
            buf.writeBoolean(packet.isWinner);
        }

        @Override
        public @NotNull GameOverPayload decode(FriendlyByteBuf buf) {
            return new GameOverPayload(buf.readBoolean());
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
