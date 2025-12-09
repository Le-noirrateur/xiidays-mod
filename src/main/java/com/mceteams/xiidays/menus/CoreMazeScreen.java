package com.mceteams.xiidays.menus;

import com.mceteams.xiidays.network.CoreMazeAnswerPacket;
import com.mceteams.xiidays.utils.EnigmaGenerator;
import com.mceteams.xiidays.utils.EnigmaGenerator.Enigma;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.mceteams.xiidays.XIIDays.MODID;

/**
 * Écran du Core Maze avec 3 énigmes à résoudre
 */
public class CoreMazeScreen extends Screen {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/core_maze_bg.png");

    private final List<EnigmaData> enigmas;
    private final int teamId;
    private int currentEnigmaIndex = 0;
    private int solvedCount = 0;

    private EditBox answerBox;
    private String feedbackMessage = "";
    private int feedbackColor = 0xFFFFFF;
    private long feedbackEndTime = 0;

    private boolean showHint = false;

    // Dimensions
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 300;

    /**
     * @param enigmas Liste des données d'énigmes (question, type, indice)
     * @param teamId ID de l'équipe du joueur
     */
    public CoreMazeScreen(List<EnigmaData> enigmas, int teamId) {
        super(Component.literal("Core Maze"));
        this.enigmas = enigmas;
        this.teamId = teamId;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // Champ de réponse
        this.answerBox = new EditBox(this.font, centerX - 100, panelY + 180, 200, 20, Component.literal("Réponse"));
        this.answerBox.setMaxLength(50);
        this.answerBox.setHint(Component.literal("Entrez votre réponse..."));
        this.answerBox.setResponder(this::onAnswerChanged);
        this.addWidget(this.answerBox);
    }

    private void onAnswerChanged(String text) {
        if (currentEnigmaIndex >= enigmas.size()) return;

        EnigmaData currentEnigma = enigmas.get(currentEnigmaIndex);

        // Vérifier si la réponse est correcte (validation automatique)
        if (text != null && !text.isEmpty()) {
            String expected = currentEnigma.answer().trim().toLowerCase();
            String given = text.trim().toLowerCase();

            if (expected.equals(given)) {
                onCorrectAnswer();
            }
        }
    }

    private void onCorrectAnswer() {
        solvedCount++;
        feedbackMessage = "Correct !";
        feedbackColor = 0x00FF00;
        feedbackEndTime = System.currentTimeMillis() + 2000;

        // Envoyer la validation au serveur
        PacketDistributor.sendToServer(new CoreMazeAnswerPacket(teamId, currentEnigmaIndex, true));

        // Passer à l'énigme suivante ou terminer
        if (solvedCount >= 3) {
            // Toutes les énigmes sont résolues !
            feedbackMessage = "Core Maze résolu ! +300 points !";
            feedbackColor = 0xFFD700;
            feedbackEndTime = System.currentTimeMillis() + 3000;

            // Fermer l'écran après un délai
            // Le serveur s'occupe d'ajouter les points
        } else {
            currentEnigmaIndex++;
            answerBox.setValue("");
            showHint = false;
        }
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
        graphics.drawCenteredString(this.font, "§6§lCORE MAZE", centerX, panelY + 15, 0xFFD700);

        // Progression
        String progress = String.format("§7Progression: §e%d§7/§e3", solvedCount);
        graphics.drawCenteredString(this.font, progress, centerX, panelY + 35, 0xFFFFFF);

        // Indicateurs de progression (cercles)
        for (int i = 0; i < 3; i++) {
            int circleX = centerX - 30 + (i * 30);
            int circleY = panelY + 55;
            int color = i < solvedCount ? 0xFF00FF00 : (i == currentEnigmaIndex ? 0xFFFFD700 : 0xFF555555);
            graphics.fill(circleX - 8, circleY - 8, circleX + 8, circleY + 8, color);
        }

        // Afficher l'énigme actuelle
        if (currentEnigmaIndex < enigmas.size() && solvedCount < 3) {
            EnigmaData enigma = enigmas.get(currentEnigmaIndex);

            // Type d'énigme
            String typeStr = "§8[" + enigma.type() + "]";
            graphics.drawCenteredString(this.font, typeStr, centerX, panelY + 80, 0x888888);

            // Question (peut être sur plusieurs lignes)
            String question = enigma.question();
            List<String> lines = splitText(question, 50);
            int lineY = panelY + 100;
            for (String line : lines) {
                graphics.drawCenteredString(this.font, "§f" + line, centerX, lineY, 0xFFFFFF);
                lineY += 12;
            }

            // Indice (si demandé)
            if (showHint) {
                graphics.drawCenteredString(this.font, "§7Indice: §e" + enigma.hint(), centerX, panelY + 160, 0xAAAAAA);
            } else {
                graphics.drawCenteredString(this.font, "§8[Appuyez sur H pour un indice]", centerX, panelY + 160, 0x555555);
            }
        } else if (solvedCount >= 3) {
            // Puzzle résolu !
            graphics.drawCenteredString(this.font, "§a§lPUZZLE RÉSOLU !", centerX, panelY + 120, 0x00FF00);
            graphics.drawCenteredString(this.font, "§6+300 points pour votre équipe !", centerX, panelY + 140, 0xFFD700);
        }

        // Champ de réponse
        if (solvedCount < 3) {
            this.answerBox.render(graphics, mouseX, mouseY, partialTick);
        }

        // Message de feedback
        if (System.currentTimeMillis() < feedbackEndTime) {
            graphics.drawCenteredString(this.font, feedbackMessage, centerX, panelY + 220, feedbackColor);
        }

        // Instructions
        graphics.drawCenteredString(this.font, "§8[ESC pour quitter]", centerX, panelY + PANEL_HEIGHT - 20, 0x555555);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // H pour afficher l'indice
        if (keyCode == 72) { // H
            showHint = true;
            return true;
        }

        // Entrée pour valider
        if (keyCode == 257 && answerBox.isFocused()) { // ENTER
            // La validation se fait automatiquement via onAnswerChanged
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Divise un texte long en plusieurs lignes
     */
    private List<String> splitText(String text, int maxChars) {
        List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxChars) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Données d'une énigme (transmises depuis le serveur)
     */
    public record EnigmaData(String type, String question, String answer, String hint) {}
}
