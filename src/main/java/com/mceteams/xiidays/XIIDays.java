package com.mceteams.xiidays;

import com.mceteams.xiidays.commands.CommandRegistry;
import com.mceteams.xiidays.config.Config;
import com.mceteams.xiidays.game.PointsManager;
import com.mceteams.xiidays.game.ScoreboardManager;
import com.mceteams.xiidays.game.TaskScheduler;
import com.mceteams.xiidays.item.ItemRegistry;
import com.mceteams.xiidays.player.PlayersHandler;
import com.mceteams.xiidays.restriction.RestrictionsManager;
import com.mceteams.xiidays.screen.MenuRegistry;
import com.mceteams.xiidays.spectator.SpectateManager;
import com.mceteams.xiidays.visual.ZoneVisualizer;
import com.mceteams.xiidays.world.BlockEntityRegistry;
import com.mceteams.xiidays.world.BlockRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
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
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(XIIDays.MODID)
public class XIIDays {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "xiidays";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public XIIDays(IEventBus modEventBus, ModContainer modContainer) {

        LOGGER.info("\n╔═══════════════════════════════════════════════╗\n║                                               ║\n║   Welcome to XII Days - Mod                   ║\n║   Developing by FSS, MCE - fss.mceteams.com   ║\n║                                               ║\n║   Version {}                              ║\n║                                               ║\n╚═══════════════════════════════════════════════╝", modContainer.getModInfo().getVersion());

        LOGGER.info("[XII Days - Mod]: Registering mod components...");
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("[XII Days - Mod]: Registering Items, Blocks, Block Entities & Menus...");
        ItemRegistry.ITEMS.register(modEventBus);
        MenuRegistry.MENUS.register(modEventBus);
        BlockRegistry.BLOCKS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);

        // Dans la méthode XIIDays() du constructeur, ajoute cette ligne :
        LOGGER.info("[XII Days - Mod]: Registering Events listeners & senders...");
        NeoForge.EVENT_BUS.register(RestrictionsManager.class);
        NeoForge.EVENT_BUS.register(SpectateManager.class);
        NeoForge.EVENT_BUS.register(ZoneVisualizer.class);
        NeoForge.EVENT_BUS.register(TaskScheduler.class);
        NeoForge.EVENT_BUS.register(new PlayersHandler());
        NeoForge.EVENT_BUS.register(ScoreboardManager.class); // ← AJOUTE CETTE LIGNE
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        LOGGER.info("[XII Days - Mod]: Registering mod Configuration & Specifications...");
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("[XII Days - Mod]: DONE, Mod components registration complete.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[XII Days - Mod]: Common setup complete.");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ItemRegistry.TOTEM_REVIVALITE.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ItemRegistry.CORE_DESTROYER.get());
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[XII Days - Mod]: The server is ready, let's play some XII Days!");

        LOGGER.info("[XII Days - Mod]: Initializing team points for leaderboard...");
        PointsManager.initializeTeamPoints();
        LOGGER.info("[XII Days - Mod]: Team points initialized");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("[XII Days - Mod]: Registering XII Days commands...");
        CommandRegistry.register(event.getDispatcher());
        LOGGER.info("[XII Days - Mod]: XII Days commands registered.");
    }
}
