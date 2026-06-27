package com.mceteams.xiidays.client;

import com.mceteams.xiidays.network.PointsPopupClientHandler;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(MODID, "points_popup"),
                PointsPopupClientHandler::render
        );
    }
}
