package com.mceteams.xiidays.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

public class ScoreboardScreen extends Screen {

    private static final ResourceLocation RANK_GOLD = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_gold.png");
    private static final ResourceLocation RANK_SILVER = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_silver.png");
    private static final ResourceLocation RANK_BRONZE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_bronze.png");
    private static final ResourceLocation TEAM_ROW_BG = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team_row.png");
    private static final ResourceLocation HEART_ALIVE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/heart_alive.png");
    private static final ResourceLocation HEART_DEAD = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/heart_dead.png");
    private static final ResourceLocation ARROW_UP = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/arrow_up.png");
    private static final ResourceLocation ARROW_DOWN = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/arrow_down.png");

    private static final Set<String> SPECIAL_TEAMS = Set.of("jaune", "vert", "rouge", "bleu");

    private final List<TeamScore> teams;
    private final String playerTeam;

    private static final int PANEL_WIDTH = 580;
    private static final int PANEL_HEIGHT = 400;
    private static final int TEAM_ROW_HEIGHT = 65;
    private static final int TEAM_ROW_SPACING = 5;
    private static final int MAX_VISIBLE_ROWS = 5;
    private static final int CONTENT_START_Y = 60;

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
        int totalHeight = teams.size() * (TEAM_ROW_HEIGHT + TEAM_ROW_SPACING);
        int visibleHeight = MAX_VISIBLE_ROWS * (TEAM_ROW_HEIGHT + TEAM_ROW_SPACING);
        maxScroll = Math.max(0, totalHeight - visibleHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (float) scrollY * 30, 0, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE0D0D0D0);
        graphics.drawCenteredString(this.font, "§6§lCLASSEMENT", centerX, panelY + 20, 0xFFFFFF);

        int contentY = panelY + CONTENT_START_Y;
        int contentHeight = MAX_VISIBLE_ROWS * (TEAM_ROW_HEIGHT + TEAM_ROW_SPACING);

        graphics.enableScissor(panelX + 10, contentY, panelX + PANEL_WIDTH - 10, contentY + contentHeight);

        int yOffset = contentY - (int) scrollOffset;
        for (int i = 0; i < teams.size(); i++) {
            TeamScore team = teams.get(i);
            if (yOffset + TEAM_ROW_HEIGHT >= contentY && yOffset <= contentY + contentHeight) {
                renderTeamRow(graphics, panelX, yOffset, team, i + 1);
            }
            yOffset += TEAM_ROW_HEIGHT + TEAM_ROW_SPACING;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            renderScrollbar(graphics, panelX + PANEL_WIDTH - 15, contentY, contentHeight);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height) {
        graphics.fill(x, y, x + 6, y + height, 0x80000000);
        float scrollPercent = scrollOffset / maxScroll;
        int barHeight = Math.max(20, (height / (teams.size() * (TEAM_ROW_HEIGHT + TEAM_ROW_SPACING))) * height);
        int barY = y + (int) (scrollPercent * (height - barHeight));
        graphics.fill(x, barY, x + 6, barY + barHeight, 0xFFC0C0C0);
    }

    private void renderTeamRow(GuiGraphics graphics, int x, int rowY, TeamScore team, int rank) {
        int rowX = x + 20;
        int rowWidth = PANEL_WIDTH - 40;

        try {
            graphics.blit(TEAM_ROW_BG, rowX, rowY, 0, 0, rowWidth, TEAM_ROW_HEIGHT, rowWidth, TEAM_ROW_HEIGHT);
        } catch (Exception e) {
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + TEAM_ROW_HEIGHT, 0xFF505050);
        }

        renderRankBadge(graphics, rowX + 10, rowY + 16, rank);

        // Flèches de changement de rang (à côté du badge)
        if (team.rankChange != RankChange.NONE) {
            renderRankChangeArrow(graphics, rowX + 50, rowY + 20, team.rankChange);
        }

        boolean isPlayerTeam = team.teamName.equalsIgnoreCase(playerTeam);
        boolean useGoldTitle = (rank == 1) && SPECIAL_TEAMS.contains(team.teamName.toLowerCase());

        int nameX = rowX + 70;
        int nameY = rowY + 18;

        if (isPlayerTeam && useGoldTitle) {
            renderSpecialTitle(graphics, nameX, nameY, team.teamName.toLowerCase(), true);
        } else if (isPlayerTeam && SPECIAL_TEAMS.contains(team.teamName.toLowerCase())) {
            renderSpecialTitle(graphics, nameX, nameY, team.teamName.toLowerCase(), false);
        } else {
            String displayName = isPlayerTeam ? team.teamName.toUpperCase() : "§kXXXXXXXX";
            graphics.drawString(this.font, "§f§lEQUIPE", nameX, nameY, 0xFFFFFF);
            graphics.drawString(this.font, "§7" + displayName, nameX, nameY + 14, 0xC0C0C0);
        }

        // Points alignés à droite (avant le cœur)
        String pointsText = String.format("§6%d pts", team.points);
        int pointsWidth = this.font.width(pointsText);
        int pointsX = rowX + rowWidth - 70 - pointsWidth; // 70px pour laisser de la place au cœur
        graphics.drawString(this.font, pointsText, pointsX, rowY + 25, 0xFFD700);

        renderHeartIcon(graphics, rowX + rowWidth - 40, rowY + 20, team.coreAlive);
    }

    private void renderRankChangeArrow(GuiGraphics graphics, int x, int y, RankChange change) {
        ResourceLocation texture = (change == RankChange.UP) ? ARROW_UP : ARROW_DOWN;

        try {
            graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
        } catch (Exception e) {
            // Fallback : dessin manuel de flèche
            if (change == RankChange.UP) {
                graphics.drawString(this.font, "§a▲", x, y, 0x00FF00);
            } else {
                graphics.drawString(this.font, "§c▼", x, y, 0xFF0000);
            }
        }
    }

    private void renderSpecialTitle(GuiGraphics graphics, int x, int y, String teamName, boolean isGold) {
        String suffix = isGold ? "_gold" : "_silver";
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team/" + teamName + suffix + ".png");

        try {
            int textureWidth = 128;
            int textureHeight = 32;
            float scale = 1.0f;
            int scaledWidth = (int) (textureWidth * scale);
            int scaledHeight = (int) (textureHeight * scale);
            graphics.blit(texture, x, y, 0, 0, scaledWidth, scaledHeight, textureWidth, textureHeight);
        } catch (Exception e) {
            String color = isGold ? "§6§l" : "§7§l";
            graphics.drawString(this.font, color + teamName.toUpperCase(), x, y, isGold ? 0xFFD700 : 0xC0C0C0);
        }
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

    private void renderHeartIcon(GuiGraphics graphics, int x, int y, boolean alive) {
        ResourceLocation texture = alive ? HEART_ALIVE : HEART_DEAD;
        try {
            graphics.blit(texture, x, y, 0, 0, 24, 24, 24, 24);
        } catch (Exception e) {
            int color = alive ? 0xFF00FF00 : 0xFFFF0000;
            graphics.fill(x + 5, y, x + 19, y + 10, color);
            graphics.fill(x, y + 3, x + 24, y + 13, color);
            graphics.fill(x + 3, y + 10, x + 21, y + 17, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    public enum RankChange {
        UP, DOWN, NONE
    }

    public record TeamScore(String teamName, String uuid, int points, boolean coreAlive, RankChange rankChange) {
    }
}