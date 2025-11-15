package com.mceteams.xiidays.client;

import com.mceteams.xiidays.network.ScoreboardPackets;
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
public class ClientEvents {

    public static final String CATEGORY = "key.categories." + MODID;

    public static KeyMapping OPEN_SCOREBOARD;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN_SCOREBOARD = new KeyMapping(
                "key." + MODID + ".open_scoreboard",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_TAB,
                CATEGORY
        );

        event.register(OPEN_SCOREBOARD);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_SCOREBOARD.consumeClick()) {
            // Demander les données au serveur
            PacketDistributor.sendToServer(new ScoreboardPackets.RequestScoreboardPayload());
        }
    }
}