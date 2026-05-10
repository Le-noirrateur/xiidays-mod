package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mceteams.xiidays.XIIDays.MODID;

/**
 * Packet envoyé du SERVEUR vers le CLIENT
 * Ouvre l'écran Core Maze avec les énigmes
 */
public record CoreMazeOpenPacket(List<EnigmaPayload> enigmas, int teamId) implements CustomPacketPayload {

    public static final Type<CoreMazeOpenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "core_maze_open"));

    public static final StreamCodec<FriendlyByteBuf, CoreMazeOpenPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, CoreMazeOpenPacket packet) {
            buf.writeInt(packet.teamId);
            buf.writeInt(packet.enigmas.size());

            for (EnigmaPayload enigma : packet.enigmas) {
                buf.writeUtf(enigma.type);
                buf.writeUtf(enigma.question);
                buf.writeUtf(enigma.answer);
                buf.writeUtf(enigma.hint);
            }
        }

        @Override
        public @NotNull CoreMazeOpenPacket decode(FriendlyByteBuf buf) {
            int teamId = buf.readInt();
            int size = buf.readInt();
            List<EnigmaPayload> enigmas = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                enigmas.add(new EnigmaPayload(
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readUtf()
                ));
            }

            return new CoreMazeOpenPacket(enigmas, teamId);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record EnigmaPayload(String type, String question, String answer, String hint) {}
}
