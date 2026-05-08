package com.mceteams.xiidays.screen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.mceteams.xiidays.XIIDays.MODID;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<TeamBlocksContainer>> TEAM_CORE_MENU =
            MENUS.register("team_blocks_menu",
                    () -> new MenuType<>(TeamBlocksContainer::new, FeatureFlags.DEFAULT_FLAGS));
}
