package com.mceteams.xiidays.network;

import com.mceteams.xiidays.screen.AdminScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record OpenAdminMenuPacket() implements CustomPacketPayload {

    public static final Type<OpenAdminMenuPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_admin_menu"));

    public static final StreamCodec<FriendlyByteBuf, OpenAdminMenuPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, OpenAdminMenuPacket packet) {}

        @Override
        public @NotNull OpenAdminMenuPacket decode(FriendlyByteBuf buf) {
            return new OpenAdminMenuPacket();
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenAdminMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(new AdminScreen())
        );
    }
}