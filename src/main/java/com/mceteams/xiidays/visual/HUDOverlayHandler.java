package com.mceteams.xiidays.visual;

import com.mceteams.xiidays.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class HUDOverlayHandler {

    // ── Day notification ──
    private static DayNotificationPayload currentNotification = null;
    private static long dayNotifStart = 0;

    // ── Death overlay ──
    private static DeathNotificationPayload deathNotif = null;
    private static long deathTime = 0;
    private static long respawnEndTime = 0;

    // ── Spectator status ──
    private static int spectatorMode = SpectatorStatusPayload.MODE_NONE;
    private static String spectatorTargetName = "";

    // ── Elimination notification ──
    private static EliminationNotificationPayload elimNotif = null;
    private static long elimTime = 0;

    // ── Game over overlay ──
    private static GameOverPayload gameOverNotif = null;
    private static long gameOverTime = 0;

    public static void onDayNotification(DayNotificationPayload packet) {
        currentNotification = packet;
        dayNotifStart = System.currentTimeMillis();
    }

    public static void onDeathNotification(DeathNotificationPayload packet) {
        deathNotif = packet;
        deathTime = System.currentTimeMillis();
        if (packet.respawnDelay() > 0) {
            respawnEndTime = System.currentTimeMillis() + packet.respawnDelay() * 1000L;
        } else {
            respawnEndTime = 0;
        }
    }

    public static void onSpectatorStatus(SpectatorStatusPayload packet) {
        spectatorMode = packet.mode();
        spectatorTargetName = packet.targetName();
    }

    public static void onEliminationNotification(EliminationNotificationPayload packet) {
        elimNotif = packet;
        elimTime = System.currentTimeMillis();
    }

    public static void onGameOver(GameOverPayload packet) {
        gameOverNotif = packet;
        gameOverTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        long now = System.currentTimeMillis();

        // Render day notification
        if (currentNotification != null) {
            long elapsed = now - dayNotifStart;
            float t = elapsed / 1000f;
            if (currentNotification.notificationType() == DayNotificationPayload.NotificationType.START) {
                renderDayStart(graphics, font, w, h, t);
            } else {
                renderDayEnd(graphics, font, w, h, t);
            }
            if (t > 6f) currentNotification = null;
        }

        // Render death overlay
        if (deathNotif != null) {
            float t = (now - deathTime) / 1000f;
            renderDeath(graphics, font, w, h, t, now);
        }

        // Render spectator info HUD
        if (spectatorMode != SpectatorStatusPayload.MODE_NONE) {
            renderSpectatorInfo(graphics, font, w, h);
        }

        // Render elimination notification
        if (elimNotif != null) {
            float t = (now - elimTime) / 1000f;
            renderElimination(graphics, font, w, h, t);
        }

        // Render game over overlay
        if (gameOverNotif != null) {
            float t = (now - gameOverTime) / 1000f;
            renderGameOver(graphics, font, w, h, t);
        }
    }

    // ── Death overlay ──

    private static void renderDeath(GuiGraphics graphics, Font font, int w, int h, float t, long now) {
        // Phase 1 (0-1.5s): Fullscreen red overlay fades in, "ELIMINATED" text in center
        if (t < 1.5f) {
            float p = Math.min(t / 1.5f, 1f);
            float easeIn = p * p;
            int alpha = (int) (180 * easeIn);
            if (alpha > 0) {
                graphics.fill(0, 0, w, h, (alpha << 24) | 0x330000);
            }

            int textAlpha = (int) (255 * Math.min(t / 0.5f, 1f));
            drawCentered(graphics, font, "§c§lELIMINATED", w / 2, h / 2 - 20, textAlpha);
        }

        // Phase 2 (1.5-2.5s): Overlay shrinks up, text moves up
        if (t >= 1.5f && t < 2.5f) {
            float p = (t - 1.5f) / 1f;
            float easeOut = 1f - (1f - p) * (1f - p);
            int overlayH = (int) (h * (1f - easeOut));
            int alpha = (int) (180 * (1f - easeOut));
            if (alpha > 0) {
                graphics.fillGradient(0, 0, w, overlayH, (alpha << 24) | 0x330000, 0);
            }

            int textY = (int) (h / 2 - 20 - easeOut * 60);
            int textAlpha = (int) (255 * (1f - easeOut));
            if (textAlpha > 0) {
                drawCentered(graphics, font, "§c§lELIMINATED", w / 2, textY, textAlpha);
            }
        }

        // Phase 3 (2.5s+): If respawn timer active, show countdown
        if (t >= 2.5f && respawnEndTime > 0) {
            long remaining = (respawnEndTime - now) / 1000;
            if (remaining > 0) {
                String timer = "§cRespawn in §f" + remaining + "§cs";
                int ta = (int) (255 * Math.min((t - 2.5f) / 0.5f, 1f));
                drawCentered(graphics, font, timer, w / 2, h - 60, ta);
            } else {
                deathNotif = null;
            }
        } else if (t >= 4f) {
            deathNotif = null;
        }
    }

    // ── Game Over overlay ──

    private static void renderGameOver(GuiGraphics graphics, Font font, int w, int h, float t) {
        if (t > 4f) { gameOverNotif = null; return; }

        float alpha;
        if (t < 0.5f) {
            alpha = t / 0.5f;
        } else if (t < 2.5f) {
            alpha = 1f;
        } else {
            alpha = Math.max(0, 1f - (t - 2.5f) / 1.5f);
        }
        if (alpha < 0.01f) return;
        int a = (int) (255 * alpha);

        graphics.fill(0, 0, w, h, (int) (alpha * 0.5f * 255) << 24 | 0x000000);

        String text = gameOverNotif.isWinner() ? "§6§lVICTORY" : "§c§lDEFEATED";
        drawCentered(graphics, font, text, w / 2, h / 2 - 20, a);

        String sub = gameOverNotif.isWinner() ? "§7Your team is the last standing!" : "§7Your team has been eliminated";
        int subA = (int) (a * 0.8f);
        drawCentered(graphics, font, sub, w / 2, h / 2 + 15, subA);
    }

    // ── Elimination notification ──

    private static void renderElimination(GuiGraphics graphics, Font font, int w, int h, float t) {
        if (t > 5f) { elimNotif = null; return; }

        float alpha;
        int xOffset;
        if (t < 0.5f) {
            float p = t / 0.5f;
            alpha = p;
            xOffset = (int) (250 * (1f - p));
        } else if (t < 3.5f) {
            alpha = 1f;
            xOffset = 0;
        } else {
            float p = (t - 3.5f) / 1.5f;
            alpha = Math.max(0, 1f - p);
            xOffset = 0;
        }

        if (alpha < 0.01f) return;

        String text = "§c§l" + elimNotif.teamName() + " §7has been eliminated!";
        if (elimNotif.gameOver()) {
            text = "§c§l" + elimNotif.teamName() + " §7has been eliminated!\n§6§lGame Over!";
        }

        int x = w - 260 + xOffset;
        int y = 20;
        int color = (int) (alpha * 255) << 24 | 0xFFFFFF;
        int tw = font.width(text);
        graphics.drawString(font, Component.literal(text), x + 4, y, color);
    }

    // ── Spectator Info HUD ──

    private static void renderSpectatorInfo(GuiGraphics graphics, Font font, int w, int h) {
        String modeText;
        switch (spectatorMode) {
            case SpectatorStatusPayload.MODE_TEAMMATE_WATCH -> {
                if (!spectatorTargetName.isEmpty()) {
                    modeText = "§7Watching: §f" + spectatorTargetName;
                } else {
                    modeText = "§7Teammate Watch";
                }
            }
            case SpectatorStatusPayload.MODE_BASE_SPECTATE -> modeText = "§7Base Spectate";
            case SpectatorStatusPayload.MODE_FREE_SPECTATE -> modeText = "§7Free Spectate";
            default -> modeText = "§7Spectator";
        }
        int color = 0xFFFFFF;
        graphics.drawString(font, Component.literal(modeText), 10, h - 30, color);
    }

    // ── Day Start ──

    private static void renderDayStart(GuiGraphics graphics, Font font, int w, int h, float t) {
        float totalAlpha;
        if (t < 4f) {
            totalAlpha = 1f;
        } else {
            totalAlpha = Math.max(0, 1f - (t - 4f) / 2f);
        }
        if (totalAlpha < 0.01f) return;

        // Phase 1 (0-1.2s): "THE DAY HAS BEGUN" fades in at center (title scale),
        // then shrinks to subtitle scale and moves to top
        float titleEase = Math.min(t / 1.2f, 1f);
        float titleEaseOut = 1f - (1f - titleEase) * (1f - titleEase);

        float titleScale = 3f + (1.5f - 3f) * titleEaseOut;
        int titleStartY = h / 2;
        int titleEndY = h / 4;
        int titleY = (int) (titleStartY + (titleEndY - titleStartY) * titleEaseOut);
        int titleAlpha = (int) (255 * Math.min(t / 0.3f, 1f) * totalAlpha);
        if (titleAlpha > 0) {
            drawScaledCentered(graphics, font, "§6§lTHE DAY HAS BEGUN", w / 2, titleY, titleScale, titleAlpha);
        }

        // Phase 2 (1.5-3.5s): Day number transition
        if (t >= 1.5f) {
            int dayLabelY = h / 2 + 10;
            int numberOffset = font.lineHeight + 5;
            int numberY = dayLabelY + numberOffset;

            // "Day" label (fixed)
            float dayLabelFade = Math.min((t - 1.5f) / 0.5f, 1f);
            int dayLabelAlpha = (int) (255 * dayLabelFade * totalAlpha);
            if (dayLabelAlpha > 0) {
                drawScaledCentered(graphics, font, "§fDay", w / 2, dayLabelY, 1.5f, dayLabelAlpha);
            }

            // Number transition
            if (t >= 1.8f && t < 3.5f) {
                float p = (t - 1.8f) / 1.2f;
                if (p > 1f) p = 1f;
                float easeP = p * p; // ease-in

                int slideUp = (int) (30 * easeP);

                // Old number: slides up and fades out
                int oldNumY = numberY - slideUp;
                int oldAlpha = (int) (255 * (1f - easeP) * totalAlpha);
                if (oldAlpha > 0) {
                    drawScaledCentered(graphics, font, "§7" + currentNotification.oldDay(), w / 2, oldNumY, 1.5f, oldAlpha);
                }

                // New number: slides from below and fades in
                int newNumY = numberY + (int) (30 * (1f - easeP));
                int newAlpha = (int) (255 * easeP * totalAlpha);
                if (newAlpha > 0) {
                    drawScaledCentered(graphics, font, "§6§l" + currentNotification.newDay(), w / 2, newNumY, 1.5f, newAlpha);
                }
            } else if (t >= 3.5f) {
                // After transition, show only new number
                int numAlpha = (int) (255 * totalAlpha);
                drawScaledCentered(graphics, font, "§6§l" + currentNotification.newDay(), w / 2, numberY, 1.5f, numAlpha);
            }
        }
    }

    // ── Day End ──

    private static void renderDayEnd(GuiGraphics graphics, Font font, int w, int h, float t) {
        if (t > 4f) return;

        float alpha;
        if (t < 1f) {
            alpha = t / 1f;
        } else if (t < 2.5f) {
            alpha = 1f;
        } else {
            alpha = Math.max(0, 1f - (t - 2.5f) / 1.5f);
        }

        if (alpha < 0.01f) return;

        int midX = w / 2;
        int midY = h / 2;

        // "THE" slides from left
        String leftText = "§cTHE";
        int leftW = font.width("THE");
        int leftOffset = (int) ((1f - Math.min(t / 0.5f, 1f)) * 80);
        int leftX = midX - 60 - leftW + leftOffset;
        int leftA = (int) (255 * alpha * Math.min(t / 0.5f, 1f));
        drawText(graphics, font, leftText, leftX, midY - 10, leftA);

        // "DAY" fixed center
        String centerText = "§cDAY";
        int cA = (int) (255 * alpha * Math.min(Math.max((t - 0.15f) / 0.35f, 0), 1f));
        drawText(graphics, font, centerText, midX - font.width("DAY") / 2, midY - 10, cA);

        // "HAS FINISHED" slides from right
        String rightText = "§cHAS FINISHED";
        int rightW = font.width("HAS FINISHED");
        int rightOffset = (int) ((1f - Math.min(t / 0.5f, 1f)) * 80);
        int rightX = midX + 60 - rightOffset;
        int rightA = (int) (255 * alpha * Math.min(t / 0.5f, 1f));
        drawText(graphics, font, rightText, rightX, midY - 10, rightA);
    }

    // ── Drawing helpers (no backgrounds) ──

    private static void drawCentered(GuiGraphics graphics, Font font, String text, int x, int y, int alpha) {
        if (alpha <= 0) return;
        int color = (alpha << 24) | 0xFFFFFF;
        graphics.drawString(font, Component.literal(text), x - font.width(text) / 2, y, color);
    }

    private static void drawScaledCentered(GuiGraphics graphics, Font font, String text, int x, int y, float scale, int alpha) {
        if (alpha <= 0) return;
        int color = (alpha << 24) | 0xFFFFFF;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1);
        graphics.drawString(font, Component.literal(text), -font.width(text) / 2, 0, color);
        pose.popPose();
    }

    private static void drawText(GuiGraphics graphics, Font font, String text, int x, int y, int alpha) {
        if (alpha <= 0) return;
        int color = (alpha << 24) | 0xFFFFFF;
        graphics.drawString(font, Component.literal(text), x, y, color);
    }
}
