package com.mceteams.xiidays.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

public class ScoreboardScreen extends Screen {

    // Textures (cherche dans assets/xiidays/textures/gui/)
    private static final ResourceLocation RANK_GOLD =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_gold.png");
    private static final ResourceLocation RANK_SILVER =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_silver.png");
    private static final ResourceLocation RANK_BRONZE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_bronze.png");
    private static final ResourceLocation TEAM_ROW_BG =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team_row.png");
    private static final ResourceLocation PROGRESS_BAR_BG =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/progress_bg.png");
    private static final ResourceLocation ARROW_UP =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/arrow_up.png");
    private static final ResourceLocation ARROW_DOWN =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/arrow_down.png");
    private static final ResourceLocation HEART_ALIVE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/heart_alive.png");
    private static final ResourceLocation HEART_DEAD =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/heart_dead.png");

    private final List<TeamScore> teams;
    private final String playerTeam;

    // Dimensions
    private static final int PANEL_WIDTH = 580;
    private static final int PANEL_HEIGHT = 340;
    private static final int TEAM_ROW_HEIGHT = 55;
    private static final int TEAM_ROW_SPACING = 3;

    public ScoreboardScreen(List<TeamScore> teams, String playerTeam) {
        super(Component.literal("Classement"));
        this.teams = teams;
        this.playerTeam = playerTeam;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // Fond semi-transparent
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE0D0D0D0);

        // Titre
        graphics.drawCenteredString(this.font, "§6§lCLASSEMENT", centerX, panelY + 15, 0xFFFFFF);

        // Lignes d'équipes
        int yOffset = panelY + 50;
        for (int i = 0; i < teams.size(); i++) {
            TeamScore team = teams.get(i);

            if (playerTeam != null && !playerTeam.isEmpty() && !team.teamName.equals(playerTeam)) {
                continue; // Masquer les autres équipes
            }

            renderTeamRow(graphics, panelX, yOffset, team, i + 1);
            yOffset += TEAM_ROW_HEIGHT + TEAM_ROW_SPACING;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTeamRow(GuiGraphics graphics, int x, int y, TeamScore team, int rank) {
        int rowX = x + 15;
        int rowY = y;
        int rowWidth = PANEL_WIDTH - 30;

        // Fond de ligne (texture PNG si disponible, sinon fallback)
        try {
            graphics.blit(TEAM_ROW_BG, rowX, rowY, 0, 0, rowWidth, TEAM_ROW_HEIGHT, rowWidth, TEAM_ROW_HEIGHT);
        } catch (Exception e) {
            // Fallback : fond orange
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + TEAM_ROW_HEIGHT, 0xFFFF8C00);
        }

        // Badge de rang
        renderRankBadge(graphics, rowX + 8, rowY + 8, rank);

        // Nom équipe
        String displayName = shouldShowTeam(team.teamName) ? team.teamName : "ÉQUIPE " + team.uuid;
        graphics.drawString(this.font, "§f§lEQUIPE", rowX + 60, rowY + 12, 0xFFFFFF);
        graphics.drawString(this.font, "§7" + displayName, rowX + 60, rowY + 25, 0xC0C0C0);

        // Barre de progression
        renderProgressBar(graphics, rowX + 250, rowY + 18, team.points);

        // Cœur (vivant/mort)
        renderHeartIcon(graphics, rowX + rowWidth - 35, rowY + 15, team.coreAlive);
    }

    private void renderRankBadge(GuiGraphics graphics, int x, int y, int rank) {
        ResourceLocation texture = switch (rank) {
            case 1 -> RANK_GOLD;
            case 2 -> RANK_SILVER;
            case 3 -> RANK_BRONZE;
            default -> null;
        };

        if (texture != null) {
            try {
                graphics.blit(texture, x, y, 0, 0, 32, 32, 32, 32);
            } catch (Exception e) {
                // Fallback : carré coloré
                int color = switch (rank) {
                    case 1 -> 0xFFFFD700;
                    case 2 -> 0xFFC0C0C0;
                    case 3 -> 0xFFCD7F32;
                    default -> 0xFF808080;
                };
                graphics.fill(x, y, x + 32, y + 32, color);
                graphics.drawCenteredString(this.font, "§l" + rank, x + 16, y + 11, 0x000000);
            }
        } else {
            graphics.fill(x, y, x + 32, y + 32, 0xFF505050);
            graphics.drawCenteredString(this.font, "§l" + rank, x + 16, y + 11, 0xFFFFFF);
        }
    }

    private void renderProgressBar(GuiGraphics graphics, int x, int y, int points) {
        int barWidth = 200;
        int barHeight = 20;

        // Fond de barre
        try {
            graphics.blit(PROGRESS_BAR_BG, x, y, 0, 0, barWidth, barHeight, barWidth, barHeight);
        } catch (Exception e) {
            graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF606060);
        }

        // Remplissage (max 10000 points = 100%)
        int fillWidth = Math.min((int) ((points / 10000.0) * barWidth), barWidth - 4);
        graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + barHeight - 2, 0xFFFFA500);

        // Marqueur rouge vertical (50%)
        int markerX = x + (barWidth / 2);
        graphics.fill(markerX, y, markerX + 2, y + barHeight, 0xFFFF0000);
    }

    private void renderHeartIcon(GuiGraphics graphics, int x, int y, boolean alive) {
        ResourceLocation texture = alive ? HEART_ALIVE : HEART_DEAD;

        try {
            graphics.blit(texture, x, y, 0, 0, 24, 24, 24, 24);
        } catch (Exception e) {
            // Fallback : carré coloré
            int color = alive ? 0xFF00FF00 : 0xFFFF0000;
            graphics.fill(x + 5, y, x + 19, y + 10, color);
            graphics.fill(x, y + 3, x + 24, y + 13, color);
            graphics.fill(x + 3, y + 10, x + 21, y + 17, color);
        }
    }

    private boolean shouldShowTeam(String teamName) {
        if (playerTeam == null || playerTeam.isEmpty()) return true;
        return teamName.equals(playerTeam);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    public static class TeamScore {
        public final String teamName;
        public final String uuid;
        public final int points;
        public final boolean coreAlive;

        public TeamScore(String teamName, String uuid, int points, boolean coreAlive) {
            this.teamName = teamName;
            this.uuid = uuid;
            this.points = points;
            this.coreAlive = coreAlive;
        }
    }
}