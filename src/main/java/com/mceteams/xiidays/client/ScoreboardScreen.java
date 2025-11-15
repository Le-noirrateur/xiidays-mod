package com.mceteams.xiidays.client;

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
    private static final ResourceLocation HEART_RED =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/arrow_down.png");
    private static final ResourceLocation HEART_GREEN =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/arrow_up.png");

    private final List<TeamScore> teams;
    private final String playerTeam;

    // Dimensions de l'interface (basées sur ton image)
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
        // Fond transparent sombre
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // Fond du panneau (semi-transparent)
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE0D0D0D0);

        // Titre "CLASSEMENT" (intégré au background, position haute)
        graphics.drawCenteredString(this.font, "§6§lCLASSEMENT", centerX, panelY + 15, 0xFFFFFF);

        // Rendu des équipes
        int yOffset = panelY + 50;
        for (int i = 0; i < teams.size(); i++) {
            TeamScore team = teams.get(i);

            // Masquer les autres équipes si le joueur n'en fait pas partie
            if (!team.teamName.equals(playerTeam)) {
                if (!shouldShowTeam(team.teamName)) {
                    continue; // Skip cette équipe
                }
            }

            renderTeamRow(graphics, panelX, yOffset, team, i + 1);
            yOffset += TEAM_ROW_HEIGHT + TEAM_ROW_SPACING;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTeamRow(GuiGraphics graphics, int x, int y, TeamScore team, int rank) {
        int rowX = x + 20;
        int rowY = y;

        // Fond de la ligne (gris clair/foncé selon rang)
        int bgColor = rank == 1 ? 0xC0505050 : 0xA0606060;
        graphics.fill(rowX, rowY, rowX + PANEL_WIDTH - 40, rowY + TEAM_ROW_HEIGHT, bgColor);

        // Badge de rang (or/argent/bronze)
        renderRankBadge(graphics, rowX + 10, rowY + 15, rank);

        // Nom de l'équipe (masqué ou UUID)
        String displayName = shouldShowTeam(team.teamName) ? team.teamName : "ÉQUIPE " + team.uuid;
        graphics.drawString(this.font, "§f§l" + displayName, rowX + 60, rowY + 10, 0xFFFFFF);

        // Score
        graphics.drawString(this.font, "§6" + team.points, rowX + 60, rowY + 35, 0xFFD700);

        // Indicateur cœur (vert = vivant, rouge = détruit)
        int heartX = rowX + PANEL_WIDTH - 100;
        int heartY = rowY + 20;
        renderHeartIcon(graphics, heartX, heartY, team.coreAlive);
    }

    private void renderRankBadge(GuiGraphics graphics, int x, int y, int rank) {
        // Badge circulaire avec numéro
        int color = switch (rank) {
            case 1 -> 0xFFFFD700; // Or
            case 2 -> 0xFFC0C0C0; // Argent
            case 3 -> 0xFFCD7F32; // Bronze
            default -> 0xFF808080; // Gris
        };

        graphics.fill(x, y, x + 30, y + 30, color);
        graphics.drawCenteredString(this.font, "§l" + rank, x + 15, y + 10, 0x000000);
    }

    private void renderHeartIcon(GuiGraphics graphics, int x, int y, boolean alive) {
        // Icône cœur simplifié (vert ou rouge)
        int color = alive ? 0xFF00FF00 : 0xFFFF0000;
        graphics.fill(x + 5, y, x + 15, y + 10, color);
        graphics.fill(x, y + 3, x + 20, y + 13, color);
        graphics.fill(x + 3, y + 10, x + 17, y + 17, color);
    }

    private boolean shouldShowTeam(String teamName) {
        // Si le joueur n'a pas d'équipe, montrer toutes
        if (playerTeam == null) return true;

        // Montrer uniquement l'équipe du joueur
        return teamName.equals(playerTeam);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Ne pas mettre le jeu en pause
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // Classe de données pour une équipe
        public record TeamScore(String teamName, String uuid, int points, boolean coreAlive) {
    }
}