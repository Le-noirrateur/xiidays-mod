package com.mceteams.xiidays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CoreBlockEntity extends BlockEntity {
    private int teamId = 0;
    private int puzzleStatus = 0;
    private boolean puzzleSolved = false;


    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.TEAM_CORE.get(), pos, state);
    }

    // Getters et Setters
    public void setTeamId(int teamId) {
        this.teamId = teamId;
        setChanged();
    }

    public int getTeamId() {
        return teamId;
    }

    public int getPuzzleStatus() {
        return puzzleStatus;
    }

    public boolean isPuzzleSolved() {
        return puzzleSolved;
    }

    public void setPuzzleSolved(boolean puzzleSolved) {
        this.puzzleSolved = puzzleSolved;
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TeamId", teamId);
        output.putBoolean("PuzzleSolved", puzzleSolved);
        output.putInt("PuzzleStatus", puzzleStatus);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        teamId = input.getIntOr("TeamId", 0);
        puzzleSolved = input.getBooleanOr("PuzzleSolved", false);
        puzzleStatus = input.getIntOr("PuzzleStatus", 0);
    }
}
