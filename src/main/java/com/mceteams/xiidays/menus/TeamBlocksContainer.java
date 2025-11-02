package com.mceteams.xiidays.menus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TeamBlocksContainer extends AbstractContainerMenu {
    private final SimpleContainerData data;

    public TeamBlocksContainer(int containerId, Inventory playerInventory) {
        super(MenuRegistry.TEAM_CORE_MENU.get(), containerId);
        this.data = new SimpleContainerData(1);
        addDataSlots(this.data);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return false;
    }

    public void setPuzzleStep(int step) {
        data.set(0, step);
    }

    public int getPuzzleStep() {
        return data.get(0);
    }
}
