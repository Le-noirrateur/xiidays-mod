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
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@SuppressWarnings("deprecation")
@Mod(value = XIIDaysManagerMod.MODID, dist = Dist.CLIENT)
public class XIIDaysManagerModClient {
    public XIIDaysManagerModClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(ClientEvents::registerKeys);
    }
    private void onClientSetup(FMLClientSetupEvent event) {
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Client setup started");
        XIIDaysManagerMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Registering Block Entity Renderers");
        BlockEntityRenderers.register(BlockEntityRegistry.TEAM_SPAWN.get(), TeamSpawnerRenderer::new);
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Block Entity Renderers registered");

        event.enqueueWork(() -> {
            XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Configuring render layers");
            ItemBlockRenderTypes.setRenderLayer(BlockRegistry.TEAM_SPAWNER.get(), RenderType.translucent());
            XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Render layers configured");
        });
    }
}