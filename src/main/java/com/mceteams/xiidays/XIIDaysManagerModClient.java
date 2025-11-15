package com.mceteams.xiidays;

import com.mceteams.xiidays.blocks.BlockEntityRegistry;
import com.mceteams.xiidays.blocks.BlockRegistry;
import com.mceteams.xiidays.client.ClientEvents;
import com.mceteams.xiidays.render.TeamSpawnerRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = XIIDaysManagerMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = XIIDaysManagerMod.MODID, value = Dist.CLIENT)
public class XIIDaysManagerModClient {
    public XIIDaysManagerModClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // Enregistrer les événements client (pour les touches)
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Registering client events...");
        NeoForge.EVENT_BUS.register(ClientEvents.class);

        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Client events registered");
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Registering keybindings");
        ClientEvents.registerKeys(event);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Client setup started");
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // Enregistrement des renderers de Block Entities
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Registering Block Entity Renderers");
        BlockEntityRenderers.register(BlockEntityRegistry.TEAM_SPAWN.get(), TeamSpawnerRenderer::new);
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Block Entity Renderers registered");

        // Configuration des render layers
        event.enqueueWork(() -> {
            XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Configuring render layers");

            // Team Spawner avec transparence
            ItemBlockRenderTypes.setRenderLayer(BlockRegistry.TEAM_SPAWNER.get(), RenderType.translucent());

            // Team Core (si besoin de transparence aussi)
            // ItemBlockRenderTypes.setRenderLayer(BlockRegistry.TEAM_CORE.get(), RenderType.translucent());

            XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Render layers configured");
        });
    }
}