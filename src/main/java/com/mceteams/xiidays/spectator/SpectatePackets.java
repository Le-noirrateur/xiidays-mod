package com.mceteams.xiidays.spectator;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.mceteams.xiidays.XIIDays.MODID;

public class SpectatePackets {

    /**
     * Packet pour changer de coéquipier
     * direction: true = suivant, false = précédent
     */
    public record SpectateSwitchPayload(boolean direction) implements CustomPacketPayload {

        public static final Type<SpectateSwitchPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "spectate_switch"));

        public static final StreamCodec<ByteBuf, SpectateSwitchPayload> CODEC =
                ByteBufCodecs.BOOL.map(SpectateSwitchPayload::new, SpectateSwitchPayload::direction);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /**
         * Gestion côté serveur
         */
        public static void handle(SpectateSwitchPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    SpectateManager.switchTeammate(player, payload.direction());
                }
            });
        }
    }
}
