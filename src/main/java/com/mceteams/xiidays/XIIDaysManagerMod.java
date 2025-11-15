package com.mceteams.xiidays;

import com.mceteams.xiidays.blocks.BlockEntityRegistry;
import com.mceteams.xiidays.blocks.BlockRegistry;
import com.mceteams.xiidays.commands.CommandRegistry;
import com.mceteams.xiidays.items.ItemRegistry;
import com.mceteams.xiidays.menus.MenuRegistry;
import com.mceteams.xiidays.network.ScoreboardPackets;
import com.mceteams.xiidays.utils.*;
import com.mceteams.xiidays.utils.spectate.SpectatePackets;
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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

        // Dans la méthode XIIDaysManagerMod() du constructeur, ajoute cette ligne :
        LOGGER.info("[XII Days - Mod]: Registering Events listeners & senders...");
        NeoForge.EVENT_BUS.register(RestrictionsManager.class);
        NeoForge.EVENT_BUS.register(SpectateManager.class);
        NeoForge.EVENT_BUS.register(TaskScheduler.class);
        NeoForge.EVENT_BUS.register(new PlayersHandler());
        NeoForge.EVENT_BUS.register(ScoreboardManager.class); // ← AJOUTE CETTE LIGNE
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        LOGGER.info("[XII Days - Mod]: Registering mod Configuration & Specifications...");
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("[XII Days - Mod]: Registering network packets...");
        modEventBus.addListener(this::registerPackets);

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
        LOGGER.info("[XII Days - Mod]: The server is ready, let's play some XII Days!");

        LOGGER.info("[XII Days - Mod]: Initializing team points for leaderboard...");
        PointsManager.initializeTeamPoints();
        LOGGER.info("[XII Days - Mod]: Team points initialized");
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                SpectatePackets.SpectateSwitchPayload.TYPE,
                SpectatePackets.SpectateSwitchPayload.CODEC,
                SpectatePackets.SpectateSwitchPayload::handle
        );

        registrar.playToServer(
                ScoreboardPackets.RequestScoreboardPayload.TYPE,
                ScoreboardPackets.RequestScoreboardPayload.CODEC,
                ScoreboardPackets.RequestScoreboardPayload::handle
        );

        registrar.playToClient(
                ScoreboardPackets.ScoreboardDataPayload.TYPE,
                ScoreboardPackets.ScoreboardDataPayload.CODEC,
                ScoreboardPackets.ScoreboardDataPayload::handle
        );

        LOGGER.info("[XII Days - Mod]: Network packets registered");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("[XII Days - Mod]: Registering XII Days commands...");
        CommandRegistry.register(event.getDispatcher());
        LOGGER.info("[XII Days - Mod]: XII Days commands registered.");
    }
}
