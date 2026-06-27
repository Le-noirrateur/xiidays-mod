package com.mceteams.xiidays;

import com.mceteams.xiidays.visual.KeyBindings;
import com.mceteams.xiidays.visual.TeamSpawnerRenderer;
import com.mceteams.xiidays.world.BlockEntityRegistry;
import com.mceteams.xiidays.world.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import static com.mceteams.xiidays.XIIDays.MODID;

@Mod(value = MODID, dist = Dist.CLIENT)
public class XIIDaysClient {

    public XIIDaysClient(ModContainer container, IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeyMappings);
    }

    @SubscribeEvent
    private void onClientSetup(FMLClientSetupEvent event) {
        XIIDays.LOGGER.info("[XII Days - Mod]: Client setup started");
        XIIDays.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        BlockEntityRenderers.register(BlockEntityRegistry.TEAM_SPAWN.get(), TeamSpawnerRenderer::new);
        XIIDays.LOGGER.info("[XII Days - Mod]: Block Entity Renderers registered");

        event.enqueueWork(() -> {
            XIIDays.LOGGER.info("[XII Days - Mod]: Configuring render layers");
            ItemBlockRenderTypes.setRenderLayer(BlockRegistry.TEAM_SPAWNER.get(), ChunkSectionLayer.TRANSLUCENT);
            XIIDays.LOGGER.info("[XII Days - Mod]: Render layers configured");
        });
    }

    @SubscribeEvent
    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        XIIDays.LOGGER.info("[XII Days - Client]: Registering key bindings...");
        event.register(KeyBindings.OPEN_SCOREBOARD);
        event.register(KeyBindings.OPEN_ADMIN_MENU);
        XIIDays.LOGGER.info("[XII Days - Client]: Key bindings registered (2 keys)");
    }
}