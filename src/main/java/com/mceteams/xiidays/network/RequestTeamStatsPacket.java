package com.mceteams.xiidays.network;

import com.mceteams.xiidays.utils.DataManager;
import com.mceteams.xiidays.utils.TeamManager;
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

/**
 * Packet envoyé du CLIENT vers le SERVEUR
 * Demande les stats détaillées d'une équipe
 */
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
            int teamId = TeamManager.getTeamId(teamName);

            if (teamId == 0) return;

            // Récupérer les stats de l'équipe
            int teamPoints = DataManager.dataReadInt("team_" + teamId + "_points", "total", 0);
            int teamKills = DataManager.dataReadInt("team_" + teamId + "_stats", "kills", 0);
            int teamDeaths = DataManager.dataReadInt("team_" + teamId + "_stats", "deaths", 0);
            int teamBlocksMined = DataManager.dataReadInt("team_" + teamId + "_stats", "blocks_mined", 0);
            int teamDamageDealt = DataManager.dataReadInt("team_" + teamId + "_stats", "damage_dealt", 0);
            int teamDamageReceived = DataManager.dataReadInt("team_" + teamId + "_stats", "damage_received", 0);
            int teamPointGain = DataManager.dataReadInt("team_" + teamId + "_stats", "pointgain", 0);
            int teamPointLoss = DataManager.dataReadInt("team_" + teamId + "_stats", "pointloss", 0);

            // Récupérer les stats des joueurs de l'équipe
            List<OpenTeamStatsPacket.PlayerStatsData> playerStatsList = new ArrayList<>();
            String[] memberKeys = DataManager.getAllDataNames("team_" + teamId + "_members");

            for (String memberKey : memberKeys) {
                String playerUUID = DataManager.dataRead("team_" + teamId + "_members", memberKey);
                if (playerUUID == null || playerUUID.isEmpty()) continue;

                // Récupérer le nom du joueur
                ServerPlayer memberPlayer = player.server.getPlayerList().getPlayer(UUID.fromString(playerUUID));
                String playerName = memberPlayer != null ? memberPlayer.getName().getString() : "Joueur hors ligne";

                int pKills = DataManager.dataReadInt("player_" + playerUUID + "_stats", "kills", 0);
                int pDeaths = DataManager.dataReadInt("player_" + playerUUID + "_stats", "deaths", 0);
                int pPoints = DataManager.dataReadInt("player_" + playerUUID + "_stats", "team_points", 0);
                int pBlocksMined = DataManager.dataReadInt("player_" + playerUUID + "_stats", "blocks_mined", 0);
                int pDamageDealt = DataManager.dataReadInt("player_" + playerUUID + "_stats", "damage_dealt", 0);

                playerStatsList.add(new OpenTeamStatsPacket.PlayerStatsData(
                        playerName, playerUUID, pKills, pDeaths, pPoints, pBlocksMined, pDamageDealt
                ));
            }

            // Envoyer le packet de réponse
            OpenTeamStatsPacket.TeamStatsData teamStats = new OpenTeamStatsPacket.TeamStatsData(
                    teamName, teamPoints, teamKills, teamDeaths,
                    teamBlocksMined, teamDamageDealt, teamDamageReceived,
                    teamPointGain, teamPointLoss
            );

            PacketDistributor.sendToPlayer(player, new OpenTeamStatsPacket(teamStats, playerStatsList));
        });
    }
}
