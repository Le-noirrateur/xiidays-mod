package com.mceteams.xiidays.client;

import com.mceteams.xiidays.client.ClientRankTracker.BattleInfo;
import com.mceteams.xiidays.client.ClientRankTracker.RankChangeInfo;
import com.mceteams.xiidays.client.ClientRankTracker.RankChangeType;
import com.mceteams.xiidays.network.RequestTeamStatsPacket;
import com.mceteams.xiidays.utils.DataManager;
import com.mceteams.xiidays.utils.TeamManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import static com.mceteams.xiidays.XIIDays.MODID;

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

    @Override
    protected void renderBlurredBackground(float partialTick) {
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
        graphics.fill(0, 0, this.width, this.height, 0xC0101010); // Fond noir semi-transparent

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        if (customBackground != null) {
            try {
                graphics.blit(customBackground, panelX, panelY, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
            } catch (Exception e) {
                graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFD0D0D0);
            }
        } else {
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

        // Vérifier si cette équipe est en bataille avec une autre
        BattleInfo battle = ClientRankTracker.getActiveBattleFor(team.teamName);
        boolean inBattle = battle != null;

        // Dessiner le fond de la ligne (surbrillance si en bataille)
        if (inBattle) {
            // Fond en surbrillance pour les équipes en bataille
            long time = System.currentTimeMillis();
            float pulse = (float) (0.6 + 0.4 * Math.sin(time / 300.0));
            int alpha = (int) (0x40 * pulse);
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + TEAM_ROW_HEIGHT, (alpha << 24) | 0xFFAA00);
        }

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

        // Récupérer l'état du core depuis TeamManager
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

        // Afficher l'icône de bataille ou la flèche de changement
        if (inBattle) {
            renderBattleIcon(graphics, pointsX - 25, pointsY - 3);
        } else {
            // Afficher la flèche de changement de rang depuis le tracker client
            RankChangeInfo changeInfo = ClientRankTracker.getActiveChange(team.teamName);
            if (changeInfo != null) {
                renderRankChangeArrow(graphics, pointsX - 20, pointsY, changeInfo);
            }
        }
    }

    /**
     * Affiche une flèche animée indiquant le changement de rang
     * Utilise le ClientRankTracker pour la durée et le fade-out
     */
    private void renderRankChangeArrow(GuiGraphics graphics, int x, int y, RankChangeInfo changeInfo) {
        long time = System.currentTimeMillis();

        // Calculer l'opacité en fonction du temps restant (fade-out sur la dernière seconde)
        float progress = changeInfo.getProgress();
        float fadeStart = 0.8f; // Commence le fade à 80% du temps
        float alpha;
        if (progress > fadeStart) {
            // Fade-out progressif
            alpha = 1.0f - ((progress - fadeStart) / (1.0f - fadeStart));
        } else {
            alpha = 1.0f;
        }

        // Animation de pulsation
        float pulse = (float) (0.7 + 0.3 * Math.sin(time / 200.0));
        alpha *= pulse;

        String arrow;
        int baseColor;

        if (changeInfo.getType() == RankChangeType.UP) {
            arrow = "▲"; // Flèche vers le haut
            baseColor = 0x00FF00; // Vert
        } else {
            arrow = "▼"; // Flèche vers le bas
            baseColor = 0xFF0000; // Rouge
        }

        int color = ((int) (0xFF * alpha) << 24) | baseColor;

        // Petit mouvement vertical pour l'animation
        int animOffset = (int) (2 * Math.sin(time / 150.0));
        int finalY = y + (changeInfo.getType() == RankChangeType.UP ? -animOffset : animOffset);

        graphics.drawString(this.font, arrow, x, finalY, color);
    }

    /**
     * Affiche l'icône de bataille entre deux équipes (épées croisées)
     */
    private void renderBattleIcon(GuiGraphics graphics, int x, int y) {
        long time = System.currentTimeMillis();

        // Animation de pulsation plus intense pour la bataille
        float pulse = (float) (0.5 + 0.5 * Math.sin(time / 150.0));
        int alpha = (int) (0xFF * pulse);

        // Symbole de bataille (épées croisées) - utilise ⚔ ou X stylisé
        String battleSymbol = "⚔";
        int color = (alpha << 24) | 0xFFAA00; // Orange

        // Léger tremblement horizontal
        int shake = (int) (1 * Math.sin(time / 50.0));

        graphics.drawString(this.font, battleSymbol, x + shake, y, color);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Clic gauche
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int panelX = centerX - PANEL_WIDTH / 2;
            int panelY = centerY - PANEL_HEIGHT / 2;

            int contentY = panelY + CONTENT_START_Y;
            int rowX = panelX + 20;
            int rowWidth = PANEL_WIDTH - 40;

            // Vérifier si le clic est dans la zone des équipes
            int yOffset = contentY - (int) scrollOffset;
            for (int i = 0; i < teams.size(); i++) {
                TeamScore team = teams.get(i);
                int rowTop = yOffset;
                int rowBottom = yOffset + TEAM_ROW_HEIGHT;

                // Vérifier si le clic est sur cette ligne (en tenant compte du scissor)
                if (mouseX >= rowX && mouseX <= rowX + rowWidth &&
                        mouseY >= Math.max(rowTop, contentY) &&
                        mouseY <= Math.min(rowBottom, contentY + MAX_VISIBLE_ROWS * (TEAM_ROW_HEIGHT + TEAM_ROW_SPACING))) {

                    // Seul le joueur peut voir les stats de son équipe
                    if (team.teamName.equalsIgnoreCase(playerTeam)) {
                        // Demander les stats détaillées au serveur
                        PacketDistributor.sendToServer(new RequestTeamStatsPacket(team.teamName));
                        return true;
                    }
                }

                yOffset += TEAM_ROW_HEIGHT + TEAM_ROW_SPACING;
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

    /**
     * Données d'une équipe pour l'affichage
     * Note: le changement de rang est géré par ClientRankTracker, pas ici
     */
    public record TeamScore(String teamName, String uuid, int points, boolean coreAlive) {
    }
}