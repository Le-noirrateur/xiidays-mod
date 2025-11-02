package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.blocks.teamCore.teamCore;
import com.mceteams.xiidays.blocks.teamSpawner.teamSpawner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class TeamBlockOverlay {
    public static List<String> getBlockInfo(BlockState state, BlockEntity be) {
        List<String> infos = new ArrayList<>();

        // Si c’est ton BlockEntity custom
        if (be instanceof teamCore core) {
            infos.add("TeamCore:");
            infos.add("");
            infos.add("Team ID: " + core.getTeamId());
            infos.add("Position : " + core.getBlockPos().getX() + ", " + core.getBlockPos().getY() + ", " + core.getBlockPos().getZ());
            infos.add("Puzzle Solved: " + core.isPuzzleSolved());
        } else if (be instanceof teamSpawner spawnBlock) {
            infos.add("TeamSpawner:");
            infos.add("");
            infos.add("Team ID: " + spawnBlock.getTeamId());
            infos.add("Position : " + spawnBlock.getBlockPos().getX() + ", " + spawnBlock.getBlockPos().getY() + ", " + spawnBlock.getBlockPos().getZ());
        }

        return infos;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Vérifie si on a bien le debug stick
        if (!mc.player.getMainHandItem().is(Items.DEBUG_STICK)) return;

        if (!(mc.hitResult instanceof BlockHitResult bhr)) return;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        BlockEntity be = mc.level.getBlockEntity(pos);

        List<String> infos = getBlockInfo(state, be);
        if (infos.isEmpty()) return;

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;

        int maxWidth = infos.stream().mapToInt(font::width).max().orElse(0);
        int boxWidth = maxWidth + 8;
        int boxHeight = infos.size() * (font.lineHeight + 2) + 4;

        // Position : milieu de l'écran (verticalement) et à droite du centre (crosshair)
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        int x = centerX + 20; // décalage horizontal à droite du curseur
        int y = centerY - (boxHeight / 2); // centré verticalement

        // Ajustement si déborde écran
        if (x + boxWidth > mc.getWindow().getGuiScaledWidth()) {
            x = mc.getWindow().getGuiScaledWidth() - boxWidth - 5;
        }

        // Fond noir semi-transparent
        gui.fill(x, y, x + boxWidth, y + boxHeight, 0xAA000000);

        // Texte ligne par ligne
        int offsetY = y + 2;
        for (String line : infos) {
            gui.drawString(font, line, x + 4, offsetY, 0xFFFFFF);
            offsetY += font.lineHeight + 2;
        }
    }
}