package com.mceteams.xiidays;

import com.mceteams.xiidays.blocks.BlockEntityRegistry;
import com.mceteams.xiidays.blocks.BlockRegistry;
import com.mceteams.xiidays.client.KeyBindings;
import com.mceteams.xiidays.network.OpenScoreboardPacket;
import com.mceteams.xiidays.render.TeamSpawnerRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@Mod(value = MODID, dist = Dist.CLIENT)
public class XIIDaysManagerModClient {

    public XIIDaysManagerModClient(ModContainer container, IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerClientPackets);
        modEventBus.addListener(this::onRegisterKeyMappings);
    }

    @SubscribeEvent
    private void onClientSetup(FMLClientSetupEvent event) {
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Client setup started");
        XIIDaysManagerMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // Enregistrement des renderers de Block Entities
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Registering Block Entity Renderers");
        BlockEntityRenderers.register(BlockEntityRegistry.TEAM_SPAWN.get(), TeamSpawnerRenderer::new);
        XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Block Entity Renderers registered");

        // Configuration des render layers
        event.enqueueWork(() -> {
            XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Configuring render layers");
            ItemBlockRenderTypes.setRenderLayer(BlockRegistry.TEAM_SPAWNER.get(), RenderType.translucent());
            XIIDaysManagerMod.LOGGER.info("[XII Days - Mod]: Render layers configured");
        });
    }

    /**
     * Enregistre les packets SERVEUR → CLIENT
     */
    @SubscribeEvent
    private void registerClientPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();

        XIIDaysManagerMod.LOGGER.info("[XII Days - Client]: Registering client-bound packets...");

        // Serveur → Client : Données du scoreboard + ouverture de l'écran
        registrar.playToClient(
                OpenScoreboardPacket.TYPE,
                OpenScoreboardPacket.CODEC,
                OpenScoreboardPacket::handle
        );

        XIIDaysManagerMod.LOGGER.info("[XII Days - Client]: Client-bound packets registered (1 channel)");
    }

    /**
     * Enregistre la touche U pour ouvrir le scoreboard
     */
    @SubscribeEvent
    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        XIIDaysManagerMod.LOGGER.info("[XII Days - Client]: Registering key bindings...");
        event.register(KeyBindings.OPEN_SCOREBOARD);
        XIIDaysManagerMod.LOGGER.info("[XII Days - Client]: Key bindings registered (1 key)");
    }
}