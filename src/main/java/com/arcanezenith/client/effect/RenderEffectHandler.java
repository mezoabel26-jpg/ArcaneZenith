package com.arcanezenith.client.effect;

import com.arcanezenith.ArcaneZenith;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = ArcaneZenith.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RenderEffectHandler {

    /** Apply post-processing after the world is rendered but before the HUD. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            PostEffectManager.applyIfActive((float) event.getPartialTick().getRealtimeDeltaTicks());
        }
    }

    /** Inject camera shake into the camera angle computation. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (CameraShake.isShaking()) {
            event.setYaw(event.getYaw()     + CameraShake.getYawOffset());
            event.setPitch(event.getPitch() + CameraShake.getPitchOffset());
        }
    }

    /** Inject FOV punch. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float add = CameraShake.getFovAddition();
        if (add != 0f) {
            // FOV modifier is a multiplier; convert degree delta to rough multiplier
            event.setNewFovModifier(event.getNewFovModifier() + add / 70.0f);
        }
    }
}
