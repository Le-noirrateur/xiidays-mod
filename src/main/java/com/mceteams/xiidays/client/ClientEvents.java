package com.mceteams.xiidays.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

public class ClientEvents {
    public static final String CATEGORY = "key.categories." + MODID;
    public static KeyMapping OPEN_SCOREBOARD; // ← UNE SEULE déclaration

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN_SCOREBOARD = new KeyMapping(
                "key." + MODID + ".open_scoreboard", // ← Enregistré UNE SEULE fois
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                CATEGORY
        );
        event.register(OPEN_SCOREBOARD);
    }

    // ... reste du code
}