package com.mceteams.xiidays;

import com.mceteams.xiidays.blocks.BlockEntityRegistry;
import com.mceteams.xiidays.blocks.BlockRegistry;
import com.mceteams.xiidays.commands.CommandRegistry;
import com.mceteams.xiidays.items.ItemRegistry;
import com.mceteams.xiidays.menus.MenuRegistry;
import com.mceteams.xiidays.utils.PlayersHandler;
import com.mceteams.xiidays.utils.RestrictionsManager;
import com.mceteams.xiidays.utils.SpectateManager;
import com.mceteams.xiidays.utils.TaskScheduler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(XIIDaysManagerMod.MODID)
public class XIIDaysManagerMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "xiidays";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public XIIDaysManagerMod(IEventBus modEventBus, ModContainer modContainer) {

        LOGGER.info(  "\n╔═══════════════════════════════════════════════╗\n"+
                        "║                                               ║\n"+
                        "║   Welcome to XII Days - Mod                   ║\n"+
                        "║   Developing by FSS, MCE - fss.mceteams.com   ║\n"+
                        "║                                               ║\n"+
                        "║   Version " + modContainer.getModInfo().getVersion() + "                              ║\n"+
                        "║                                               ║\n"+
                        "╚═══════════════════════════════════════════════╝");

        LOGGER.info("[XII Days - Mod]: Registering mod components...");
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("[XII Days - Mod]: Registering Items, Blocks, Block Entities & Menus...");
        ItemRegistry.ITEMS.register(modEventBus);
        MenuRegistry.MENUS.register(modEventBus);
        BlockRegistry.BLOCKS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);

        LOGGER.info("[XII Days - Mod]: Registering Events listeners & senders...");
        NeoForge.EVENT_BUS.register(RestrictionsManager.class);
        NeoForge.EVENT_BUS.register(SpectateManager.class);
        NeoForge.EVENT_BUS.register(TaskScheduler.class);
        NeoForge.EVENT_BUS.register(new PlayersHandler());
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        LOGGER.info("[XII Days - Mod]: Registering mod Configuration & Specifications...");
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("[XII Days - Mod]: DONE, Mod components registration complete.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("[XII Days - Mod]: Common setup beginning...");

        LOGGER.info("[XII Days - Mod]: Initializing default cinematics...");

        LOGGER.info("[XII Days - Mod]: Common setup complete.");
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("[XII Days - Mod]: The server in ready, let's play some XII Days!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("[XII Days - Mod]: Registering XII Days commands...");
        CommandRegistry.register(event.getDispatcher());
        LOGGER.info("[XII Days - Mod]: XII Days commands registered.");
    }

    private void onServerTick(ServerTickEvent.Post event) {

    }
}
