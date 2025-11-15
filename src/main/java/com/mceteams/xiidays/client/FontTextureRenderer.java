package com.mceteams.xiidays.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

/**
 * Renderer pour afficher du texte avec le sprite sheet de police custom
 */
public class FontTextureRenderer {

    // Textures des différentes polices (tes 8 images)
    // Textures des différentes polices (tes 8 images)
    private static final ResourceLocation FONT_GOLD =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/font/gold.png");
    private static final ResourceLocation FONT_GOLD_GLOWING =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/font/gold_glowing.png");
    private static final ResourceLocation FONT_GOLD_GLASSY =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/font/gold_glassy.png");
    private static final ResourceLocation FONT_DARK_GRAY =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/font/dark_gray.png");
    private static final ResourceLocation FONT_GRAY =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/font/gray.png");
    private static final ResourceLocation FONT_PURPLE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/font/purple.png");

    // Layout du sprite sheet (d'après ton image)
    // Ligne 1: A-Z (26 caractères)
    // Ligne 2: a-z (26 caractères)
    // Ligne 3: 0-9 + symboles
    // Ligne 4: Symboles spéciaux
    private static final String CHARS_ROW_1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CHARS_ROW_2 = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHARS_ROW_3 = "0123456789";
    private static final String CHARS_ROW_4 = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ ";

    // Dimensions RÉELLES de ton sprite sheet
    private static final int CHAR_WIDTH = 160;      // Largeur d'UNE lettre
    private static final int CHAR_HEIGHT = 112;     // Hauteur d'UNE lettre
    private static final int CHAR_FULL_WIDTH = 344; // Largeur complète (avec espacement)
    private static final int TEXTURE_WIDTH = 4000;  // Largeur totale du sprite
    private static final int TEXTURE_HEIGHT = 1280; // Hauteur totale du sprite
    private static final int SPACING = CHAR_FULL_WIDTH - CHAR_WIDTH; // Espacement entre caractères (176px)

    /**
     * Trouve la position UV d'un caractère dans le sprite sheet
     */
    private static class CharUV {
        int u, v; // Position en pixels dans la texture

        CharUV(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }

    private static CharUV getCharUV(char c) {
        // Chercher dans la ligne 1 (majuscules)
        int index = CHARS_ROW_1.indexOf(c);
        if (index >= 0) {
            return new CharUV(index * CHAR_WIDTH, 0);
        }

        // Chercher dans la ligne 2 (minuscules)
        index = CHARS_ROW_2.indexOf(c);
        if (index >= 0) {
            return new CharUV(index * CHAR_WIDTH, CHAR_HEIGHT);
        }

        // Chercher dans la ligne 3 (chiffres + symboles)
        index = CHARS_ROW_3.indexOf(c);
        if (index >= 0) {
            return new CharUV(index * CHAR_WIDTH, CHAR_HEIGHT * 2);
        }

        // Caractère non trouvé → espace
        return new CharUV(CHARS_ROW_3.indexOf(' ') * CHAR_WIDTH, CHAR_HEIGHT * 2);
    }

    /**
     * Dessine un texte avec le sprite sheet
     * @param graphics GuiGraphics
     * @param texture Quelle police utiliser (FONT_ORANGE, FONT_GRAY, etc.)
     * @param text Texte à afficher
     * @param x Position X
     * @param y Position Y
     * @param scale Échelle (1.0 = normal, 1.5 = 150%, etc.)
     * @return Largeur totale du texte rendu
     */
    public static int drawText(GuiGraphics graphics, ResourceLocation texture,
                               String text, int x, int y, float scale) {

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();

        int offsetX = 0;

        for (char c : text.toCharArray()) {
            CharUV uv = getCharUV(c);

            // Dessiner le caractère depuis le sprite sheet
            graphics.blit(
                    texture,
                    offsetX, 0,                    // Position destination
                    uv.u, uv.v,                    // Position source (UV)
                    CHAR_WIDTH, CHAR_HEIGHT,       // Taille à copier
                    TEXTURE_WIDTH, TEXTURE_HEIGHT  // Taille totale de la texture
            );

            offsetX += CHAR_WIDTH;
        }

        RenderSystem.disableBlend();
        graphics.pose().popPose();

        return (int) (offsetX * scale);
    }

    /**
     * Calcule la largeur d'un texte
     */
    public static int getTextWidth(String text, float scale) {
        return (int) (text.length() * CHAR_WIDTH * scale);
    }

    /**
     * Dessine un texte centré
     */
    public static void drawCenteredText(GuiGraphics graphics, ResourceLocation texture,
                                        String text, int centerX, int y, float scale) {
        int width = getTextWidth(text, scale);
        drawText(graphics, texture, text, centerX - width / 2, y, scale);
    }

    // ===== Méthodes raccourcies pour chaque couleur =====

    public static int drawGoldText(GuiGraphics graphics, String text, int x, int y, float scale) {
        return drawText(graphics, FONT_GOLD, text, x, y, scale);
    }

    public static int drawGrayText(GuiGraphics graphics, String text, int x, int y, float scale) {
        return drawText(graphics, FONT_GRAY, text, x, y, scale);
    }
}