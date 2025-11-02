package com.mceteams.xiidays.blocks;

import com.mceteams.xiidays.blocks.teamCore.teamCoreBlock;
import com.mceteams.xiidays.blocks.teamSpawner.teamSpawnerSettings;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, MODID);

    public static final DeferredHolder<Block, teamCoreBlock> TEAM_CORE = BLOCKS.register("team_core",
            () -> new teamCoreBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.STONE)
                    .pushReaction(PushReaction.BLOCK)
                    .strength(20.0F, 1200.0F)
            ));

    public static final DeferredHolder<Block, teamSpawnerSettings> TEAM_SPAWNER = BLOCKS.register("team_spawner",
            () -> new teamSpawnerSettings(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()
                    .noTerrainParticles()
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.STONE)
                    .explosionResistance(1200.0f)
                    .pushReaction(PushReaction.IGNORE)
            ));
}
