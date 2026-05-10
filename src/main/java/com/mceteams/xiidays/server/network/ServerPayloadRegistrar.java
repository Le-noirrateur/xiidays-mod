package com.mceteams.xiidays.server.network;

import com.mceteams.xiidays.network.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID, value = net.neoforged.api.distmarker.Dist.DEDICATED_SERVER)
public class ServerPayloadRegistrar {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToClient(
                PointsPopupPayload.TYPE,
                PointsPopupPayload.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                OpenScoreboardPacket.TYPE,
                OpenScoreboardPacket.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                CoreMazeOpenPacket.TYPE,
                CoreMazeOpenPacket.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                OpenTeamStatsPacket.TYPE,
                OpenTeamStatsPacket.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                OpenEndScoreboardPacket.TYPE,
                OpenEndScoreboardPacket.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                OpenAdminMenuPacket.TYPE,
                OpenAdminMenuPacket.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                AdminDataResponsePayload.TYPE,
                AdminDataResponsePayload.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                DayNotificationPayload.TYPE,
                DayNotificationPayload.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                DeathNotificationPayload.TYPE,
                DeathNotificationPayload.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                SpectatorStatusPayload.TYPE,
                SpectatorStatusPayload.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                EliminationNotificationPayload.TYPE,
                EliminationNotificationPayload.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                GameOverPayload.TYPE,
                GameOverPayload.CODEC,
                (p, ctx) -> {}
        );

        registrar.playToClient(
                DayEndScorePayload.TYPE,
                DayEndScorePayload.CODEC,
                (p, ctx) -> {}
        );
    }
}
