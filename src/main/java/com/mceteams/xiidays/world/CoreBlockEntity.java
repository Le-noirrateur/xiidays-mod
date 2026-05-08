package com.mceteams.xiidays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TeamId", teamId);
        tag.putBoolean("PuzzleSolved", puzzleSolved);
        tag.putInt("PuzzleStatus", puzzleStatus);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        teamId = tag.getInt("TeamId");
        puzzleSolved = tag.getBoolean("PuzzleSolved");
        puzzleStatus = tag.getInt("PuzzleStatus");
    }
}
