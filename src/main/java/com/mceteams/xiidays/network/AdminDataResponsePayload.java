package com.mceteams.xiidays.network;

import com.mceteams.xiidays.screen.AdminScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record AdminDataResponsePayload(String dataType, String data) implements CustomPacketPayload {

    public static final Type<AdminDataResponsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "admin_data_response"));

    public static final StreamCodec<FriendlyByteBuf, AdminDataResponsePayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, AdminDataResponsePayload packet) {
            buf.writeUtf(packet.dataType);
            buf.writeUtf(packet.data);
        }

        @Override
        public @NotNull AdminDataResponsePayload decode(FriendlyByteBuf buf) {
            return new AdminDataResponsePayload(buf.readUtf(), buf.readUtf());
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdminDataResponsePayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            AdminScreen.cachedData.put(packet.dataType, packet.data);
        });
    }
}
