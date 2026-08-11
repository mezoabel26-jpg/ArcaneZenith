package com.arcanezenith.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public class ModKeybinds {

    public static final String CATEGORY = "key.categories.arcanezenith";

    public static final KeyMapping OPEN_SPELL_MENU = new KeyMapping(
            "key.arcanezenith.open_spell_menu", InputConstants.Type.KEYSYM,
            InputConstants.KEY_G, CATEGORY);

    public static final KeyMapping CYCLE_SPELL = new KeyMapping(
            "key.arcanezenith.cycle_spell", InputConstants.Type.KEYSYM,
            InputConstants.KEY_V, CATEGORY);
}
