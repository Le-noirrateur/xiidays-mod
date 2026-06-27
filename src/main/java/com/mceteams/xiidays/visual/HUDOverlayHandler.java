package com.mceteams.xiidays.visual;

import com.mceteams.xiidays.client.cinematic.CameraPath;
import com.mceteams.xiidays.client.cinematic.CinematicController;
import com.mceteams.xiidays.network.*;
import com.mceteams.xiidays.player.ClientRankTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

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

    // ── Day-end scoreboard ──
    private static DayEndScorePayload dayEndScore = null;
    private static long dayEndScoreStart = 0;

    public static void onDayNotification(DayNotificationPayload packet) {
        currentNotification = packet;
        dayNotifStart = System.currentTimeMillis();
        if (packet.notificationType() == DayNotificationPayload.NotificationType.START) {
            CameraPath path = CinematicController.createDayStartPath(Minecraft.getInstance());
            if (path != null) {
                CinematicController.play(path, true);
            }
        }
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

    public static void onDayEndScore(DayEndScorePayload packet) {
        dayEndScore = packet;
        dayEndScoreStart = System.currentTimeMillis();
        // Update client rank tracker
        List<String> rankingOrder = packet.teams().stream()
                .map(DayEndScorePayload.TeamEntry::teamName).toList();
        ClientRankTracker.updateRanking(rankingOrder);
        ClientRankTracker.cleanup();
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

        // Render day-end scoreboard
        if (dayEndScore != null) {
            float t = (now - dayEndScoreStart) / 1000f;
            renderDayEndScoreboard(graphics, font, w, h, t, now);
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
            drawCentered(graphics, font, Component.translatable("xiidays.hud.eliminated").getString(), w / 2, h / 2 - 20, textAlpha);
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
                drawCentered(graphics, font, Component.translatable("xiidays.hud.eliminated").getString(), w / 2, textY, textAlpha);
            }
        }

        // Phase 3 (2.5s+): If respawn timer active, show countdown
        if (t >= 2.5f && respawnEndTime > 0) {
            long remaining = (respawnEndTime - now) / 1000;
            if (remaining > 0) {
                String timer = Component.translatable("xiidays.hud.respawn_in", remaining).getString();
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

        String text = gameOverNotif.isWinner() ? Component.translatable("xiidays.hud.victory").getString() : Component.translatable("xiidays.hud.defeated").getString();
        drawCentered(graphics, font, text, w / 2, h / 2 - 20, a);

        String sub = gameOverNotif.isWinner() ? Component.translatable("xiidays.hud.victory_sub").getString() : Component.translatable("xiidays.hud.defeated_sub").getString();
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

        String text;
        if (elimNotif.gameOver()) {
            text = Component.translatable("xiidays.hud.game_over", elimNotif.teamName()).getString();
        } else {
            text = Component.translatable("xiidays.hud.eliminated_team", elimNotif.teamName()).getString();
        }

        int x = w - 260 + xOffset;
        int y = 20;
        int color = (int) (alpha * 255) << 24 | 0xFFFFFF;
        int tw = font.width(text);
        graphics.drawString(font, Component.literal(text), x + 4, y, color);
    }

    // ── Spectator Info HUD ──

    private static void renderSpectatorInfo(GuiGraphics graphics, Font font, int w, int h) {
        Component modeComponent;
        switch (spectatorMode) {
            case SpectatorStatusPayload.MODE_TEAMMATE_WATCH -> {
                if (!spectatorTargetName.isEmpty()) {
                    modeComponent = Component.translatable("xiidays.hud.spectator_teammate", spectatorTargetName);
                } else {
                    modeComponent = Component.translatable("xiidays.hud.spectator_teammate_watch");
                }
            }
            case SpectatorStatusPayload.MODE_BASE_SPECTATE -> modeComponent = Component.translatable("xiidays.hud.spectator_base");
            case SpectatorStatusPayload.MODE_FREE_SPECTATE -> modeComponent = Component.translatable("xiidays.hud.spectator_free");
            default -> modeComponent = Component.translatable("xiidays.hud.spectator_default");
        }
        int color = 0xFFFFFF;
        graphics.drawString(font, modeComponent, 10, h - 30, color);
    }

    // ── Day Start ──

    private static void renderDayStart(GuiGraphics graphics, Font font, int w, int h, float t) {
        float totalAlpha;
        if (t < 5f) {
            totalAlpha = 1f;
        } else {
            totalAlpha = Math.max(0, 1f - (t - 5f) / 3f);
        }
        if (totalAlpha < 0.01f) return;

        String titleText = Component.translatable("xiidays.day_start.title").getString();
        String dayLabel = Component.translatable("xiidays.day_start.day_label").getString();

        // ── Title: "The Day Has Begun" ──
        // 0-0.5s: fade in at center (4x)
        // 0.5-2.0s: hold at center (4x)
        // 2.0-3.0s: slide to top + shrink to 2x
        // 3.0s+: stay at top (2x)
        float holdEnd = 2.0f;
        float slideEnd = 3.0f;

        if (t < holdEnd) {
            int alpha = (int) (255 * Math.min(t / 0.5f, 1f) * totalAlpha);
            if (alpha > 0) {
                drawScaledCentered(graphics, font, "§6§l" + titleText, w / 2, h / 2, 4f, alpha);
            }
        } else if (t < slideEnd) {
            float p = (t - holdEnd) / (slideEnd - holdEnd);
            float ease = p * p;
            float scale = 4f + (2f - 4f) * ease;
            int y = (int) ((h / 2) + ((h / 5) - (h / 2)) * ease);
            int alpha = (int) (255 * totalAlpha);
            drawScaledCentered(graphics, font, "§6§l" + titleText, w / 2, y, scale, alpha);
        } else {
            int alpha = (int) (255 * totalAlpha);
            drawScaledCentered(graphics, font, "§6§l" + titleText, w / 2, h / 5, 2f, alpha);
        }

        // ── Day label + number ──
        if (t >= 2.5f) {
            int labelY = h / 2 - 20;
            int numY = labelY + 60;

            // Day label (fades in 2.5-3.0s, holds)
            float labelP = Math.min((t - 2.5f) / 0.5f, 1f);
            int labelAlpha = (int) (255 * labelP * totalAlpha);
            if (labelAlpha > 0) {
                drawScaledCentered(graphics, font, "§f" + dayLabel, w / 2, labelY, 3f, labelAlpha);
            }

            // Number
            if (t >= 3.5f) {
                float numFade = Math.min((t - 3.5f) / 0.5f, 1f);

                if (t < 4.5f) {
                    float p = (t - 3.5f) / 1.0f;
                    if (p > 1f) p = 1f;
                    float easeP = p * p;

                    int slideDist = 50;

                    int oldY = numY - (int) (slideDist * easeP);
                    int oldA = (int) (255 * (1f - easeP) * numFade * totalAlpha);
                    if (oldA > 0) {
                        drawScaledCentered(graphics, font, "§7" + currentNotification.oldDay(), w / 2, oldY, 2f, oldA);
                    }

                    int newY = numY + (int) (slideDist * (1f - easeP));
                    int newA = (int) (255 * easeP * numFade * totalAlpha);
                    if (newA > 0) {
                        drawScaledCentered(graphics, font, "§6§l" + currentNotification.newDay(), w / 2, newY, 2f, newA);
                    }
                } else {
                    int a = (int) (255 * totalAlpha);
                    drawScaledCentered(graphics, font, "§6§l" + currentNotification.newDay(), w / 2, numY, 2f, a);
                }
            }
        }
    }

    // ── Day End ──

    private static void renderDayEnd(GuiGraphics graphics, Font font, int w, int h, float t) {
        if (t > 6f) return;

        float alpha;
        if (t < 3.5f) {
            alpha = 1f;
        } else {
            alpha = Math.max(0, 1f - (t - 3.5f) / 2.5f);
        }
        if (alpha < 0.01f) return;

        String text = Component.translatable("xiidays.day_end.title").getString();
        String format = "§c";
        String raw = format + text;

        int len = text.length();
        int totalPairs = (len + 1) / 2;
        float progress = Math.min(t / 2.0f, 1.0f);
        int pairs = (int) (progress * totalPairs);

        StringBuilder sb = new StringBuilder();
        sb.append(format);
        for (int i = 0; i < len; i++) {
            if (i < pairs || i >= len - pairs) {
                sb.append(text.charAt(i));
            } else {
                sb.append(' ');
            }
        }

        int a = (int) (255 * alpha);
        drawScaledCentered(graphics, font, sb.toString(), w / 2, h / 2 - 10, 3f, a);
    }

    // ── Day-End Scoreboard ──

    private static void renderDayEndScoreboard(GuiGraphics graphics, Font font, int w, int h, float t, long now) {
        float te = t - 5.5f; // wait for day-end text to finish
        if (te < 0) return;

        if (te > 8.0f) { dayEndScore = null; return; }

        float alpha;
        float slideOffset;

        if (te < 0.5f) {
            float p = te / 0.5f;
            alpha = p;
            slideOffset = h * (1f - p * p);
        } else if (te < 6.0f) {
            alpha = 1f;
            slideOffset = 0;
        } else {
            float p = (te - 6.0f) / 2.0f;
            alpha = Math.max(0, 1f - p);
            slideOffset = 0;
        }

        if (alpha < 0.01f) return;

        var teams = dayEndScore.teams();
        int panelW = 260;
        int rowH = 28;
        int headerH = 38;
        int pad = 12;
        int totalH = headerH + teams.size() * rowH + pad * 2;
        int panelX = (w - panelW) / 2;
        int baseY = (h - totalH) / 2;
        int panelY = (int) (baseY + slideOffset);
        int a = (int) (255 * alpha);
        int bgA = (int) (200 * alpha);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + totalH, (bgA << 24) | 0x1A1A2E);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 3, (a << 24) | 0xFFD700);

        String title = Component.translatable("xiidays.hud.scoreboard_title").getString();
        int titleX = panelX + (panelW - font.width(title)) / 2;
        int titleY = panelY + pad + 8;
        drawText(graphics, font, title, titleX, titleY, a);

        int sepY = titleY + 16;
        int sepA = (int) (80 * alpha);
        if (sepA > 0) {
            graphics.fill(panelX + pad, sepY, panelX + panelW - pad, sepY + 1, (sepA << 24) | 0xFFFFFF);
        }

        int rowY = sepY + 8;
        for (int i = 0; i < teams.size(); i++) {
            var entry = teams.get(i);

            int rowBgA = (int) (10 * alpha);
            if (rowBgA > 0 && i % 2 == 1) {
                graphics.fill(panelX, rowY, panelX + panelW, rowY + rowH, (rowBgA << 24) | 0xFFFFFF);
            }

            String pos = Component.translatable("xiidays.hud.scoreboard_rank", i + 1).getString();
            drawText(graphics, font, pos, panelX + pad, rowY + 6, a);

            graphics.drawString(font, entry.teamName(), panelX + 40, rowY + 6, (a << 24) | 0xFFFFFF);

            String pts = Component.translatable("xiidays.hud.scoreboard_points", entry.points()).getString();
            int ptsX = panelX + panelW - pad - font.width(pts);
            drawText(graphics, font, pts, ptsX, rowY + 6, a);

            int iconX = ptsX - font.width(" ▲") - 4;
            var battle = ClientRankTracker.getActiveBattleFor(entry.teamName());
            if (battle != null) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(now / 150.0));
                int iconAlpha = (int) (a * pulse);
                int shake = (int) (1 * Math.sin(now / 50.0));
                graphics.drawString(font, "\u2694", iconX + shake, rowY + 6, (iconAlpha << 24) | 0xFFAA00);
            } else {
                var change = ClientRankTracker.getActiveChange(entry.teamName());
                if (change != null) {
                    float progress = change.getProgress();
                    float fadeStart = 0.8f;
                    float arrowFade = progress > fadeStart ? 1.0f - ((progress - fadeStart) / (1.0f - fadeStart)) : 1.0f;
                    arrowFade *= (float) (0.7 + 0.3 * Math.sin(now / 200.0));
                    arrowFade *= alpha;
                    int arrowA = (int) (255 * arrowFade);
                    if (arrowA > 0) {
                        String arrow = change.getType() == ClientRankTracker.RankChangeType.UP ? "\u25B2" : "\u25BC";
                        int col = change.getType() == ClientRankTracker.RankChangeType.UP ? 0x00FF00 : 0xFF0000;
                        int dir = (int) (2 * Math.sin(now / 150.0)) * (change.getType() == ClientRankTracker.RankChangeType.UP ? -1 : 1);
                        graphics.drawString(font, arrow, iconX, rowY + 6 + dir, (arrowA << 24) | col);
                    }
                }
            }

            rowY += rowH;
        }
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
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        graphics.drawString(font, Component.literal(text), -font.width(text) / 2, 0, color);
        pose.popMatrix();
    }

    private static void drawText(GuiGraphics graphics, Font font, String text, int x, int y, int alpha) {
        if (alpha <= 0) return;
        int color = (alpha << 24) | 0xFFFFFF;
        graphics.drawString(font, Component.literal(text), x, y, color);
    }
}
