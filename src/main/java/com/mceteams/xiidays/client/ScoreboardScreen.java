package com.mceteams.xiidays.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

public class ScoreboardScreen extends Screen {

    // Textures
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

        // Note : Le titre "CLASSEMENT" sera dans le fond d'écran

        // Lignes d'équipes
        int yOffset = panelY + 50;
        for (int i = 0; i < teams.size(); i++) {
            TeamScore team = teams.get(i);

            if (playerTeam != null && !playerTeam.isEmpty() && !team.teamName.equals(playerTeam)) {
                continue;
            }

            renderTeamRow(graphics, panelX, yOffset, team, i + 1);
            yOffset += TEAM_ROW_HEIGHT + TEAM_ROW_SPACING;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTeamRow(GuiGraphics graphics, int x, int rowY, TeamScore team, int rank) {
        int rowX = x + 15;
        int rowWidth = PANEL_WIDTH - 30;

        // Fond de ligne (texture ou fallback)
        try {
            RenderSystem.setShaderTexture(0, TEAM_ROW_BG);
            RenderSystem.enableBlend();
            graphics.blit(TEAM_ROW_BG, rowX, rowY, 0, 0, rowWidth, TEAM_ROW_HEIGHT, rowWidth, TEAM_ROW_HEIGHT);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            // Fallback : fond gris foncé avec bordure
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + TEAM_ROW_HEIGHT, 0xFF3A3A3A);
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + 2, 0xFF5A5A5A); // bordure haut
            graphics.fill(rowX, rowY + TEAM_ROW_HEIGHT - 2, rowX + rowWidth, rowY + TEAM_ROW_HEIGHT, 0xFF2A2A2A); // bordure basse
        }

        // Badge de rang (à gauche)
        renderRankBadge(graphics, rowX + 10, rowY + 12, rank);

        // Texte "EQUIPE" et UUID avec police custom
        int textY = rowY + (TEAM_ROW_HEIGHT / 2) - 8;

        String displayName = shouldShowTeam(team.teamName) ? team.teamName : formatUUID(team.uuid);

        // "EQUIPE" en orange
        FontTextureRenderer.drawGoldText(graphics, "EQUIPE", rowX + 60, textY, 1.0f);

        // UUID/Nom en gris
        FontTextureRenderer.drawGrayText(graphics, displayName, rowX + 160, textY, 1.0f);

        // Points affichés avec police custom
        String pointsText = String.format("%d", team.points);
        int pointsX = rowX + rowWidth - 150;

        FontTextureRenderer.drawGoldText(graphics, pointsText, pointsX, textY, 1.0f);

        // Cœur (tout à droite)
        renderHeartIcon(graphics, rowX + rowWidth - 35, rowY + 15, team.coreAlive);
    }

    /**
     * Vérifie si on doit afficher le vrai nom de l'équipe
     * @return true si c'est l'équipe du joueur, false sinon (à offusquer)
     */
    private boolean shouldShowTeam(String teamName) {
        if (playerTeam == null || playerTeam.isEmpty()) return false; // Aucune équipe = tout offusqué
        return teamName.equals(playerTeam); // true si c'est l'équipe du joueur
    }

    // Formate l'UUID en format court (8 premiers caractères)
    private String formatUUID(String uuid) {
        if (uuid == null || uuid.length() < 8) return "INCONNU";
        return uuid.substring(0, 8).toUpperCase();
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
                RenderSystem.setShaderTexture(0, texture);
                RenderSystem.enableBlend();
                // Taille 32x32 pour le badge
                graphics.blit(texture, x, y, 0, 0, 32, 32, 32, 32);
                RenderSystem.disableBlend();
            } catch (Exception e) {
                // Fallback : carré coloré avec numéro
                renderFallbackBadge(graphics, x, y, rank);
            }
        } else {
            // Rang > 3 : badge gris simple
            renderFallbackBadge(graphics, x, y, rank);
        }
    }

    private void renderFallbackBadge(GuiGraphics graphics, int x, int y, int rank) {
        // Couleurs selon le rang
        int bgColor = switch (rank) {
            case 1 -> 0xFFFFD700; // Or
            case 2 -> 0xFFC0C0C0; // Argent
            case 3 -> 0xFFCD7F32; // Bronze
            default -> 0xFF4A4A4A; // Gris foncé
        };

        // Fond du badge
        graphics.fill(x, y, x + 32, y + 32, bgColor);

        // Bordure plus foncée
        int borderColor = (bgColor & 0xFEFEFE) >> 1; // Assombrir
        graphics.fill(x, y, x + 32, y + 1, borderColor); // haut
        graphics.fill(x, y, x + 1, y + 32, borderColor); // gauche
        graphics.fill(x + 31, y, x + 32, y + 32, borderColor); // droite
        graphics.fill(x, y + 31, x + 32, y + 32, borderColor); // bas

        // Numéro centré
        String rankText = String.valueOf(rank);
        int textX = x + 16 - (this.font.width(rankText) / 2);
        int textY = y + 12;

        // Ombre du texte
        graphics.drawString(this.font, rankText, textX + 1, textY + 1, 0xFF000000);
        // Texte principal
        graphics.drawString(this.font, "§l" + rankText, textX, textY, 0xFFFFFFFF);
    }

    private void renderProgressBar(GuiGraphics graphics, int x, int y, int points) {
        int barWidth = 200;
        int barHeight = 20;

        // Fond de la barre (texture ou fallback)
        try {
            RenderSystem.setShaderTexture(0, PROGRESS_BAR_BG);
            RenderSystem.enableBlend();
            graphics.blit(PROGRESS_BAR_BG, x, y, 0, 0, barWidth, barHeight, barWidth, barHeight);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            // Fallback : fond gris avec bordure
            graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF2A2A2A);
            graphics.fill(x, y, x + barWidth, y + 1, 0xFF4A4A4A); // bordure haut
            graphics.fill(x, y + barHeight - 1, x + barWidth, y + barHeight, 0xFF1A1A1A); // bordure basse
        }

        // Remplissage orange/jaune (proportionnel aux points, max = 10000)
        int maxPoints = 10000;
        int fillWidth = Math.min((int) ((points / (double) maxPoints) * (barWidth - 4)), barWidth - 4);

        // Dégradé orange → jaune
        int orangeColor = 0xFFFF8C00;
        graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + barHeight - 2, orangeColor);

        // Marqueur rouge vertical à 50%
        int markerX = x + (barWidth / 2);
        graphics.fill(markerX - 1, y, markerX + 1, y + barHeight, 0xFFDC143C); // Rouge crimson

        // Texte des points (centré dans la barre)
        String pointsText = String.format("%d", points);
        int textX = x + (barWidth / 2) - (this.font.width(pointsText) / 2);
        int textY = y + (barHeight / 2) - 4;

        // Ombre du texte
        graphics.drawString(this.font, pointsText, textX + 1, textY + 1, 0xFF000000);
        // Texte principal
        graphics.drawString(this.font, "§f§l" + pointsText, textX, textY, 0xFFFFFFFF);
    }

    private void renderHeartIcon(GuiGraphics graphics, int x, int y, boolean alive) {
        ResourceLocation texture = alive ? HEART_ALIVE : HEART_DEAD;

        try {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            // Taille 24x24 pour le cœur
            graphics.blit(texture, x, y, 0, 0, 24, 24, 24, 24);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            // Fallback : cœur pixelisé
            int heartColor = alive ? 0xFF00FF00 : 0xFFFF0000; // Vert ou rouge

            // Forme de cœur en pixels (8-bit style)
            // Haut du cœur (2 bosses)
            graphics.fill(x + 6, y + 2, x + 10, y + 6, heartColor);
            graphics.fill(x + 14, y + 2, x + 18, y + 6, heartColor);

            // Centre large
            graphics.fill(x + 2, y + 6, x + 22, y + 14, heartColor);

            // Bas pointu
            graphics.fill(x + 4, y + 14, x + 20, y + 18, heartColor);
            graphics.fill(x + 6, y + 18, x + 18, y + 20, heartColor);
            graphics.fill(x + 8, y + 20, x + 16, y + 22, heartColor);
            graphics.fill(x + 10, y + 22, x + 14, y + 24, heartColor);

            // Reflet blanc (effet brillant)
            if (alive) {
                graphics.fill(x + 8, y + 6, x + 10, y + 8, 0xFFFFFFFF);
            }
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

    public record TeamScore(String teamName, String uuid, int points, boolean coreAlive) {
    }
}