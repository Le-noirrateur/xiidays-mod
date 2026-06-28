package com.mceteams.xiidays.client.network;

import com.mceteams.xiidays.network.*;
import com.mceteams.xiidays.screen.AdminScreen;
import com.mceteams.xiidays.screen.CoreMazeScreen;
import com.mceteams.xiidays.screen.TeamStatsScreen;
import com.mceteams.xiidays.visual.HUDOverlayHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class ClientPacketHandlers {

    public static void handleDayNotification(DayNotificationPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> HUDOverlayHandler.onDayNotification(packet));
    }

    public static void handleDeathNotification(DeathNotificationPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> HUDOverlayHandler.onDeathNotification(packet));
    }

    public static void handleSpectatorStatus(SpectatorStatusPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> HUDOverlayHandler.onSpectatorStatus(packet));
    }

    public static void handleEliminationNotification(EliminationNotificationPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> HUDOverlayHandler.onEliminationNotification(packet));
    }

    public static void handleGameOver(GameOverPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> HUDOverlayHandler.onGameOver(packet));
    }

    public static void handleDayEndScore(DayEndScorePayload packet, IPayloadContext context) {
        context.enqueueWork(() -> HUDOverlayHandler.onDayEndScore(packet));
    }

    public static void handleCoreMazeOpen(CoreMazeOpenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            List<CoreMazeScreen.EnigmaData> screenEnigmas = new ArrayList<>();
            for (CoreMazeOpenPacket.EnigmaPayload ep : packet.enigmas()) {
                screenEnigmas.add(new CoreMazeScreen.EnigmaData(ep.type(), ep.question(), ep.answer(), ep.hint()));
            }
            Minecraft.getInstance().setScreen(new CoreMazeScreen(screenEnigmas, packet.teamId()));
        });
    }

    public static void handleOpenAdminMenu(OpenAdminMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(new AdminScreen())
        );
    }

    public static void handleOpenTeamStats(OpenTeamStatsPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(new TeamStatsScreen(packet.teamStats(), packet.playerStats()))
        );
    }

    public static void handleAdminDataResponse(AdminDataResponsePayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            AdminScreen.cachedData.put(packet.dataType(), packet.data());
        });
    }

    public static void handlePointsPopup(PointsPopupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PointsPopupClientHandler.show(payload));
    }

    public static void handleOpenScoreboard(OpenScoreboardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientScoreboardHandler.openScoreboard(packet));
    }

    public static void handleOpenEndScoreboard(OpenEndScoreboardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientEndScoreboardHandler.openEndScoreboard(packet));
    }
}
