package com.mceteams.xiidays.screen;

import com.mceteams.xiidays.network.RequestTeamStatsPacket;
import com.mceteams.xiidays.player.ClientRankTracker;
import com.mceteams.xiidays.player.ClientRankTracker.BattleInfo;
import com.mceteams.xiidays.player.ClientRankTracker.RankChangeInfo;
import com.mceteams.xiidays.player.ClientRankTracker.RankChangeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ScoreboardScreen extends Screen {

    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_HEIGHT = 400;
    private static final int ROW_HEIGHT = 28;
    private static final int ROW_SPACING = 2;
    private static final int MAX_VISIBLE = 9;

    private final List<TeamScore> teams;
    private final String playerTeam;
    private float scrollOffset = 0;
    private float maxScroll = 0;

    public ScoreboardScreen(List<TeamScore> teams, String playerTeam) {
        super(Component.literal("Classement"));
        this.teams = teams;
        this.playerTeam = playerTeam;
    }

    @Override
    protected void init() {
        super.init();
        int totalHeight = teams.size() * (ROW_HEIGHT + ROW_SPACING);
        int visibleHeight = MAX_VISIBLE * (ROW_HEIGHT + ROW_SPACING);
        maxScroll = Math.max(0, totalHeight - visibleHeight);
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
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        int centerY = height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // Panel
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A1A2E);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 3, 0xFFFFD700);

        // Title
        graphics.drawCenteredString(font, "§6§lCLASSEMENT", centerX, panelY + 15, 0xFFD700);

        int contentX = panelX + 15;
        int contentY = panelY + 40;
        int contentWidth = PANEL_WIDTH - 30;
        int contentHeight = PANEL_HEIGHT - 60;

        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        int yOffset = contentY - (int) scrollOffset;
        for (int i = 0; i < teams.size(); i++) {
            TeamScore team = teams.get(i);
            if (yOffset + ROW_HEIGHT >= contentY && yOffset <= contentY + contentHeight) {
                renderTeamRow(graphics, contentX, yOffset, contentWidth, team, i + 1, mouseX, mouseY);
            }
            yOffset += ROW_HEIGHT + ROW_SPACING;
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xCC000000);
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    private void renderTeamRow(GuiGraphics graphics, int x, int y, int width, TeamScore team, int rank, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        boolean isPlayer = team.teamName.equalsIgnoreCase(playerTeam);

        if (isPlayer) {
            graphics.fill(x, y, x + width, y + ROW_HEIGHT, 0x40FFD700);
        } else if (hovered) {
            graphics.fill(x, y, x + width, y + ROW_HEIGHT, 0x20FFFFFF);
        }

        // Medal / rank
        String prefix;
        if (rank == 1) prefix = "🥇";
        else if (rank == 2) prefix = "🥈";
        else if (rank == 3) prefix = "🥉";
        else prefix = " §7#" + rank;

        int medalWidth = font.width(prefix);
        graphics.drawString(font, prefix, x + 4, y + 6, 0xFFFFFF);

        // Name
        int nameX = x + 36;
        int nameColor = isPlayer ? 0xFFFFAA : 0xFFFFFF;
        graphics.drawString(font, team.teamName, nameX, y + 6, nameColor);

        // Points
        String pts = "§6" + team.points + " pts";
        int ptsWidth = font.width(" " + team.points + " pts");
        graphics.drawString(font, pts, x + width - ptsWidth - 28, y + 6, 0xFFD700);

        // Heart
        String heart = team.coreAlive() ? "§c❤" : "§8💔";
        graphics.drawString(font, heart, x + width - 18, y + 6, 0xFFFFFF);

        // Battle / rank change
        BattleInfo battle = ClientRankTracker.getActiveBattleFor(team.teamName);
        if (battle != null) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 150.0));
            int alpha = (int) (0xFF * pulse);
            int shake = (int) (1 * Math.sin(System.currentTimeMillis() / 50.0));
            graphics.drawString(font, "⚔", x + width - medalWidth - 38 + shake, y + 6, (alpha << 24) | 0xFFAA00);
        } else {
            RankChangeInfo change = ClientRankTracker.getActiveChange(team.teamName);
            if (change != null) {
                float progress = change.getProgress();
                float fadeStart = 0.8f;
                float alpha = progress > fadeStart ? 1.0f - ((progress - fadeStart) / (1.0f - fadeStart)) : 1.0f;
                alpha *= (float) (0.7 + 0.3 * Math.sin(System.currentTimeMillis() / 200.0));
                String arrow = change.getType() == RankChangeType.UP ? "▲" : "▼";
                int base = change.getType() == RankChangeType.UP ? 0x00FF00 : 0xFF0000;
                int dir = (int) (2 * Math.sin(System.currentTimeMillis() / 150.0)) * (change.getType() == RankChangeType.UP ? -1 : 1);
                graphics.drawString(font, arrow, x + width - medalWidth - 38, y + 6 + dir, ((int) (0xFF * alpha) << 24) | base);
            }
        }

        // Separator line
        graphics.fill(x, y + ROW_HEIGHT - 1, x + width, y + ROW_HEIGHT, 0x10FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = width / 2;
            int centerY = height / 2;
            int panelX = centerX - PANEL_WIDTH / 2;
            int panelY = centerY - PANEL_HEIGHT / 2;
            int contentX = panelX + 15;
            int contentY = panelY + 40;
            int contentWidth = PANEL_WIDTH - 30;

            int yOffset = contentY - (int) scrollOffset;
            for (int i = 0; i < teams.size(); i++) {
                TeamScore team = teams.get(i);
                int rowTop = yOffset;
                int rowBottom = yOffset + ROW_HEIGHT;

                if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
                        mouseY >= Math.max(rowTop, contentY) &&
                        mouseY <= Math.min(rowBottom, contentY + MAX_VISIBLE * (ROW_HEIGHT + ROW_SPACING))) {

                    if (team.teamName.equalsIgnoreCase(playerTeam)) {
                        PacketDistributor.sendToServer(new RequestTeamStatsPacket(team.teamName));
                        return true;
                    }
                }
                yOffset += ROW_HEIGHT + ROW_SPACING;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    public record TeamScore(String teamName, String uuid, int points, boolean coreAlive) {
    }
}
