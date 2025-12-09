package com.mceteams.xiidays.menus;

import com.mceteams.xiidays.network.OpenTeamStatsPacket.PlayerStatsData;
import com.mceteams.xiidays.network.OpenTeamStatsPacket.TeamStatsData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Écran affichant les statistiques détaillées d'une équipe
 */
public class TeamStatsScreen extends Screen {

    private final TeamStatsData teamStats;
    private final List<PlayerStatsData> playerStats;

    private static final int PANEL_WIDTH = 500;
    private static final int PANEL_HEIGHT = 400;

    private float scrollOffset = 0;
    private float maxScroll = 0;

    public TeamStatsScreen(TeamStatsData teamStats, List<PlayerStatsData> playerStats) {
        super(Component.literal("Stats - " + teamStats.teamName()));
        this.teamStats = teamStats;
        this.playerStats = playerStats;
    }

    @Override
    protected void init() {
        super.init();
        // Calculer le scroll max basé sur le nombre de joueurs
        int contentHeight = 200 + (playerStats.size() * 60);
        maxScroll = Math.max(0, contentHeight - (PANEL_HEIGHT - 100));
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
        // Fond semi-transparent
        graphics.fill(0, 0, this.width, this.height, 0xCC000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // Panneau principal
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A1A2E);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 3, 0xFFFFD700); // Bordure or

        // Titre
        graphics.drawCenteredString(this.font, "§6§lSTATISTIQUES D'ÉQUIPE", centerX, panelY + 15, 0xFFD700);
        graphics.drawCenteredString(this.font, "§e" + teamStats.teamName().toUpperCase(), centerX, panelY + 30, 0xFFFFFF);

        // Zone scrollable
        int contentY = panelY + 50;
        int contentHeight = PANEL_HEIGHT - 70;
        graphics.enableScissor(panelX + 10, contentY, panelX + PANEL_WIDTH - 10, contentY + contentHeight);

        int yOffset = contentY - (int) scrollOffset;

        // === STATS DE L'ÉQUIPE ===
        graphics.drawString(this.font, "§6§l▸ STATISTIQUES GLOBALES", panelX + 20, yOffset, 0xFFD700);
        yOffset += 20;

        // Grille de stats (2 colonnes)
        int col1X = panelX + 30;
        int col2X = panelX + 260;
        int lineHeight = 14;

        graphics.drawString(this.font, "§7Points totaux: §f" + teamStats.points(), col1X, yOffset, 0xFFFFFF);
        graphics.drawString(this.font, "§7Points gagnés: §a+" + teamStats.pointGain(), col2X, yOffset, 0x00FF00);
        yOffset += lineHeight;

        graphics.drawString(this.font, "§7Kills: §c" + teamStats.kills(), col1X, yOffset, 0xFF5555);
        graphics.drawString(this.font, "§7Points perdus: §c-" + teamStats.pointLoss(), col2X, yOffset, 0xFF5555);
        yOffset += lineHeight;

        graphics.drawString(this.font, "§7Morts: §8" + teamStats.deaths(), col1X, yOffset, 0x888888);
        graphics.drawString(this.font, "§7K/D Ratio: §e" + formatKD(teamStats.kills(), teamStats.deaths()), col2X, yOffset, 0xFFFF55);
        yOffset += lineHeight;

        graphics.drawString(this.font, "§7Blocs minés: §b" + teamStats.blocksMined(), col1X, yOffset, 0x55FFFF);
        graphics.drawString(this.font, "§7Dégâts infligés: §4" + teamStats.damageDealt(), col2X, yOffset, 0xAA0000);
        yOffset += lineHeight;

        graphics.drawString(this.font, "§7Dégâts reçus: §6" + teamStats.damageReceived(), col1X, yOffset, 0xFFAA00);
        yOffset += 30;

        // === STATS DES JOUEURS ===
        graphics.drawString(this.font, "§6§l▸ JOUEURS DE L'ÉQUIPE (" + playerStats.size() + ")", panelX + 20, yOffset, 0xFFD700);
        yOffset += 25;

        // En-tête du tableau
        graphics.fill(panelX + 20, yOffset - 2, panelX + PANEL_WIDTH - 20, yOffset + 12, 0x40FFFFFF);
        graphics.drawString(this.font, "§fJoueur", panelX + 25, yOffset, 0xFFFFFF);
        graphics.drawString(this.font, "§fPts", panelX + 150, yOffset, 0xFFFFFF);
        graphics.drawString(this.font, "§fKills", panelX + 210, yOffset, 0xFFFFFF);
        graphics.drawString(this.font, "§fMorts", panelX + 270, yOffset, 0xFFFFFF);
        graphics.drawString(this.font, "§fK/D", panelX + 330, yOffset, 0xFFFFFF);
        graphics.drawString(this.font, "§fBlocs", panelX + 390, yOffset, 0xFFFFFF);
        yOffset += 18;

        // Ligne de séparation
        graphics.fill(panelX + 20, yOffset - 3, panelX + PANEL_WIDTH - 20, yOffset - 2, 0xFFFFD700);

        // Liste des joueurs
        for (int i = 0; i < playerStats.size(); i++) {
            PlayerStatsData player = playerStats.get(i);

            // Fond alterné
            if (i % 2 == 0) {
                graphics.fill(panelX + 20, yOffset - 2, panelX + PANEL_WIDTH - 20, yOffset + 14, 0x20FFFFFF);
            }

            // Médaille pour le MVP de l'équipe (celui avec le plus de points)
            String prefix = "";
            if (i == 0 && playerStats.size() > 1) {
                prefix = "§6★ "; // Étoile pour le meilleur
            }

            graphics.drawString(this.font, prefix + "§f" + truncate(player.playerName(), 12), panelX + 25, yOffset, 0xFFFFFF);
            graphics.drawString(this.font, "§e" + player.points(), panelX + 150, yOffset, 0xFFFF55);
            graphics.drawString(this.font, "§c" + player.kills(), panelX + 210, yOffset, 0xFF5555);
            graphics.drawString(this.font, "§8" + player.deaths(), panelX + 270, yOffset, 0x888888);
            graphics.drawString(this.font, "§a" + formatKD(player.kills(), player.deaths()), panelX + 330, yOffset, 0x55FF55);
            graphics.drawString(this.font, "§b" + player.blocksMined(), panelX + 390, yOffset, 0x55FFFF);

            yOffset += 18;
        }

        graphics.disableScissor();

        // Indicateur de scroll si nécessaire
        if (maxScroll > 0) {
            int scrollBarHeight = PANEL_HEIGHT - 70;
            int scrollBarX = panelX + PANEL_WIDTH - 15;
            int scrollBarY = panelY + 50;

            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarHeight, 0x40FFFFFF);

            float scrollPercent = scrollOffset / maxScroll;
            int barSize = Math.max(20, (int) ((float) scrollBarHeight / (scrollBarHeight + maxScroll) * scrollBarHeight));
            int barY = scrollBarY + (int) (scrollPercent * (scrollBarHeight - barSize));

            graphics.fill(scrollBarX, barY, scrollBarX + 6, barY + barSize, 0xFFFFD700);
        }

        // Instructions
        graphics.drawCenteredString(this.font, "§8[ESC pour fermer]", centerX, panelY + PANEL_HEIGHT - 15, 0x555555);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String formatKD(int kills, int deaths) {
        if (deaths == 0) return kills > 0 ? "∞" : "0.00";
        return String.format("%.2f", (double) kills / deaths);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
