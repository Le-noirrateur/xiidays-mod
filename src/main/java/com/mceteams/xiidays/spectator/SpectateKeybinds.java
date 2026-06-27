package com.mceteams.xiidays.spectator;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class SpectateKeybinds {

    public static KeyMapping SPECTATE_NEXT;
    public static KeyMapping SPECTATE_PREVIOUS;

    private static final KeyMapping.Category CATEGORY_OBJ = new KeyMapping.Category(Identifier.fromNamespaceAndPath(MODID, "xiidays"));

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        SPECTATE_NEXT = new KeyMapping(
                "key." + MODID + ".spectate_next",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT, // Flèche droite
                CATEGORY_OBJ
        );

        SPECTATE_PREVIOUS = new KeyMapping(
                "key." + MODID + ".spectate_previous",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT, // Flèche gauche
                CATEGORY_OBJ
        );

        event.register(SPECTATE_NEXT);
        event.register(SPECTATE_PREVIOUS);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (SPECTATE_NEXT.consumeClick()) {
            ClientPacketDistributor.sendToServer(new SpectatePackets.SpectateSwitchPayload(true));
        }

        while (SPECTATE_PREVIOUS.consumeClick()) {
            ClientPacketDistributor.sendToServer(new SpectatePackets.SpectateSwitchPayload(false));
        }
    }
}
