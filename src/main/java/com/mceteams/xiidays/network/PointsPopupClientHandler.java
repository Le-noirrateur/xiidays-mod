package com.mceteams.xiidays.network;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PointsPopupClientHandler {

    private static final List<PopupEntry> activePopups = new ArrayList<>();
    private static final int MAX_POPUPS = 5;
    private static final int SLOT_HEIGHT = 32;
    private static final int PANEL_WIDTH = 140;
    private static final int PANEL_HEIGHT = 22;
    private static final int BASE_DURATION_MS = 3000;
    private static final int MAX_DURATION_MS = 10000;
    private static final float SMOOTHING = 0.12f;

    public static void show(PointsPopupPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int baseY = screenHeight - 50;
        int count = activePopups.size();
        float lifetime = Math.min(MAX_DURATION_MS, BASE_DURATION_MS + count * 1500f);

        PopupEntry entry = new PopupEntry(payload.points(), payload.typeName(),
                System.currentTimeMillis(), (long) lifetime);
        entry.visualY = baseY + 20;
        activePopups.add(entry);

        if (activePopups.size() > MAX_POPUPS) {
            activePopups.remove(0);
        }
    }

    public static void render(GuiGraphics graphics, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        long now = System.currentTimeMillis();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int baseY = screenHeight - 50;

        Iterator<PopupEntry> it = activePopups.iterator();
        while (it.hasNext()) {
            PopupEntry entry = it.next();
            long age = now - entry.createdAt;
            if (age > entry.lifetimeMs) {
                it.remove();
                continue;
            }
        }

        // Animate each popup toward its target slot
        for (int i = 0; i < activePopups.size(); i++) {
            PopupEntry entry = activePopups.get(i);
            float targetY = baseY - i * SLOT_HEIGHT;
            entry.visualY += (targetY - entry.visualY) * SMOOTHING;
        }

        // Render bottom to top so newest (index 0) renders on top
        for (int i = activePopups.size() - 1; i >= 0; i--) {
            PopupEntry entry = activePopups.get(i);
            long age = now - entry.createdAt;
            float progress = (float) age / entry.lifetimeMs;

            // Fade out in last 600ms
            int alpha = 255;
            float fadeStart = 1.0f - 600f / entry.lifetimeMs;
            if (progress > fadeStart) {
                float fadeProgress = (progress - fadeStart) / (1.0f - fadeStart);
                alpha = (int) (255 * (1.0f - fadeProgress));
            }
            alpha = Math.max(0, Math.min(255, alpha));

            int color = entry.points >= 0 ? 0xFF00FF00 : 0xFFFF0000;
            int bgColor = (entry.points >= 0 ? 0x80002a00 : 0x802a0000);
            bgColor = (bgColor & 0x00FFFFFF) | (alpha << 24);
            color = (color & 0x00FFFFFF) | (alpha << 24);

            int x = screenWidth - PANEL_WIDTH - 8;
            int y = Math.round(entry.visualY);

            graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, bgColor);
            graphics.fill(x, y, x + PANEL_WIDTH, y + 2, color);
            graphics.drawString(mc.font, entry.formattedText(), x + 6, y + 6, color);
        }
    }

    private static class PopupEntry {
        final int points;
        final String typeName;
        final long createdAt;
        final long lifetimeMs;
        float visualY;

        PopupEntry(int points, String typeName, long createdAt, long lifetimeMs) {
            this.points = points;
            this.typeName = typeName;
            this.createdAt = createdAt;
            this.lifetimeMs = lifetimeMs;
            this.visualY = createdAt;
        }

        String displayName() {
            return Component.translatable("xiidays.points_popup." + typeName.toLowerCase()).getString();
        }

        String formattedText() {
            if (points == 0) return "§a" + displayName();
            String sign = points >= 0 ? "§a+" : "§c";
            return "§7" + displayName() + " " + sign + points;
        }
    }
}
