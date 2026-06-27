package com.mceteams.xiidays.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.mceteams.xiidays.XIIDays.MODID;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoreBlockEntity>> TEAM_CORE =
            BLOCK_ENTITIES.register("team_core",
                    () -> new BlockEntityType<>(
                            CoreBlockEntity::new,
                            BlockRegistry.TEAM_CORE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpawnerBlockEntity>> TEAM_SPAWN =
            BLOCK_ENTITIES.register("team_spawn",
                    () -> new BlockEntityType<>(
                            SpawnerBlockEntity::new,
                            BlockRegistry.TEAM_SPAWNER.get()
                    ));
}
