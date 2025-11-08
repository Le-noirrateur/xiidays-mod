package com.mceteams.xiidays.render;

import com.mceteams.xiidays.blocks.BlockRegistry;
import com.mceteams.xiidays.blocks.teamSpawner.teamSpawner;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class TeamSpawnerRenderer implements BlockEntityRenderer<teamSpawner> {

    public TeamSpawnerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(@NotNull teamSpawner spawner, float partialTicks, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int light, int overlay) {

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        var held = player.getMainHandItem();
        var off = player.getOffhandItem();

        boolean shouldRender = held.is(BlockRegistry.TEAM_SPAWNER.get().asItem())
                || off.is(BlockRegistry.TEAM_SPAWNER.get().asItem())
                || held.is(Items.DEBUG_STICK)
                || off.is(Items.DEBUG_STICK);

        if (!shouldRender) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ItemStack stack = new ItemStack(BlockRegistry.TEAM_SPAWNER.get());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        itemRenderer.renderStatic(stack, ItemDisplayContext.NONE,
                light, overlay, poseStack, buffer, spawner.getLevel(), 0);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull teamSpawner blockEntity) {
        return true;
    }
}
