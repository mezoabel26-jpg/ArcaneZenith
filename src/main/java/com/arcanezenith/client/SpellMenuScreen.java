package com.arcanezenith.client;

import com.arcanezenith.network.C2SSelectSpellPacket;
import com.arcanezenith.network.C2SUnlockSpellPacket;
import com.arcanezenith.progression.SpellRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Real 360-degree animated radial quick-cast selector (default keybind G).
 *
 * Each unlocked/locked spell gets an equal angular segment around the circle. Mouse
 * displacement from the center is converted to an angle via atan2, which picks the
 * hovered segment - the same "dynamic vector math" approach described in the design doc,
 * just implemented with GuiGraphics immediate-mode rendering instead of a 3D model, which
 * keeps this screen safe to run without any custom renderer plumbing.
 *
 * Network calls are unchanged from the old button-list version: C2SSelectSpellPacket /
 * C2SUnlockSpellPacket, so nothing server-side needed to change for this swap.
 */
public class SpellMenuScreen extends Screen {

    private static final int INNER_RADIUS = 40;
    private static final int OUTER_RADIUS = 110;
    private static final int SEGMENT_GAP_DEG = 2;

    private float openAnim = 0f; // 0..1 expansion animation
    private int hoveredIndex = -1;

    public SpellMenuScreen() {
        super(Component.literal("Arcane Spells"));
    }

    @Override
    protected void init() {
        // No vanilla widgets - this screen is entirely custom-rendered and mouse-driven.
    }

    @Override
    public void tick() {
        super.tick();
        if (openAnim < 1f) {
            openAnim = Math.min(1f, openAnim + 0.12f);
        }
    }

    private List<SpellRegistry.Definition> spells() {
        return SpellRegistry.all();
    }

    /** Converts current mouse position relative to screen center into a selected segment index, or -1. */
    private int segmentAtMouse(double mouseX, double mouseY) {
        List<SpellRegistry.Definition> spells = spells();
        if (spells.isEmpty()) return -1;

        double centerX = this.width / 2.0;
        double centerY = this.height / 2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        // Dead zone in the middle (like a real radial menu) so hovering near the center
        // doesn't accidentally select a segment.
        if (dist < INNER_RADIUS * 0.5) return -1;

        // atan2 gives -180..180 with 0 = +X axis; rotate so segment 0 starts at the top (-90deg)
        // and sweeps clockwise, which reads more naturally for a vertical spell list mapped to a circle.
        double angleDeg = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angleDeg < 0) angleDeg += 360.0;

        double segmentSize = 360.0 / spells.size();
        return (int) (angleDeg / segmentSize) % spells.size();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<SpellRegistry.Definition> spells = spells();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Dim background so the world is still faintly visible (diegetic feel) rather than a flat block.
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        hoveredIndex = segmentAtMouse(mouseX, mouseY);

        float outerR = OUTER_RADIUS * easeOutBack(openAnim);
        float innerR = INNER_RADIUS * easeOutBack(openAnim);

        if (!spells.isEmpty()) {
            drawRing(graphics, centerX, centerY, innerR, outerR, spells);
        }

        // Center hub: points + selected spell name
        graphics.fill(centerX - 38, centerY - 12, centerX + 38, centerY + 12, 0xCC1A1030);
        graphics.drawCenteredString(this.font, Component.literal(String.valueOf(ClientProgressionCache.getPoints())),
                centerX, centerY - 9, 0xFFD9CCFF);
        graphics.drawCenteredString(this.font, Component.literal("pts"),
                centerX, centerY + 1, 0xFF8C7BC7);

        // Hovered segment label + cost, drawn near the cursor
        if (hoveredIndex >= 0 && hoveredIndex < spells.size()) {
            var def = spells.get(hoveredIndex);
            boolean unlocked = ClientProgressionCache.isUnlocked(def.id());
            boolean isSelected = def.id().equals(ClientProgressionCache.getSelected());

            String label = def.displayName();
            if (isSelected) label = "\u00bb " + label + " \u00ab";
            String sub = unlocked ? "Left-click to select" : ("Locked \u2014 " + def.unlockCost() + " pts to unlock");

            int labelY = centerY - (int) outerR - 28;
            graphics.drawCenteredString(this.font, Component.literal(label), centerX, labelY,
                    unlocked ? 0xFFFFFFFF : 0xFFAA88FF);
            graphics.drawCenteredString(this.font, Component.literal(sub), centerX, labelY + 10,
                    unlocked ? 0xFF8C7BC7 : 0xFFCC6633);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Draws the ring as per-segment colored triangle-fan wedges via a raw mesh, plus text labels. */
    private void drawRing(GuiGraphics graphics, int centerX, int centerY, float innerR, float outerR,
                           List<SpellRegistry.Definition> spells) {
        int count = spells.size();
        double segmentSize = 360.0 / count;

        for (int i = 0; i < count; i++) {
            var def = spells.get(i);
            boolean unlocked = ClientProgressionCache.isUnlocked(def.id());
            boolean isSelected = def.id().equals(ClientProgressionCache.getSelected());
            boolean isHovered = i == hoveredIndex;

            double startDeg = i * segmentSize - 90.0 + SEGMENT_GAP_DEG / 2.0;
            double endDeg = (i + 1) * segmentSize - 90.0 - SEGMENT_GAP_DEG / 2.0;

            int baseColor = !unlocked ? 0x552A1B4A : (isSelected ? 0xAA5B3FCF : 0x772A1B4A);
            int color = isHovered ? brighten(baseColor) : baseColor;

            drawWedge(graphics, centerX, centerY, innerR, outerR, startDeg, endDeg, color);

            // Segment label, placed at the mid-angle, mid-radius
            double midDeg = (startDeg + endDeg) / 2.0;
            double midRad = Math.toRadians(midDeg);
            float midRadius = (innerR + outerR) / 2f;
            int labelX = (int) (centerX + Math.cos(midRad) * midRadius);
            int labelY = (int) (centerY + Math.sin(midRad) * midRadius);

            String shortLabel = abbreviate(def.displayName());
            int textColor = unlocked ? (isSelected ? 0xFFFFF2C0 : 0xFFEDE6FF) : 0xFF9D8FBF;
            graphics.drawCenteredString(this.font, Component.literal(shortLabel), labelX, labelY - 4, textColor);
        }
    }

    private String abbreviate(String name) {
        // Keep labels readable on a small wedge without truncation artifacts mid-word.
        String[] words = name.split(" ");
        if (words.length == 1) return name.length() > 10 ? name.substring(0, 9) + "\u2026" : name;
        return words[0].length() > 9 ? words[0].substring(0, 8) + "\u2026" : words[0];
    }

    private int brighten(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 60);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 60);
        int b = Math.min(255, (argb & 0xFF) + 60);
        return (Math.min(255, a + 40) << 24) | (r << 16) | (g << 8) | b;
    }

    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float p = t - 1f;
        return 1f + c3 * p * p * p + c1 * p * p;
    }

    /** Renders one annular wedge (a ring segment) as a triangle strip between innerR and outerR. */
    private void drawWedge(GuiGraphics graphics, float cx, float cy, float innerR, float outerR,
                            double startDeg, double endDeg, int argbColor) {
        float a = ((argbColor >> 24) & 0xFF) / 255f;
        float r = ((argbColor >> 16) & 0xFF) / 255f;
        float g = ((argbColor >> 8) & 0xFF) / 255f;
        float b = (argbColor & 0xFF) / 255f;

        int steps = 8;
        var matrix = graphics.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= steps; i++) {
            double t = startDeg + (endDeg - startDeg) * (i / (double) steps);
            double rad = Math.toRadians(t);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            buffer.addVertex(matrix, cx + cos * outerR, cy + sin * outerR, 0f).setColor(r, g, b, a);
            buffer.addVertex(matrix, cx + cos * innerR, cy + sin * innerR, 0f).setColor(r, g, b, a);
        }

        MeshData mesh = buffer.buildOrThrow();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // left click = confirm current hover
            int index = segmentAtMouse(mouseX, mouseY);
            if (index >= 0 && index < spells().size()) {
                onSelect(spells().get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onSelect(SpellRegistry.Definition def) {
        boolean unlocked = ClientProgressionCache.isUnlocked(def.id());
        if (unlocked) {
            PacketDistributor.sendToServer(new C2SSelectSpellPacket(def.id()));
        } else {
            PacketDistributor.sendToServer(new C2SUnlockSpellPacket(def.id()));
        }
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
