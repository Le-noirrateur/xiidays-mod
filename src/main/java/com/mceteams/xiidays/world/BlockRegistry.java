package com.mceteams.xiidays.world;

import net.minecraft.core.registries.Registries;
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
            () -> new CoreBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()                                          // Pas d'occlusion
                    .noLootTable()                                          // Pas de table de loot
                    .sound(SoundType.STONE)                                 // Son de pierre
                    .mapColor(MapColor.STONE)                               // Couleur de la carte
                    .pushReaction(PushReaction.IGNORE)                      // Ignore les pistons
                    .strength(20.0F, 1200.0F)    // Dureté du bloc
            ));

    public static final DeferredHolder<Block, SpawnerBlock> TEAM_SPAWNER = BLOCKS.register("team_spawner",
            () -> new SpawnerBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()                                                                                // Pas d'occlusion (transparent)
                    .noLootTable()                                                                                // Pas de table de loot
                    .noCollission()                                                                               // Pas de collision
                    .noTerrainParticles()                                                                         // Pas de particules de terrain
                    .sound(SoundType.STONE)                                                                       // Son de pierre
                    .mapColor(MapColor.STONE)                                                                     // Couleur de la carte
                    .explosionResistance(1200.0f)                                                                 // Résistance aux explosions
                    .pushReaction(PushReaction.IGNORE)                                                            // Ignore les pistons
                    .strength(20.0F, 1200.0F) // Dureté du bloc
                    .isValidSpawn((state, level, pos, type) -> false)  // Empêche spawn de mobs
                    .isRedstoneConductor((state, level, pos) -> false)              // Pas de signal redstone
                    .isSuffocating((state, level, pos) -> false)                    // Pas de suffocation
                    .isViewBlocking((state, level, pos) -> false)                   // Pas de blocage de vue
            ));
}
