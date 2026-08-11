package com.arcanezenith.client;

import com.arcanezenith.item.WandItem;
import com.arcanezenith.progression.SpellRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Diegetic Wand 3D World HUD — dual counter-rotating rune rings rendered at the
 * wand tip in world-space.
 *
 * FIXED (was: approximation via eye+look vector):
 * Now extracts the true tip position from the first-person hand render matrix by:
 *  1. Capturing the PoseStack at RenderHandEvent (client dist only, no perf cost when
 *     wand is not held).
 *  2. Transforming a local "tip" point (0, 0, -1 in item space = forward out of the
 *     wand's nozzle) through that matrix into camera space, then into world space via
 *     the camera offset.
 * Falls back to the eye+look approximation for third-person view or when the matrix
 * hasn't been captured yet this frame.
 */
@EventBusSubscriber(modid = "arcanezenith", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WandTipHudRenderer {

    private static float ringAngleA = 0f;
    private static float ringAngleB = 0f;

    /** Set each frame by RenderHandEvent before AFTER_PARTICLES fires. */
    private static Matrix4f capturedHandMatrix = null;
    private static boolean capturedIsFirstPerson = false;

    private WandTipHudRenderer() {}

    // ── Hand matrix capture ───────────────────────────────────────────────────

    /**
     * Fires during the hand render pass (first-person only).
     * We snapshot the PoseStack's current matrix which, at this point, already
     * includes all the bob, sway, and held-item transform that Minecraft applies.
     * The "tip" of the wand is approximately +0.5 units along the item's local
     * forward axis (Z = -1 in GL convention → we sample at local (0, 0.05, -0.5)).
     */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof WandItem)) return;

        // Snapshot the matrix — copy it so the PoseStack can keep mutating
        capturedHandMatrix = new Matrix4f(event.getPoseStack().last().pose());
        capturedIsFirstPerson = true;
    }

    // ── Main render ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        ItemStack held = findHeldWand(player);
        if (held == null) {
            capturedHandMatrix = null;
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        ringAngleA += 4.0f;
        ringAngleB -= 3.0f;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        Vec3 discPos = computeDiscPosition(player, camera, camPos, partialTick);

        var progress = ClientProgressionCache.getSelected();
        SpellRegistry.Definition def = progress != null ? SpellRegistry.get(progress) : null;
        int baseColor = def != null ? def.element().color : 0xFFB08CFF;

        float chargeProgress = ClientWandState.getCooldownProgress();
        float scale = 0.2f + 0.8f * chargeProgress;

        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) return;

        poseStack.pushPose();
        poseStack.translate(discPos.x - camPos.x, discPos.y - camPos.y, discPos.z - camPos.z);
        poseStack.mulPose(camera.rotation());

        renderRing(poseStack, 0.5f * scale, ringAngleA, baseColor, 0.85f);
        renderRing(poseStack, 0.35f * scale, ringAngleB, brighten(baseColor), 0.6f);

        poseStack.popPose();

        // Clear capture so stale matrices don't persist to the next frame
        capturedHandMatrix = null;
        capturedIsFirstPerson = false;
    }

    // ── Tip position computation ──────────────────────────────────────────────

    /**
     * Returns the world-space position of the wand's tip.
     *
     * First-person: uses the captured hand render matrix (exact).
     * Third-person / fallback: uses the eye+look approximation (same as before).
     */
    private static Vec3 computeDiscPosition(LocalPlayer player, Camera camera,
                                            Vec3 camPos, float partialTick) {
        if (capturedHandMatrix != null && capturedIsFirstPerson) {
            // Transform the local "nozzle" point through the hand matrix.
            // In Minecraft's first-person hand space: X right, Y up, Z toward viewer.
            // The item extends forward (away from viewer) so the tip is at local (0, 0.05, -0.55).
            // capturedHandMatrix is camera-space (origin = camera), so the result is already
            // a camera-space offset — we just add camPos to get world-space.
            Vector4f localTip = new Vector4f(0f, 0.05f, -0.55f, 1f);
            // JOML: matrix.transform(v) = matrix * v (column-vector convention)
            capturedHandMatrix.transform(localTip);
            return new Vec3(
                    camPos.x + localTip.x,
                    camPos.y + localTip.y,
                    camPos.z + localTip.z
            );
        }

        // Fallback: eye + look approximation (works for third-person and first frames)
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        return eye.add(look.scale(1.3)).add(0, -0.35, 0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ItemStack findHeldWand(Player player) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof WandItem) return main;
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof WandItem) return off;
        return null;
    }

    private static void renderRing(PoseStack poseStack, float radius, float angleDeg,
                                   int argb, float alphaMul) {
        float a = ((argb >> 24) & 0xFF) / 255f * alphaMul;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(angleDeg)));

        Matrix4f matrix = poseStack.last().pose();
        float inner = radius * 0.75f;
        float outer = radius;
        int segments = 6;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP,
                DefaultVertexFormat.POSITION_COLOR);

        float sweep = 270f;
        for (int i = 0; i <= segments; i++) {
            double t = Math.toRadians(sweep * (i / (double) segments));
            float cos = (float) Math.cos(t);
            float sin = (float) Math.sin(t);
            buffer.addVertex(matrix, cos * outer, sin * outer, 0f).setColor(r, g, b, a);
            buffer.addVertex(matrix, cos * inner, sin * inner, 0f).setColor(r, g, b, a * 0.4f);
        }

        MeshData mesh = buffer.buildOrThrow();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static int brighten(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 70);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 70);
        int b = Math.min(255, (argb & 0xFF) + 70);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
