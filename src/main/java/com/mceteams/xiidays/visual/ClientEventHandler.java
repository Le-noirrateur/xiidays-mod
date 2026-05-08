package com.mceteams.xiidays.visual;

import com.mceteams.xiidays.network.PacketHandler;
import com.mceteams.xiidays.network.RequestScoreboardPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

import static com.mceteams.xiidays.XIIDays.MODID;

/**
 * Gère les événements d'input côté client (touche U)
 * L'enregistrement de la touche se fait maintenant dans XIIDaysClient
 */
@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    /**
     * Détecte quand le joueur appuie sur la touche U
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        // Vérifier que le joueur est en jeu et que la touche est pressée
        if (mc.player != null && KeyBindings.OPEN_SCOREBOARD.consumeClick()) {
            // Envoyer une demande au serveur
            PacketHandler.sendToServer(new RequestScoreboardPacket());
        }
    }
}
