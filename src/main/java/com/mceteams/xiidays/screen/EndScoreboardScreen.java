package com.mceteams.xiidays.screen;

import com.mceteams.xiidays.network.OpenEndScoreboardPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EndScoreboardScreen extends Screen {

    private static final int PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 420;

    private final String winningTeam;
    private final OpenEndScoreboardPacket.MvpData mvpData;
    private final List<OpenEndScoreboardPacket.TeamEntry> teamStatsList;
    private float scrollOffset = 0;
    private float maxScroll = 0;

    public EndScoreboardScreen(String winningTeam, List<OpenEndScoreboardPacket.TeamEntry> teamStats, OpenEndScoreboardPacket.MvpData mvpData) {
        super(Component.literal("Fin de Partie"));
        this.winningTeam = winningTeam;
        this.mvpData = mvpData;
        this.teamStatsList = teamStats;
    }

    @Override
    protected void init() {
        super.init();
        int contentHeight = 230 + teamStatsList.size() * 22;
        maxScroll = Math.max(0, contentHeight - (PANEL_HEIGHT - 120));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (float) scrollY * 20, 0, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xCC000000);

        int centerX = width / 2;
        int centerY = height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A1A2E);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 3, 0xFFFFD700);

        graphics.drawCenteredString(font, "§6§lFIN DE PARTIE", centerX, panelY + 15, 0xFFD700);

        int contentX = panelX + 15;
        int contentY = panelY + 40;
        int contentWidth = PANEL_WIDTH - 30;
        int contentHeight = PANEL_HEIGHT - 60;

        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        int yOffset = contentY - (int) scrollOffset;

        // ── WINNER BANNER ──
        graphics.fill(contentX, yOffset, contentX + contentWidth, yOffset + 30, 0x40FFD700);
        graphics.drawCenteredString(font, "\uD83C\uDFC6 §6§l" + winningTeam + " §r§6a gagné la partie !", contentX + contentWidth / 2, yOffset + 8, 0xFFD700);
        yOffset += 40;

        // ── WINNER STATS ──
        int pts = 0, kills = 0, deaths = 0, blocks = 0, damage = 0;
        for (OpenEndScoreboardPacket.TeamEntry t : teamStatsList) {
            if (t.teamName().equals(winningTeam)) {
                pts = t.points();
                kills = t.kills();
                deaths = t.deaths();
                blocks = t.blocksMined();
                damage = t.damageDealt();
                break;
            }
        }
        graphics.drawString(font, "§7Points: §6" + pts + "  §7Kills: §c" + kills + "  §7Morts: §8" + deaths
                + "  §7Blocs: §b" + blocks + "  §7Degats: §4" + damage, contentX + 10, yOffset, 0xAAAAAA);
        yOffset += 20;

        // ── MVP ──
        if (mvpData != null && !mvpData.playerUUID().isEmpty()) {
            graphics.fill(contentX, yOffset, contentX + contentWidth, yOffset + 34, 0x3000FF00);
            graphics.drawString(font, "\u2B50 §6§lMVP §7» §e" + mvpData.playerName()
                    + " §7(§6" + mvpData.pointsContributed() + " pts§7) — Équipe §6" + mvpData.teamName(), contentX + 10, yOffset + 3, 0xFFFFFF);
            String mvpLine = "§7Score: §a" + mvpData.score() + "  §7=  §6" + mvpData.pointsContributed() + " pts"
                    + " §7+ §c" + mvpData.kills() + "K*50 §7- §8" + mvpData.deaths() + "M*10";
            graphics.drawString(font, mvpLine, contentX + 10, yOffset + 15, 0xAAAAAA);
            yOffset += 44;
        }

        // ── RANKING ──
        graphics.drawString(font, "§6§l▸ CLASSEMENT FINAL", contentX + 10, yOffset, 0xFFD700);
        yOffset += 20;

        for (int i = 0; i < teamStatsList.size(); i++) {
            OpenEndScoreboardPacket.TeamEntry team = teamStatsList.get(i);

            String medal;
            String color;
            switch (i) {
                case 0 -> { medal = "\uD83E\uDD47"; color = "§6"; }
                case 1 -> { medal = "\uD83E\uDD48"; color = "§7"; }
                case 2 -> { medal = "\uD83E\uDD49"; color = "§e"; }
                default -> { medal = "  "; color = "§8"; }
            }

            boolean isWinner = team.teamName().equals(winningTeam);
            if (isWinner) {
                graphics.fill(contentX, yOffset - 2, contentX + contentWidth, yOffset + 14, 0x40FFD700);
            } else if (i % 2 == 0) {
                graphics.fill(contentX, yOffset - 2, contentX + contentWidth, yOffset + 14, 0x10FFFFFF);
            }

            graphics.drawString(font, medal + " " + color + "#" + (i + 1) + " §f" + team.teamName()
                    + " §7- §6" + team.points() + " pts", contentX + 10, yOffset, 0xFFFFFF);

            graphics.drawString(font, "§7K:§c" + team.kills() + " §7M:§8" + team.deaths()
                    + " §7B:§b" + team.blocksMined() + " §7D:§4" + team.damageDealt(),
                    contentX + contentWidth - 200, yOffset, 0xAAAAAA);

            yOffset += 20;
        }

        graphics.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int scrollX = panelX + PANEL_WIDTH - 12;
            graphics.fill(scrollX, contentY, scrollX + 4, contentY + contentHeight, 0x40FFFFFF);
            float pct = scrollOffset / maxScroll;
            int barH = Math.max(15, (int) ((float) contentHeight / (contentHeight + maxScroll) * contentHeight));
            int barY = contentY + (int) (pct * (contentHeight - barH));
            graphics.fill(scrollX, barY, scrollX + 4, barY + barH, 0xFFFFD700);
        }

        graphics.drawCenteredString(font, "§8[ESC pour fermer]", centerX, panelY + PANEL_HEIGHT - 12, 0x555555);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
