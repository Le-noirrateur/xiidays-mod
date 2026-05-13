package com.mceteams.xiidays.network;

import com.mceteams.xiidays.data.PlayerStatsData;
import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.data.TeamStatsData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mceteams.xiidays.XIIDays.MODID;

public record RequestTeamStatsPacket(String teamName) implements CustomPacketPayload {

    public static final Type<RequestTeamStatsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "request_team_stats"));

    public static final StreamCodec<FriendlyByteBuf, RequestTeamStatsPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, RequestTeamStatsPacket packet) {
            buf.writeUtf(packet.teamName);
        }

        @Override
        public @NotNull RequestTeamStatsPacket decode(FriendlyByteBuf buf) {
            return new RequestTeamStatsPacket(buf.readUtf());
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestTeamStatsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            String teamName = packet.teamName;
            int teamId = TeamData.getTeamId(teamName);

            if (teamId == 0) return;

            int teamPoints = TeamStatsData.getPoints(teamId);
            int teamKills = TeamStatsData.getKills(teamId);
            int teamDeaths = 0;
            int teamBlocksMined = TeamStatsData.getBlocksMined(teamId);
            int teamDamageDealt = TeamStatsData.getDamageDealt(teamId);
            int teamDamageReceived = TeamStatsData.getDamageReceived(teamId);
            int teamPointGain = TeamStatsData.getPointGain(teamId);
            int teamPointLoss = TeamStatsData.getPointLoss(teamId);

            List<OpenTeamStatsPacket.PlayerStatsData> playerStatsList = new ArrayList<>();
            var members = TeamData.getMembers(teamId);

            for (int i = 0; i < members.size(); i++) {
                String playerUUID = members.get(i).getAsString();
                if (playerUUID == null || playerUUID.isEmpty()) continue;

                ServerPlayer memberPlayer = player.server.getPlayerList().getPlayer(UUID.fromString(playerUUID));
                String playerName = memberPlayer != null ? memberPlayer.getName().getString() : "Joueur hors ligne";

                int pKills = PlayerStatsData.getKills(playerUUID);
                int pDeaths = PlayerStatsData.getDeaths(playerUUID);
                int pPoints = PlayerStatsData.getTeamPoints(playerUUID);
                int pBlocksMined = PlayerStatsData.getBlocksMined(playerUUID);
                int pDamageDealt = PlayerStatsData.getDamageDealt(playerUUID);

                playerStatsList.add(new OpenTeamStatsPacket.PlayerStatsData(
                        playerName, playerUUID, pKills, pDeaths, pPoints, pBlocksMined, pDamageDealt
                ));
            }

            OpenTeamStatsPacket.TeamStatsData teamStats = new OpenTeamStatsPacket.TeamStatsData(
                    teamName, teamPoints, teamKills, teamDeaths,
                    teamBlocksMined, teamDamageDealt, teamDamageReceived,
                    teamPointGain, teamPointLoss
            );

            PacketDistributor.sendToPlayer(player, new OpenTeamStatsPacket(teamStats, playerStatsList));
        });
    }
}
