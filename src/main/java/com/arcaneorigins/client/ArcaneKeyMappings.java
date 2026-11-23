package com.arcaneorigins.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ArcaneKeyMappings {
    public static final String CATEGORY = "key.categories.arcaneorigins";

    public static final KeyMapping CAST_SPELL = new KeyMapping(
            "key.arcaneorigins.cast_spell",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping CYCLE_SPELL = new KeyMapping(
            "key.arcaneorigins.cycle_spell",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static final KeyMapping OPEN_CLASS_MENU = new KeyMapping(
            "key.arcaneorigins.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );
}
