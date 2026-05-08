package com.mceteams.xiidays.item;

import com.mceteams.xiidays.world.BlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.mceteams.xiidays.XIIDays.MODID;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, MODID);

    public static final DeferredHolder<Item, BlockItem> TEAM_CORE_ITEM = ITEMS.register("team_core",
            () -> new BlockItem(BlockRegistry.TEAM_CORE.get(), new Item.Properties()
                    .fireResistant()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
            ));

    public static final DeferredHolder<Item, BlockItem> TEAM_SPAWNER_ITEM = ITEMS.register("team_spawner",
            () -> new BlockItem(BlockRegistry.TEAM_SPAWNER.get(), new Item.Properties()
                    .fireResistant()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
            ));

    public static final DeferredHolder<Item, CoreDestroyerItem> CORE_DESTROYER = ITEMS.register("core_destroyer",
                () -> new CoreDestroyerItem(new Item.Properties()
                        .stacksTo(1)
                        .durability(3)
                        .fireResistant()
                        .rarity(Rarity.RARE)
                ));

    public static final DeferredHolder<Item, TotemRevivalityItem> TOTEM_REVIVALITE = ITEMS.register("totem_revivalite",
            () -> new TotemRevivalityItem(new Item.Properties()
                    .stacksTo(16)
                    .fireResistant()
                    .rarity(Rarity.RARE)
            ));
}
