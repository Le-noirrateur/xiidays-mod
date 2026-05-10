package com.mceteams.xiidays.client.cinematic;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CameraPath {

    private final List<Keyframe> keyframes = new ArrayList<>();
    private float duration = 0f;

    public record Keyframe(float time, Vec3 position, Vec2 rotation, Easing.Type easing) {}

    public record Sample(Vec3 position, Vec2 rotation) {}

    public CameraPath add(float time, Vec3 position, Vec2 rotation, Easing.Type easing) {
        keyframes.add(new Keyframe(time, position, rotation, easing));
        if (time > duration) duration = time;
        return this;
    }

    public float getDuration() {
        return duration;
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public Sample sample(float t) {
        if (keyframes.isEmpty()) return null;

        if (t <= keyframes.getFirst().time()) {
            Keyframe kf = keyframes.getFirst();
            return new Sample(kf.position(), kf.rotation());
        }

        if (t >= keyframes.getLast().time()) {
            Keyframe kf = keyframes.getLast();
            return new Sample(kf.position(), kf.rotation());
        }

        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe a = keyframes.get(i);
            Keyframe b = keyframes.get(i + 1);
            if (t >= a.time() && t < b.time()) {
                float local = (t - a.time()) / (b.time() - a.time());
                float eased = Easing.apply(a.easing(), local);

                double px = lerp(a.position().x, b.position().x, eased);
                double py = lerp(a.position().y, b.position().y, eased);
                double pz = lerp(a.position().z, b.position().z, eased);

                float rx = lerp(a.rotation().x, b.rotation().x, eased);
                float ry = lerp(a.rotation().y, b.rotation().y, eased);

                return new Sample(new Vec3(px, py, pz), new Vec2(rx, ry));
            }
        }
        return null;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }
}
