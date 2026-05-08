package com.mceteams.xiidays.visual;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String CATEGORY = "key.categories.xiidays";

    public static final KeyMapping OPEN_SCOREBOARD = new KeyMapping(
            "key.xiidays.open_scoreboard",           // Clé de traduction
            KeyConflictContext.IN_GAME,              // Contexte (en jeu uniquement)
            InputConstants.Type.KEYSYM,              // Type d'input
            GLFW.GLFW_KEY_U,                         // Touche U
            CATEGORY                                 // Catégorie dans les options
    );
}
