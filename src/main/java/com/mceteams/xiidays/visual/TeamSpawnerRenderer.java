package com.mceteams.xiidays.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mceteams.xiidays.world.BlockRegistry;
import com.mceteams.xiidays.world.SpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class TeamSpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity, TeamSpawnerRenderer.State> {

    public TeamSpawnerRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            @NotNull SpawnerBlockEntity spawner,
            @NotNull State state,
            float partialTick,
            @NotNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(spawner, state, partialTick, cameraPosition, breakProgress);

        var player = Minecraft.getInstance().player;
        boolean should;
        if (player == null) {
            should = false;
        } else {
            var held = player.getMainHandItem();
            var off = player.getOffhandItem();
            should = held.is(BlockRegistry.TEAM_SPAWNER.get().asItem())
                    || off.is(BlockRegistry.TEAM_SPAWNER.get().asItem())
                    || held.is(Items.DEBUG_STICK)
                    || off.is(Items.DEBUG_STICK);
        }
        state.shouldRender = should;

        if (should) {
            ItemStack stack = new ItemStack(BlockRegistry.TEAM_SPAWNER.get());
            Minecraft.getInstance().getItemModelResolver()
                    .updateForTopItem(state.itemState, stack, ItemDisplayContext.NONE, spawner.getLevel(), null, 0);
        }
    }

    @Override
    public void submit(@NotNull State state, @NotNull PoseStack poseStack,
                       @NotNull SubmitNodeCollector nodeCollector, @NotNull CameraRenderState cameraRenderState) {
        if (!state.shouldRender) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        state.itemState.submit(poseStack, nodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static class State extends BlockEntityRenderState {
        public boolean shouldRender;
        public final ItemStackRenderState itemState = new ItemStackRenderState();
    }
}