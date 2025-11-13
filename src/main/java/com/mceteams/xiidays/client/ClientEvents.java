package com.mceteams.xiidays.client;

import com.mceteams.xiidays.menus.ScoreboardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = "xiidays", value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        // Vérifier si la touche du scoreboard est pressée
        if (KeyBindings.OPEN_SCOREBOARD.get().consumeClick()) {
            // Ouvrir l'écran du scoreboard
            mc.setScreen(new ScoreboardScreen(Component.literal("Classement des Équipes")));
        }
    }
}