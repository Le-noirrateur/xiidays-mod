package com.mceteams.xiidays.visual;

import com.mceteams.xiidays.network.PacketHandler;
import com.mceteams.xiidays.network.RequestOpenAdminMenuPacket;
import com.mceteams.xiidays.network.RequestScoreboardPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        if (KeyBindings.OPEN_SCOREBOARD.consumeClick()) {
            PacketHandler.sendToServer(new RequestScoreboardPacket());
        }

        if (KeyBindings.OPEN_ADMIN_MENU.consumeClick()) {
            PacketDistributor.sendToServer(new RequestOpenAdminMenuPacket());
        }
    }
}
