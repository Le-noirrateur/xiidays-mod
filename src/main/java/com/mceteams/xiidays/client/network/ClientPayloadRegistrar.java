package com.mceteams.xiidays.client.network;

import com.mceteams.xiidays.network.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientPayloadRegistrar {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToClient(
                PointsPopupPayload.TYPE,
                PointsPopupPayload.CODEC,
                ClientPacketHandlers::handlePointsPopup
        );

        registrar.playToClient(
                OpenScoreboardPacket.TYPE,
                OpenScoreboardPacket.CODEC,
                ClientPacketHandlers::handleOpenScoreboard
        );

        registrar.playToClient(
                CoreMazeOpenPacket.TYPE,
                CoreMazeOpenPacket.CODEC,
                ClientPacketHandlers::handleCoreMazeOpen
        );

        registrar.playToClient(
                OpenTeamStatsPacket.TYPE,
                OpenTeamStatsPacket.CODEC,
                ClientPacketHandlers::handleOpenTeamStats
        );

        registrar.playToClient(
                OpenEndScoreboardPacket.TYPE,
                OpenEndScoreboardPacket.CODEC,
                ClientPacketHandlers::handleOpenEndScoreboard
        );

        registrar.playToClient(
                OpenAdminMenuPacket.TYPE,
                OpenAdminMenuPacket.CODEC,
                ClientPacketHandlers::handleOpenAdminMenu
        );

        registrar.playToClient(
                AdminDataResponsePayload.TYPE,
                AdminDataResponsePayload.CODEC,
                ClientPacketHandlers::handleAdminDataResponse
        );

        registrar.playToClient(
                DayNotificationPayload.TYPE,
                DayNotificationPayload.CODEC,
                ClientPacketHandlers::handleDayNotification
        );

        registrar.playToClient(
                DeathNotificationPayload.TYPE,
                DeathNotificationPayload.CODEC,
                ClientPacketHandlers::handleDeathNotification
        );

        registrar.playToClient(
                SpectatorStatusPayload.TYPE,
                SpectatorStatusPayload.CODEC,
                ClientPacketHandlers::handleSpectatorStatus
        );

        registrar.playToClient(
                EliminationNotificationPayload.TYPE,
                EliminationNotificationPayload.CODEC,
                ClientPacketHandlers::handleEliminationNotification
        );

        registrar.playToClient(
                GameOverPayload.TYPE,
                GameOverPayload.CODEC,
                ClientPacketHandlers::handleGameOver
        );

        registrar.playToClient(
                DayEndScorePayload.TYPE,
                DayEndScorePayload.CODEC,
                ClientPacketHandlers::handleDayEndScore
        );
    }
}
