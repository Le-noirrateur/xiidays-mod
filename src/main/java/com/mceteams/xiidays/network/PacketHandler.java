package com.mceteams.xiidays.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

/**
 * Gestionnaire central des paquets réseau
 * Enregistre tous les paquets utilisés par le mod
 */
@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
public class PacketHandler {

    /**
     * Enregistre tous les paquets du mod
     */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        // Client -> Serveur : Demande d'ouverture du scoreboard
        registrar.playToServer(
                RequestScoreboardPacket.TYPE,
                RequestScoreboardPacket.CODEC,
                RequestScoreboardPacket::handle
        );

        // Serveur -> Client : Données du scoreboard
        registrar.playToClient(
                OpenScoreboardPacket.TYPE,
                OpenScoreboardPacket.CODEC,
                OpenScoreboardPacket::handle
        );
    }

    /**
     * Envoie un paquet au serveur (depuis le client)
     */
    public static void sendToServer(RequestScoreboardPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    /**
     * Envoie un paquet à un joueur spécifique (depuis le serveur)
     */
    public static void sendToClient(OpenScoreboardPacket packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}