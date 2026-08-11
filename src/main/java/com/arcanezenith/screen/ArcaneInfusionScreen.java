package com.arcanezenith.screen;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.menu.ArcaneInfusionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ArcaneInfusionScreen extends AbstractContainerScreen<ArcaneInfusionMenu> {

    // Uses a simple colored background drawn via GuiGraphics - no external texture PNG needed
    private static final int BG_WIDTH = 200;
    private static final int BG_HEIGHT = 172;

    public ArcaneInfusionScreen(ArcaneInfusionMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Dark purple background
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A0A2E);
        // Inner border
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF23104A);

        // Slot backing - Wand slot (44,35)
        drawSlotBacking(graphics, x + 44, y + 35, 0xFF4A2080);
        // Scroll slot (84,35)
        drawSlotBacking(graphics, x + 84, y + 35, 0xFF6A1050);
        // Arrow between slots
        graphics.fill(x + 112, y + 38, x + 140, y + 40, 0xFFAA80FF);
        graphics.fill(x + 138, y + 35, x + 142, y + 43, 0xFFAA80FF); // arrowhead
        // Result/learn slot (152,35)
        drawSlotBacking(graphics, x + 152, y + 35, 0xFF208040);

        // Labels above slots
        graphics.drawString(this.font, "Wand", x + 40, y + 22, 0xBBBBFF, false);
        graphics.drawString(this.font, "Scroll", x + 79, y + 22, 0xFFBBFF, false);
        graphics.drawString(this.font, "Learn", x + 148, y + 22, 0xAAFFAA, false);

        // Decorative corner rune lines
        graphics.fill(x + 4, y + 4, x + 20, y + 5, 0xFF8844FF);
        graphics.fill(x + 4, y + 4, x + 5, y + 20, 0xFF8844FF);
        graphics.fill(x + imageWidth - 20, y + 4, x + imageWidth - 4, y + 5, 0xFF8844FF);
        graphics.fill(x + imageWidth - 5, y + 4, x + imageWidth - 4, y + 20, 0xFF8844FF);
        graphics.fill(x + 4, y + imageHeight - 5, x + 20, y + imageHeight - 4, 0xFF8844FF);
        graphics.fill(x + 4, y + imageHeight - 20, x + 5, y + imageHeight - 4, 0xFF8844FF);

        // Player inventory label
        graphics.drawString(this.font, "Inventory", x + 8, y + 72, 0x9977BB, false);
    }

    private void drawSlotBacking(GuiGraphics graphics, int x, int y, int color) {
        // Slot is 18x18, draw backing box
        graphics.fill(x - 2, y - 2, x + 18, y + 18, color);
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF110820);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xDDAAFF, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
