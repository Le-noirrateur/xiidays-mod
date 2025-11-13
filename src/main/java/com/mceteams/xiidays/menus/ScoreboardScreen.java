package com.mceteams.xiidays.menus;

import com.mceteams.xiidays.utils.ScoreboardManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ScoreboardScreen extends Screen {

    public ScoreboardScreen(Component title) {
        super(title);
    }

    @Override
    public boolean isPauseScreen() {
        // Ne met pas le jeu en pause quand le menu est ouvert
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Rendre le fond de l'écran précédent (optionnel, pour voir le jeu en arrière-plan)
        super.render(graphics, mouseX, mouseY, partialTick);

        // Fond semi-transparent
        graphics.fill(0, 0, width, height, 0x80000000);

        // Titre
        String titleText = "Classement des Équipes";
        int titleWidth = font.width(titleText);
        graphics.drawString(font, titleText, (width - titleWidth) / 2, 20, 0xFFFFFF);

        List<ScoreboardManager.TeamRankingEntry> rankings = ScoreboardManager.getRankings();

        // Si aucune équipe, afficher un message
        if (rankings.isEmpty()) {
            String noTeamText = "Aucune équipe disponible";
            int textWidth = font.width(noTeamText);
            graphics.drawString(font, noTeamText, (width - textWidth) / 2, height / 2, 0xFF5555);
            return;
        }

        int startY = 50;
        int barHeight = 30;

        for (int i = 0; i < rankings.size(); i++) {
            ScoreboardManager.TeamRankingEntry entry = rankings.get(i);
            int y = startY + (i * (barHeight + 5));

            // Couleur selon le rang
            int color = getRankColor(entry.currentRank());

            // Barre de l'équipe
            graphics.fill(50, y, width - 50, y + barHeight, color);

            // Médaille
            String medal = getRankMedal(entry.currentRank());
            graphics.drawString(font, medal, 60, y + 10, 0xFFFFFF);

            // Nom équipe
            graphics.drawString(font, entry.teamName(), 100, y + 10, 0xFFFFFF);

            // Points
            graphics.drawString(font, entry.points() + " pts", width - 150, y + 10, 0xFFD700);

            // Flèche si récent changement
            if (ScoreboardManager.shouldShowArrow(entry)) {
                String arrow = entry.change() == ScoreboardManager.RankChange.UP ? "⬆" : "⬇";
                int arrowColor = entry.change() == ScoreboardManager.RankChange.UP ? 0x00FF00 : 0xFF0000;
                graphics.drawString(font, arrow, width - 80, y + 10, arrowColor);
            }
        }
    }

    /**
     * Retourne l'emoji de médaille selon le rang
     * @param rank Position dans le classement (1, 2, 3, etc.)
     * @return Emoji de médaille ou numéro de rang
     */
    private String getRankMedal(int rank) {
        return switch(rank) {
            case 1 -> "🥇"; // Médaille d'or
            case 2 -> "🥈"; // Médaille d'argent
            case 3 -> "🥉"; // Médaille de bronze
            default -> "#" + rank; // Affiche le numéro de rang pour les autres
        };
    }

    /**
     * Retourne la couleur de fond selon le rang
     * @param rank Position dans le classement
     * @return Couleur ARGB
     */
    private int getRankColor(int rank) {
        return switch(rank) {
            case 1 -> 0xFFFFD700; // Or
            case 2 -> 0xFFC0C0C0; // Argent
            case 3 -> 0xFFCD7F32; // Bronze
            default -> 0xFF404040; // Gris
        };
    }
}