package com.mceteams.xiidays.menus;

import com.mceteams.xiidays.utils.ScoreboardManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

public class ScoreboardScreen extends Screen {

    // ===== TEXTURES =====
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/scoreboard_bg.png");
    private static final ResourceLocation ARROW_UP =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/arrow_up.png");
    private static final ResourceLocation ARROW_DOWN =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/arrow_down.png");
    private static final ResourceLocation TEAM_BAR_GOLD =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team_bar_gold.png");
    private static final ResourceLocation TEAM_BAR_SILVER =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team_bar_silver.png");
    private static final ResourceLocation TEAM_BAR_BRONZE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team_bar_bronze.png");
    private static final ResourceLocation TEAM_BAR_NORMAL =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/team_bar_normal.png");

    // ===== POLICE CUSTOM (optionnel) =====
//    private static final ResourceLocation CUSTOM_FONT =
//            ResourceLocation.fromNamespaceAndPath(MODID, "scoreboard_font");

    // ===== DIMENSIONS =====
    private static final int MENU_WIDTH = 600;
    private static final int MENU_HEIGHT = 400;
    private static final int BAR_WIDTH = 400;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_SPACING = 10;

    private int menuX;
    private int menuY;

    public ScoreboardScreen() {
        super(Component.literal("Scoreboard"));
    }

    @Override
    protected void init() {
        super.init();

        // Centrer le menu
        this.menuX = (this.width - MENU_WIDTH) / 2;
        this.menuY = (this.height - MENU_HEIGHT) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fond d'écran flou (optionnel)
        this.renderBlurredBackground(partialTick);

        // Background du menu
        renderMenuBackground(graphics);

        // Titre "CLASSEMENT"
        renderTitle(graphics);

        // Barres des équipes
        renderTeamBars(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void renderMenuBackground(GuiGraphics graphics) {
        // Fond semi-transparent noir
        graphics.fill(menuX, menuY, menuX + MENU_WIDTH, menuY + MENU_HEIGHT, 0xCC000000);

        // Background texture personnalisé
        RenderSystem.enableBlend();
        graphics.blit(BACKGROUND, menuX, menuY, 0, 0, MENU_WIDTH, MENU_HEIGHT, MENU_WIDTH, MENU_HEIGHT);
        RenderSystem.disableBlend();
    }

    private void renderTitle(GuiGraphics graphics) {
        Component title = Component.literal("CLASSEMENT");
//                .withStyle(style -> style.withFont(CUSTOM_FONT)); // Police custom si disponible

        int titleX = menuX + (MENU_WIDTH / 2);
        int titleY = menuY + 30;

        // Ombre du texte
        graphics.drawCenteredString(font, title, titleX + 2, titleY + 2, 0x88000000);

        // Texte principal (or)
        graphics.drawCenteredString(font, title, titleX, titleY, 0xFFFFD700);
    }

    private void renderTeamBars(GuiGraphics graphics) {
        List<ScoreboardManager.TeamRankingEntry> rankings = ScoreboardManager.getRankings();

        int startX = menuX + (MENU_WIDTH - BAR_WIDTH) / 2;
        int startY = menuY + 80;

        for (int i = 0; i < rankings.size(); i++) {
            ScoreboardManager.TeamRankingEntry entry = rankings.get(i);
            int barY = startY + (i * (BAR_HEIGHT + BAR_SPACING));

            renderSingleTeamBar(graphics, entry, startX, barY, i + 1);
        }
    }

    private void renderSingleTeamBar(GuiGraphics graphics, ScoreboardManager.TeamRankingEntry entry,
                                     int x, int y, int rank) {
        // Texture de la barre selon le rang
        ResourceLocation barTexture = switch(rank) {
            case 1 -> TEAM_BAR_GOLD;
            case 2 -> TEAM_BAR_SILVER;
            case 3 -> TEAM_BAR_BRONZE;
            default -> TEAM_BAR_NORMAL;
        };

        // Rendu de la barre
        graphics.blit(barTexture, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        // Médaille (émoji ou texture)
        String medal = getRankMedal(rank);
        graphics.drawString(font, medal, x + 15, y + 18, 0xFFFFFFFF);

        // Nom de l'équipe
        Component teamName = Component.literal(entry.teamName());
//                .withStyle(style -> style.withFont(CUSTOM_FONT));
        graphics.drawString(font, teamName, x + 60, y + 18, 0xFFFFFFFF);

        // Points (police custom digits)
        Component points = Component.literal(String.format("%010d", entry.points()));
//                .withStyle(style -> style.withFont(CUSTOM_FONT));
        graphics.drawString(font, points, x + 220, y + 18, 0xFFCCCCCC);

        // Flèche (si changement récent)
        if (ScoreboardManager.shouldShowArrow(entry)) {
            renderArrow(graphics, entry, x + BAR_WIDTH - 40, y + 10);
        }
    }

    private void renderArrow(GuiGraphics graphics, ScoreboardManager.TeamRankingEntry entry,
                             int x, int y) {
        ResourceLocation arrowTexture = entry.change() == ScoreboardManager.RankChange.UP
                ? ARROW_UP
                : ARROW_DOWN;

        // Animation pulsante
        long time = System.currentTimeMillis();
        float scale = 1.0f + 0.15f * (float) Math.sin(time / 150.0);

        // Animation fade (disparaît progressivement après 15s)
        long elapsed = time - entry.lastChangeTime();
        float alpha = 1.0f;
        if (elapsed > 15000) { // Commence à fade après 15s
            alpha = Math.max(0, 1.0f - ((elapsed - 15000) / 5000.0f)); // Fade sur 5s
        }

        graphics.pose().pushPose();
        graphics.pose().translate(x + 16, y + 16, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(-16, -16, 0);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        graphics.blit(arrowTexture, 0, 0, 0, 0, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        graphics.pose().popPose();
    }

    private String getRankMedal(int rank) {
        return switch(rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}