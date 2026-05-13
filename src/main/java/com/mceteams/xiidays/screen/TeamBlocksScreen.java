package com.mceteams.xiidays.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class TeamBlocksScreen extends AbstractContainerScreen<TeamBlocksContainer> {

    public TeamBlocksScreen(TeamBlocksContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float v, int i, int i1) {
        int x = (width - 176) / 2;
        int y = (height - 166) / 2;
        graphics.fill(x, y, x + 176, y + 166, 0xFF1A1A2E);
        graphics.fill(x, y, x + 176, y + 3, 0xFFFFD700);
        graphics.drawCenteredString(font, title, width / 2, y + 15, 0xFFD700);
    }
}
