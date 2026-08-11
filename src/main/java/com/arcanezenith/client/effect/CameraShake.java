package com.arcanezenith.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * Camera shake and FOV punch effects applied in the client render tick.
 *
 * Hooked into the client tick (ClientTickHandler) and into
 * RenderLevelStageEvent for the actual camera offset injection.
 *
 * CameraShake.apply() is called from a mixin or a forge camera event;
 * in 1.21.x the cleanest hook is ViewportEvent.ComputeCameraAngles.
 */
public final class CameraShake {

    private static final Random RNG = new Random();

    // Shake state
    private static float shakeIntensity  = 0f;
    private static int   shakeTicks      = 0;
    private static float shakeOffsetYaw  = 0f;
    private static float shakeOffsetPitch= 0f;

    // FOV punch state
    private static float fovDelta     = 0f;
    private static int   fovTicks     = 0;
    private static int   fovTotal     = 0;

    private CameraShake() {}

    /** Call from server via S2CVisualEffectPacket -> client handler. */
    public static void startShake(float intensity, int durationTicks) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeTicks     = Math.max(shakeTicks, durationTicks);
    }

    /** FOV punch: positive = zoom out (explosion), negative = zoom in (teleport). */
    public static void startFovPunch(float deltaDegrees, int durationTicks) {
        fovDelta = deltaDegrees;
        fovTicks = durationTicks;
        fovTotal = durationTicks;
    }

    /** Call each client tick. */
    public static void tick() {
        if (shakeTicks > 0) {
            shakeTicks--;
            float decay = (float) shakeTicks / Math.max(1, shakeTicks + 1);
            float mag   = shakeIntensity * decay;
            shakeOffsetYaw   = (RNG.nextFloat() * 2 - 1) * mag;
            shakeOffsetPitch = (RNG.nextFloat() * 2 - 1) * mag;
            if (shakeTicks == 0) shakeIntensity = 0;
        } else {
            shakeOffsetYaw   = Mth.lerp(0.3f, shakeOffsetYaw,   0);
            shakeOffsetPitch = Mth.lerp(0.3f, shakeOffsetPitch, 0);
        }
        if (fovTicks > 0) fovTicks--;
    }

    /** Returns current yaw offset in degrees. Call from camera angle event. */
    public static float getYawOffset()   { return shakeOffsetYaw; }
    public static float getPitchOffset() { return shakeOffsetPitch; }

    /** Returns FOV addition (degrees). Call from FOV compute event. */
    public static float getFovAddition() {
        if (fovTotal == 0 || fovTicks == 0) return 0f;
        float frac = (float) fovTicks / fovTotal;
        // Sharp punch, smooth return
        return fovDelta * frac * frac;
    }

    public static boolean isShaking() { return shakeTicks > 0 || Math.abs(shakeOffsetYaw) > 0.01f; }
}
