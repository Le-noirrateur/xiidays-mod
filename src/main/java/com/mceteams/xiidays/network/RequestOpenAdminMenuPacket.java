package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record RequestOpenAdminMenuPacket() implements CustomPacketPayload {

    public static final Type<RequestOpenAdminMenuPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "request_open_admin_menu"));

    public static final StreamCodec<FriendlyByteBuf, RequestOpenAdminMenuPacket> CODEC =
            StreamCodec.unit(new RequestOpenAdminMenuPacket());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenAdminMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.hasPermissions(4)) {
                PacketDistributor.sendToPlayer(player, new OpenAdminMenuPacket());
            }
        });
    }
}
