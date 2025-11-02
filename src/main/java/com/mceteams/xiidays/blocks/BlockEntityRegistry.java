package com.mceteams.xiidays.blocks;

import com.mceteams.xiidays.blocks.teamCore.teamCore;
import com.mceteams.xiidays.blocks.teamSpawner.teamSpawner;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<teamCore>> TEAM_CORE =
            BLOCK_ENTITIES.register("team_core",
                    () -> BlockEntityType.Builder.of(
                            teamCore::new,
                            BlockRegistry.TEAM_CORE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<teamSpawner>> TEAM_SPAWN =
            BLOCK_ENTITIES.register("team_spawn",
                    () -> BlockEntityType.Builder.of(
                            teamSpawner::new,
                            BlockRegistry.TEAM_SPAWNER.get()
                    ).build(null));
}
