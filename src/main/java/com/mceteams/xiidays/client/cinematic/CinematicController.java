package com.mceteams.xiidays.client.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class CinematicController {

    private static CameraPath currentPath = null;
    private static long startTime = 0;
    private static boolean lockControls = false;
    private static ArmorStand dummy = null;
    private static Entity previousCameraTarget = null;

    public static void play(CameraPath path, boolean lockPlayerControls) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        currentPath = path;
        startTime = System.currentTimeMillis();
        lockControls = lockPlayerControls;

        CameraPath.Sample s = path.sample(0f);
        if (s == null) return;

        Vec3 pos = s.position();
        Vec2 rot = s.rotation();

        dummy = new ArmorStand(mc.level, pos.x, pos.y, pos.z);
        dummy.setNoGravity(true);
        dummy.setInvisible(true);
        dummy.setNoBasePlate(true);
        dummy.setYRot(rot.x);
        dummy.setXRot(rot.y);
        mc.level.addFreshEntity(dummy);

        previousCameraTarget = mc.getCameraEntity();
        mc.setCameraEntity(dummy);
    }

    public static void stop() {
        if (dummy != null) {
            Minecraft mc = Minecraft.getInstance();
            if (previousCameraTarget != null && mc.getCameraEntity() == dummy) {
                mc.setCameraEntity(previousCameraTarget);
            }
            dummy.discard();
            dummy = null;
        }
        currentPath = null;
        lockControls = false;
        previousCameraTarget = null;
    }

    public static boolean isPlaying() {
        return currentPath != null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (currentPath == null || dummy == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !dummy.isAlive()) {
            stop();
            return;
        }

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;

        if (elapsed > currentPath.getDuration()) {
            stop();
            return;
        }

        CameraPath.Sample s = currentPath.sample(elapsed);
        if (s == null) {
            stop();
            return;
        }

        Vec3 pos = s.position();
        Vec2 rot = s.rotation();
        dummy.setPos(pos.x, pos.y, pos.z);
        dummy.setYRot(rot.x);
        dummy.setXRot(rot.y);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (lockControls && currentPath != null) {
            try {
                var field = net.minecraft.client.player.ClientInput.class.getDeclaredField("moveVector");
                field.setAccessible(true);
                field.set(event.getInput(), net.minecraft.world.phys.Vec2.ZERO);
            } catch (ReflectiveOperationException ignored) {
            }
            event.getInput().keyPresses = net.minecraft.world.entity.player.Input.EMPTY;
        }
    }

    public static CameraPath createDayStartPath(Minecraft mc) {
        if (mc.player == null) return null;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        Vec3 behind = eye.subtract(look.multiply(4, 0, 4)).add(0, 3, 0);
        Vec3 aboveBehind = eye.add(0, 5, 0).subtract(look.multiply(3, 0, 3));
        Vec3 close = eye.subtract(look.multiply(0.5, 0, 0.5)).add(0, 0.5, 0);

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        return new CameraPath()
                .add(0f, behind, new Vec2(yaw, 30f), Easing.Type.SINE_OUT)
                .add(0.8f, aboveBehind, new Vec2(yaw, 15f), Easing.Type.SINE_OUT)
                .add(1.6f, close, new Vec2(yaw, 5f), Easing.Type.QUAD_IN)
                .add(2.0f, eye, new Vec2(yaw, pitch), Easing.Type.QUAD_OUT);
    }
}
