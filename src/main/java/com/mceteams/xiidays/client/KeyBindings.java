package com.mceteams.xiidays.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    // Définition de la touche pour ouvrir le scoreboard (par défaut : TAB)
    public static final Lazy<KeyMapping> OPEN_SCOREBOARD = Lazy.of(() -> new KeyMapping(
            "key.xiidays.open_scoreboard",  // Clé de traduction
            InputConstants.Type.KEYSYM,      // Type d'input
            GLFW.GLFW_KEY_U,               // Touche par défaut (TAB)
            "key.categories.xiidays"         // Catégorie dans les paramètres
    ));
}