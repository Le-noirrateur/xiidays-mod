package com.mceteams.xiidays.client;

import com.mceteams.xiidays.utils.DataManager;
import com.mceteams.xiidays.utils.ScoreboardManager;
import com.mceteams.xiidays.utils.TeamManager;
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

    private static ResourceLocation customBackground = null; // null par défaut

    private static final Set<String> SPECIAL_TEAMS = Set.of("jaune", "vert", "rouge", "bleu");

    private final List<TeamScore> teams;
    private final String playerTeam;

    private static final int PANEL_WIDTH = 580;
    private static final int PANEL_HEIGHT = 400;
    private static final int TEAM_ROW_HEIGHT = 55;
    private static final int TEAM_ROW_SPACING = 5;
    private static final int MAX_VISIBLE_ROWS = 5;
    private static final int CONTENT_START_Y = 60;

    private float scrollOffset = 0;
    private float maxScroll = 0;

    // Méthode pour charger un background
    public void setCustomBackground(String path) {
        if (path != null && !path.isEmpty()) {
            // path = "textures/gui/mon_background.png" par exemple
            customBackground = ResourceLocation.fromNamespaceAndPath(MODID, path);
        } else {
            customBackground = null; // réinitialiser au gris
        }
    }


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

        if (customBackground != null) {
            // Dessiner l'image en fond du panneau
            try {
                graphics.blit(customBackground, panelX, panelY, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
            } catch (Exception e) {
                // fallback au gris
                graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFD0D0D0);
            }
        } else {
            // fond gris par défaut
            graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFD0D0D0);
        }

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

        // Dessiner le fond de la ligne
        try {
            graphics.blit(TEAM_ROW_BG, rowX, rowY, 0, 0, rowWidth, TEAM_ROW_HEIGHT, rowWidth, TEAM_ROW_HEIGHT);
        } catch (Exception e) {
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + TEAM_ROW_HEIGHT, 0xFF505050);
        }

        // Dessiner le badge
        renderRankBadge(graphics, rowX + 10, rowY, rank);
        int badgeWidth = 32;
        int badgeToNameGap = 15;

        // Calculer position du texte
        int nameX = rowX + badgeWidth + badgeToNameGap;
        boolean isPlayerTeam = team.teamName.equalsIgnoreCase(playerTeam);
        boolean useGoldTitle = (rank == 1) && SPECIAL_TEAMS.contains(team.teamName.toLowerCase());

        if (isPlayerTeam && (useGoldTitle || SPECIAL_TEAMS.contains(team.teamName.toLowerCase()))) {
            // Special title
            renderSpecialTitle(graphics, nameX, rowY, team.teamName.toLowerCase(), useGoldTitle);
        } else {
            // Nom normal
            int lines = 2; // titre + sous-titre
            int totalHeight = font.lineHeight * lines;
            int nameY = rowY + (TEAM_ROW_HEIGHT - totalHeight) / 2;

            String displayName = isPlayerTeam ? team.teamName.toUpperCase() : "§kXXXXXXXX";
            graphics.drawString(this.font, "§f§lEQUIPE", nameX, nameY, 0xFFFFFF);
            graphics.drawString(this.font, "§7" + displayName, nameX, nameY + font.lineHeight, 0xC0C0C0);
        }


        // Récupérer l’état du core depuis TeamManager
        boolean alive = true;
        int teamId = TeamManager.getTeamId(team.teamName);
        if (teamId != 0) {
            alive = !DataManager.dataReadBoolean("team_" + teamId + "_config", "team_eliminated", false);
        }

        // Dessiner les cœurs
        int rightPadding = 15; // espace entre bord droit et cœur
        int heartWidth = 24;
        int heartGap = 8; // espace entre points et cœur

        // Calcul de la position du cœur
        int heartX = rowX + rowWidth - rightPadding - heartWidth;
        int heartY = rowY + (TEAM_ROW_HEIGHT - heartWidth) / 2;
        renderHeartIcon(graphics, heartX, heartY, alive);

        // Calcul de la position des points à gauche du cœur
        String pointsText = String.format("§6%d pts", team.points);
        int pointsWidth = this.font.width(pointsText);
        int pointsX = heartX - heartGap - pointsWidth;
        int pointsY = rowY + (TEAM_ROW_HEIGHT - font.lineHeight) / 2;
        graphics.drawString(this.font, pointsText, pointsX, pointsY, 0xFFD700);
    }

    private void renderSpecialTitle(GuiGraphics graphics, int x, int rowY, String teamName, boolean isGold) {
        String suffix = isGold ? "_gold" : "_silver";
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team/" + teamName + suffix + ".png");

        try {
            int textureWidth = 822;  // largeur originale
            int textureHeight = 221; // hauteur originale
            int maxHeight = 32;      // hauteur fixe

            float scale = (float) maxHeight / textureHeight;
            int scaledWidth = (int)(textureWidth * scale); // largeur adaptée pour garder le ratio

            int centeredY = rowY + (TEAM_ROW_HEIGHT - maxHeight)/2;

            graphics.pose().pushPose();
            graphics.pose().translate(x, centeredY, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            graphics.blit(texture, 0, 0, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
            graphics.pose().popPose();

        } catch (Exception e) {
            String color = isGold ? "§6§l" : "§7§l";
            graphics.drawString(this.font, color + teamName.toUpperCase(), x, rowY + (TEAM_ROW_HEIGHT - font.lineHeight) / 2,
                    isGold ? 0xFFD700 : 0xC0C0C0);
        }
    }

    private void renderRankBadge(GuiGraphics graphics, int x, int y, int rank) {
        // Cadres décoratifs (32x32)
        ResourceLocation frameTexture = switch (rank) {
            case 1 -> ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_gold.png");
            case 2 -> ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_silver.png");
            case 3 -> ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_bronze.png");
            default -> null;
        };

        // Chiffres romains (354x354)
        ResourceLocation numeralTexture = switch (rank) {
            case 1 -> ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_numeral_1.png");
            case 2 -> ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_numeral_2.png");
            case 3 -> ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/rank_numeral_3.png");
            default -> null;
        };

        int frameSize = 32;
        int centeredY = y + (TEAM_ROW_HEIGHT - frameSize) / 2;

        // 1. Dessiner le cadre (32x32)
        if (frameTexture != null) {
            try {
                graphics.blit(frameTexture, x, centeredY, 0, 0, frameSize, frameSize, frameSize, frameSize);
            } catch (Exception e) {
                // Fallback : cadre coloré
                int color = switch (rank) {
                    case 1 -> 0xFFFFD700;
                    case 2 -> 0xFFC0C0C0;
                    case 3 -> 0xFFCD7F32;
                    default -> 0xFF808080;
                };
                graphics.fill(x, centeredY, x + frameSize, centeredY + frameSize, color);
            }
        } else {
            // Rang 4+ : cadre gris
            graphics.fill(x, centeredY, x + frameSize, centeredY + frameSize, 0xFF505050);
        }

        // 2. Dessiner le chiffre romain par-dessus (24x24, centré dans le cadre)
        if (numeralTexture != null) {
            try {
                int numeralSize = 24;
                int numeralX = x + (frameSize - numeralSize) / 2; // Centrer horizontalement
                int numeralY = centeredY + (frameSize - numeralSize) / 2; // Centrer verticalement

                int textureSize = 354;

                // Utiliser PoseStack pour scaler
                graphics.pose().pushPose();
                graphics.pose().translate(numeralX, numeralY, 0);

                float scale = (float) numeralSize / textureSize;
                graphics.pose().scale(scale, scale, 1.0f);

                graphics.blit(numeralTexture, 0, 0, 0, 0, textureSize, textureSize, textureSize, textureSize);

                graphics.pose().popPose();

            } catch (Exception e) {
                // Fallback : texte
                String romanNumeral = switch (rank) {
                    case 1 -> "I";
                    case 2 -> "II";
                    case 3 -> "III";
                    default -> String.valueOf(rank);
                };
                graphics.drawCenteredString(this.font, "§l§0" + romanNumeral,
                        x + frameSize / 2,
                        centeredY + frameSize / 2 - 4,
                        0x000000);
            }
        } else {
            // Rang 4+ : afficher le numéro
            graphics.drawCenteredString(this.font, "§l" + rank,
                    x + frameSize / 2,
                    centeredY + frameSize / 2 - 4,
                    0xFFFFFF);
        }
    }

    private void renderFallbackBadge(GuiGraphics graphics, int x, int y, int rank) {
        int color = switch (rank) {
            case 1 -> 0xFFFFD700;
            case 2 -> 0xFFC0C0C0;
            case 3 -> 0xFFCD7F32;
            default -> 0xFF808080;
        };

        // Calculer largeurs proportionnelles pour le fallback aussi
        int[] badgeWidths = {
                (int)(118 * 24.0 / 324),  // I   ≈ 9px
                (int)(232 * 24.0 / 324),  // II  ≈ 17px
                (int)(348 * 24.0 / 324)   // III ≈ 26px
        };

        int badgeWidth = (rank >= 1 && rank <= 3) ? badgeWidths[rank - 1] : 24;
        int badgeHeight = 24;
        int centeredY = y + (TEAM_ROW_HEIGHT - badgeHeight) / 2;

        graphics.fill(x, centeredY, x + badgeWidth, centeredY + badgeHeight, color);

        // Chiffre romain
        String romanNumeral = switch (rank) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(rank);
        };

        // Centrer le texte dans le badge
        graphics.drawCenteredString(this.font, "§l§0" + romanNumeral,
                x + badgeWidth / 2,
                centeredY + badgeHeight / 2 - 4,
                0x000000);
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

    public record TeamScore(String teamName, String uuid, int points, boolean coreAlive, ScoreboardManager.RankChange rankChange) {
    }
}