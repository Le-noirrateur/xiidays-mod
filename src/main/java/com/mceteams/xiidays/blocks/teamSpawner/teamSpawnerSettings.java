package com.mceteams.xiidays.blocks.teamSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class teamSpawnerSettings extends Block implements EntityBlock {

    public teamSpawnerSettings(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new teamSpawner(blockPos, blockState);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE; // 👈 rend le modèle du block.json invisible
    }


    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, net.minecraft.world.level.@NotNull BlockGetter reader, @NotNull BlockPos pos) {
        return true; // Permet à la lumière de traverser
    }

    @Override
    public float getShadeBrightness(@NotNull BlockState state, net.minecraft.world.level.@NotNull BlockGetter worldIn, @NotNull BlockPos pos) {
        return 1.0F;
    }
}