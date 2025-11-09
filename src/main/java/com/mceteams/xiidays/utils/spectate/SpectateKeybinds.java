package com.mceteams.xiidays.utils.spectate;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class SpectateKeybinds {

    public static final String CATEGORY = "key.categories." + MODID;

    public static KeyMapping SPECTATE_NEXT;
    public static KeyMapping SPECTATE_PREVIOUS;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        SPECTATE_NEXT = new KeyMapping(
                "key." + MODID + ".spectate_next",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT, // Flèche droite
                CATEGORY
        );

        SPECTATE_PREVIOUS = new KeyMapping(
                "key." + MODID + ".spectate_previous",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT, // Flèche gauche
                CATEGORY
        );

        event.register(SPECTATE_NEXT);
        event.register(SPECTATE_PREVIOUS);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (SPECTATE_NEXT.consumeClick()) {
            PacketDistributor.sendToServer(new SpectatePackets.SpectateSwitchPayload(true));
        }

        while (SPECTATE_PREVIOUS.consumeClick()) {
            PacketDistributor.sendToServer(new SpectatePackets.SpectateSwitchPayload(false));
        }
    }
}