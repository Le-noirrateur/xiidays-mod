package com.mceteams.xiidays.client.cinematic;

public class Easing {

    public enum Type {
        LINEAR,
        QUAD_IN, QUAD_OUT, QUAD_INOUT,
        CUBIC_IN, CUBIC_OUT, CUBIC_INOUT,
        QUART_IN, QUART_OUT, QUART_INOUT,
        QUINT_IN, QUINT_OUT, QUINT_INOUT,
        SINE_IN, SINE_OUT, SINE_INOUT,
        EXPO_IN, EXPO_OUT, EXPO_INOUT,
        CIRC_IN, CIRC_OUT, CIRC_INOUT,
        BACK_IN, BACK_OUT, BACK_INOUT,
        ELASTIC_IN, ELASTIC_OUT, ELASTIC_INOUT,
        BOUNCE_IN, BOUNCE_OUT, BOUNCE_INOUT
    }

    public static float apply(Type type, float t) {
        return switch (type) {
            case LINEAR -> t;
            case QUAD_IN -> quadIn(t);
            case QUAD_OUT -> quadOut(t);
            case QUAD_INOUT -> quadInOut(t);
            case CUBIC_IN -> cubicIn(t);
            case CUBIC_OUT -> cubicOut(t);
            case CUBIC_INOUT -> cubicInOut(t);
            case QUART_IN -> quartIn(t);
            case QUART_OUT -> quartOut(t);
            case QUART_INOUT -> quartInOut(t);
            case QUINT_IN -> quintIn(t);
            case QUINT_OUT -> quintOut(t);
            case QUINT_INOUT -> quintInOut(t);
            case SINE_IN -> sineIn(t);
            case SINE_OUT -> sineOut(t);
            case SINE_INOUT -> sineInOut(t);
            case EXPO_IN -> expoIn(t);
            case EXPO_OUT -> expoOut(t);
            case EXPO_INOUT -> expoInOut(t);
            case CIRC_IN -> circIn(t);
            case CIRC_OUT -> circOut(t);
            case CIRC_INOUT -> circInOut(t);
            case BACK_IN -> backIn(t);
            case BACK_OUT -> backOut(t);
            case BACK_INOUT -> backInOut(t);
            case ELASTIC_IN -> elasticIn(t);
            case ELASTIC_OUT -> elasticOut(t);
            case ELASTIC_INOUT -> elasticInOut(t);
            case BOUNCE_IN -> bounceIn(t);
            case BOUNCE_OUT -> bounceOut(t);
            case BOUNCE_INOUT -> bounceInOut(t);
        };
    }

    public static float apply(Type type, float a, float b, float t) {
        return a + (b - a) * apply(type, t);
    }

    private static float quadIn(float t) { return t * t; }
    private static float quadOut(float t) { return t * (2f - t); }
    private static float quadInOut(float t) {
        return t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
    }

    private static float cubicIn(float t) { return t * t * t; }
    private static float cubicOut(float t) {
        float t1 = t - 1f;
        return t1 * t1 * t1 + 1f;
    }
    private static float cubicInOut(float t) {
        return t < 0.5f ? 4f * t * t * t : (t - 1f) * (2f * t - 2f) * (2f * t - 2f) + 1f;
    }

    private static float quartIn(float t) { return t * t * t * t; }
    private static float quartOut(float t) {
        float t1 = t - 1f;
        return 1f - t1 * t1 * t1 * t1;
    }
    private static float quartInOut(float t) {
        return t < 0.5f ? 8f * t * t * t * t : 1f - 8f * (t - 1f) * (t - 1f) * (t - 1f) * (t - 1f);
    }

    private static float quintIn(float t) { return t * t * t * t * t; }
    private static float quintOut(float t) {
        float t1 = t - 1f;
        return 1f + t1 * t1 * t1 * t1 * t1;
    }
    private static float quintInOut(float t) {
        return t < 0.5f ? 16f * t * t * t * t * t : 1f + 16f * (t - 1f) * (t - 1f) * (t - 1f) * (t - 1f) * (t - 1f);
    }

    private static float sineIn(float t) { return 1f - (float) Math.cos(t * Math.PI / 2f); }
    private static float sineOut(float t) { return (float) Math.sin(t * Math.PI / 2f); }
    private static float sineInOut(float t) { return (float) (Math.cos(t * Math.PI) - 1f) / -2f; }

    private static float expoIn(float t) { return t == 0f ? 0f : (float) Math.pow(2f, 10f * (t - 1f)); }
    private static float expoOut(float t) { return t == 1f ? 1f : 1f - (float) Math.pow(2f, -10f * t); }
    private static float expoInOut(float t) {
        if (t == 0f || t == 1f) return t;
        return t < 0.5f ? (float) Math.pow(2f, 20f * t - 10f) / 2f : (2f - (float) Math.pow(2f, -20f * t + 10f)) / 2f;
    }

    private static float circIn(float t) { return 1f - (float) Math.sqrt(1f - t * t); }
    private static float circOut(float t) {
        float t1 = t - 1f;
        return (float) Math.sqrt(1f - t1 * t1);
    }
    private static float circInOut(float t) {
        return t < 0.5f ? (1f - (float) Math.sqrt(1f - 4f * t * t)) / 2f
                : ((float) Math.sqrt(1f - (2f * t - 2f) * (2f * t - 2f)) + 1f) / 2f;
    }

    private static final float BACK_S = 1.70158f;
    private static float backIn(float t) { return t * t * ((BACK_S + 1f) * t - BACK_S); }
    private static float backOut(float t) {
        float t1 = t - 1f;
        return t1 * t1 * ((BACK_S + 1f) * t1 + BACK_S) + 1f;
    }
    private static float backInOut(float t) {
        float s = BACK_S * 1.525f;
        return t < 0.5f
                ? 0.5f * (t * 2f * t * 2f * ((s + 1f) * t * 2f - s))
                : 0.5f * ((t * 2f - 2f) * (t * 2f - 2f) * ((s + 1f) * (t * 2f - 2f) + s) + 2f);
    }

    private static float elasticIn(float t) {
        if (t == 0f || t == 1f) return t;
        float p = 0.3f;
        float s = p / 4f;
        return -(float) Math.pow(2f, 10f * (t - 1f)) * (float) Math.sin((t - 1f - s) * (2f * (float) Math.PI) / p);
    }
    private static float elasticOut(float t) {
        if (t == 0f || t == 1f) return t;
        float p = 0.3f;
        float s = p / 4f;
        return (float) Math.pow(2f, -10f * t) * (float) Math.sin((t - s) * (2f * (float) Math.PI) / p) + 1f;
    }
    private static float elasticInOut(float t) {
        if (t == 0f || t == 1f) return t;
        float p = 0.3f * 1.5f;
        float s = p / 4f;
        if (t < 0.5f) {
            return -0.5f * (float) Math.pow(2f, 20f * t - 10f) * (float) Math.sin((20f * t - 11.125f) * (2f * (float) Math.PI) / p);
        }
        return (float) Math.pow(2f, -20f * t + 10f) * (float) Math.sin((20f * t - 11.125f) * (2f * (float) Math.PI) / p) * 0.5f + 1f;
    }

    private static float bounceIn(float t) { return 1f - bounceOut(1f - t); }
    private static float bounceOut(float t) {
        if (t < 1f / 2.75f) return 7.5625f * t * t;
        if (t < 2f / 2.75f) {
            float t2 = t - 1.5f / 2.75f;
            return 7.5625f * t2 * t2 + 0.75f;
        }
        if (t < 2.5f / 2.75f) {
            float t2 = t - 2.25f / 2.75f;
            return 7.5625f * t2 * t2 + 0.9375f;
        }
        float t2 = t - 2.625f / 2.75f;
        return 7.5625f * t2 * t2 + 0.984375f;
    }
    private static float bounceInOut(float t) {
        return t < 0.5f ? bounceIn(t * 2f) * 0.5f : bounceOut(t * 2f - 1f) * 0.5f + 0.5f;
    }
}
