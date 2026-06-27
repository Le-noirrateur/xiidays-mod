package com.mceteams.xiidays.visual;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("xiidays", "key_categories"));

    public static final KeyMapping OPEN_SCOREBOARD = new KeyMapping(
            "key.xiidays.open_scoreboard",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY
    );

    public static final KeyMapping OPEN_ADMIN_MENU = new KeyMapping(
            "key.xiidays.open_admin_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_0,
            CATEGORY
    );
}
