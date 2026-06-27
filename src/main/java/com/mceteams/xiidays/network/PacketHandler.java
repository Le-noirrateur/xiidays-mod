package com.mceteams.xiidays.network;

import com.mceteams.xiidays.spectator.SpectatePackets;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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

        // Play payloads bidirectionnels
        // Les playToClient sont enregistrés dans ClientPayloadRegistrar (client) et ServerPayloadRegistrar (serveur)

        // Client -> Serveur
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
    }

    public static void sendToServer(RequestScoreboardPacket packet) {
        ClientPacketDistributor.sendToServer(packet);
    }

    public static void sendToClient(OpenScoreboardPacket packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
