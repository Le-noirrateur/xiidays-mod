package com.mceteams.xiidays.network;

import com.mceteams.xiidays.spectator.SpectatePackets;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.mceteams.xiidays.XIIDays.MODID;

/**
 * Gestionnaire central des paquets réseau
 * UNIQUE POINT D'ENREGISTREMENT DES PAQUETS
 */
@EventBusSubscriber(modid = MODID)
public class PacketHandler {

    /**
     * Enregistre TOUS les paquets (client ET serveur)
     * Cette méthode s'exécute UNE SEULE FOIS sur CLIENT et SERVEUR
     */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        // Client -> Serveur
        registrar.playToClient(
                PointsPopupPayload.TYPE,
                PointsPopupPayload.CODEC,
                PointsPopupPayload::handle
        );

        registrar.playToServer(
                RequestScoreboardPacket.TYPE,
                RequestScoreboardPacket.CODEC,
                RequestScoreboardPacket::handle
        );

        registrar.playToServer(
                CoreMazeAnswerPacket.TYPE,
                CoreMazeAnswerPacket.CODEC,
                CoreMazeAnswerPacket::handle
        );

        registrar.playToServer(
                RequestTeamStatsPacket.TYPE,
                RequestTeamStatsPacket.CODEC,
                RequestTeamStatsPacket::handle
        );

        registrar.playToServer(
                SpectatePackets.SpectateSwitchPayload.TYPE,
                SpectatePackets.SpectateSwitchPayload.CODEC,
                SpectatePackets.SpectateSwitchPayload::handle
        );

        // Serveur -> Client
        registrar.playToClient(
                OpenScoreboardPacket.TYPE,
                OpenScoreboardPacket.CODEC,
                OpenScoreboardPacket::handle
        );

        registrar.playToClient(
                CoreMazeOpenPacket.TYPE,
                CoreMazeOpenPacket.CODEC,
                CoreMazeOpenPacket::handle
        );

        registrar.playToClient(
                OpenTeamStatsPacket.TYPE,
                OpenTeamStatsPacket.CODEC,
                OpenTeamStatsPacket::handle
        );

        registrar.playToClient(
                OpenEndScoreboardPacket.TYPE,
                OpenEndScoreboardPacket.CODEC,
                OpenEndScoreboardPacket::handle
        );

        registrar.playToClient(
                OpenAdminMenuPacket.TYPE,
                OpenAdminMenuPacket.CODEC,
                OpenAdminMenuPacket::handle
        );

        registrar.playToServer(
                AdminActionPayload.TYPE,
                AdminActionPayload.CODEC,
                AdminActionPayload::handle
        );

        registrar.playToServer(
                RequestAdminDataPayload.TYPE,
                RequestAdminDataPayload.CODEC,
                RequestAdminDataPayload::handle
        );

        registrar.playToServer(
                RequestOpenAdminMenuPacket.TYPE,
                RequestOpenAdminMenuPacket.CODEC,
                RequestOpenAdminMenuPacket::handle
        );

        registrar.playToClient(
                AdminDataResponsePayload.TYPE,
                AdminDataResponsePayload.CODEC,
                AdminDataResponsePayload::handle
        );

        registrar.playToClient(
                DayNotificationPayload.TYPE,
                DayNotificationPayload.CODEC,
                DayNotificationPayload::handle
        );

        registrar.playToClient(
                DeathNotificationPayload.TYPE,
                DeathNotificationPayload.CODEC,
                DeathNotificationPayload::handle
        );

        registrar.playToClient(
                SpectatorStatusPayload.TYPE,
                SpectatorStatusPayload.CODEC,
                SpectatorStatusPayload::handle
        );

        registrar.playToClient(
                EliminationNotificationPayload.TYPE,
                EliminationNotificationPayload.CODEC,
                EliminationNotificationPayload::handle
        );

        registrar.playToClient(
                GameOverPayload.TYPE,
                GameOverPayload.CODEC,
                GameOverPayload::handle
        );
    }

    public static void sendToServer(RequestScoreboardPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToClient(OpenScoreboardPacket packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
