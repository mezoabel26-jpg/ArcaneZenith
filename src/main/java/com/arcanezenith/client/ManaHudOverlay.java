package com.arcanezenith.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * Clean client-side HUD: current mana / max mana bar + regen rate, drawn bottom-left
 * (mirroring vanilla's XP bar position on the opposite side of the hotbar).
 *
 * Reads only from ClientManaCache, which is a passive display cache kept up to date by
 * S2CSyncManaPacket - never touches the server-authoritative attachment directly.
 */
public final class ManaHudOverlay implements LayeredDraw.Layer {

    private static final int BAR_WIDTH = 90;
    private static final int BAR_HEIGHT = 6;
    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 30;

    private static final int COLOR_BG = 0xAA1A1030;
    private static final int COLOR_BORDER = 0xFF4B2E83;
    private static final int COLOR_FILL_TOP = 0xFF7B5CFF;
    private static final int COLOR_FILL_BOTTOM = 0xFF3E1FAE;
    private static final int COLOR_TEXT = 0xFFD9CCFF;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        ClientManaCache.tickSmoothing(partialTicks);

        float mana = ClientManaCache.getDisplayedMana();
        float maxMana = Math.max(1.0f, ClientManaCache.getMaxMana());
        float regen = ClientManaCache.getRegenPerSecond();
        float pct = Math.max(0f, Math.min(1f, mana / maxMana));

        int screenHeight = guiGraphics.guiHeight();
        int x = MARGIN_X;
        int y = screenHeight - MARGIN_Y;

        guiGraphics.fill(x - 2, y - 2, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, COLOR_BG);
        guiGraphics.fill(x - 2, y - 2, x + BAR_WIDTH + 2, y - 1, COLOR_BORDER);
        guiGraphics.fill(x - 2, y + BAR_HEIGHT + 1, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, COLOR_BORDER);
        guiGraphics.fill(x - 2, y - 2, x - 1, y + BAR_HEIGHT + 2, COLOR_BORDER);
        guiGraphics.fill(x + BAR_WIDTH + 1, y - 2, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, COLOR_BORDER);

        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF241535);

        int filledWidth = Math.round(BAR_WIDTH * pct);
        if (filledWidth > 0) {
            int halfHeight = Math.max(1, BAR_HEIGHT / 2);
            guiGraphics.fillGradient(x, y, x + filledWidth, y + halfHeight, COLOR_FILL_TOP, COLOR_FILL_TOP);
            guiGraphics.fillGradient(x, y + halfHeight, x + filledWidth, y + BAR_HEIGHT, COLOR_FILL_BOTTOM, COLOR_FILL_BOTTOM);
        }

        String manaText = String.format("%d / %d Mana", Math.round(mana), Math.round(maxMana));
        String regenText = String.format(" (+%.1f/s)", regen);
        Component label = Component.literal(manaText)
                .append(Component.literal(regenText).withStyle(style -> style.withColor(0xFF8C7BC7)));

        RenderSystem.enableBlend();
        guiGraphics.drawString(mc.font, label, x, y - 10, COLOR_TEXT, true);
        RenderSystem.disableBlend();
    }
}
