package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mceteams.xiidays.XIIDays.MODID;

public record DayEndScorePayload(List<TeamEntry> teams) implements CustomPacketPayload {

    public record TeamEntry(String teamName, int points, int rankChange) {}

    public static final CustomPacketPayload.Type<DayEndScorePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "day_end_score"));

    public static final StreamCodec<FriendlyByteBuf, DayEndScorePayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, DayEndScorePayload packet) {
            buf.writeInt(packet.teams.size());
            for (TeamEntry t : packet.teams) {
                buf.writeUtf(t.teamName);
                buf.writeInt(t.points);
                buf.writeInt(t.rankChange);
            }
        }

        @Override
        public @NotNull DayEndScorePayload decode(FriendlyByteBuf buf) {
            int size = buf.readInt();
            List<TeamEntry> teams = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                teams.add(new TeamEntry(buf.readUtf(), buf.readInt(), buf.readInt()));
            }
            return new DayEndScorePayload(teams);
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
