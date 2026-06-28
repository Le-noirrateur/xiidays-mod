package com.mceteams.xiidays.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.mceteams.xiidays.XIIDays.MODID;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, MODID);

    public static final DeferredHolder<Block, CoreBlock> TEAM_CORE = BLOCKS.register("team_core",
            key -> new CoreBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.STONE)
                    .pushReaction(PushReaction.IGNORE)
                    .strength(20.0F, 1200.0F)
            ));

    public static final DeferredHolder<Block, SpawnerBlock> TEAM_SPAWNER = BLOCKS.register("team_spawner",
            key -> new SpawnerBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .noOcclusion()
                    .noLootTable()
                    .noCollision()
                    .noTerrainParticles()
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.STONE)
                    .explosionResistance(1200.0f)
                    .pushReaction(PushReaction.IGNORE)
                    .strength(20.0F, 1200.0F)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
            ));
}
