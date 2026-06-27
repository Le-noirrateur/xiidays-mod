package com.mceteams.xiidays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SpawnerBlockEntity extends BlockEntity {
    private int teamId = 0;

    public SpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.TEAM_SPAWN.get(), pos, state);
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
        setChanged();
    }

    public int getTeamId() {
        return teamId;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TeamId", teamId);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        teamId = input.getIntOr("TeamId", 0);
    }
}
