package com.arcanezenith.client;

import com.arcanezenith.client.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Handles opening the Arcane Codex with a cinematic animated intro.
 *
 * A Codex nyitásakor:
 *  1. Mágikus hang szól (amethyst chime + beacon)
 *  2. Arany particle-burst az item körül
 *  3. FOV-punch befelé
 *  4. 12 tickkel később nyílik ki a screen (a particlek lefutnak előbb)
 *
 * Az animáció a ClientTickHandler-ben fut, nem blokkolja a játékot.
 */
public final class ArcaneCodexScreenOpener {

    private static int pendingOpenTick = -1; // -1 = nincs folyamatban

    private ArcaneCodexScreenOpener() {}

    /** Hívd a jobb klikknél — elindítja az animációt, 12 tick múlva nyílik a screen. */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Azonnali hang
        if (mc.player != null) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.8f, false);
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f, false);
        }

        // Particle burst az item helyén (kéz előtt)
        var pos = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(0.8));
        if (mc.level instanceof net.minecraft.client.multiplayer.ClientLevel cl) {
            // Rúna-gyűrű szétnyílás
            for (int i = 0; i < 24; i++) {
                double ang = i * Math.PI * 2 / 24.0;
                double r   = 0.8;
                cl.addParticle(ModParticles.RUNE_RING.get(),
                        pos.x + Math.cos(ang)*r, pos.y, pos.z + Math.sin(ang)*r,
                        Math.cos(ang)*0.04, 0.02, Math.sin(ang)*0.04);
            }
            // Arany csillogás
            for (int i = 0; i < 16; i++) {
                double ang = Math.random() * Math.PI * 2;
                cl.addParticle(ModParticles.GOLDEN_LIGHT.get(),
                        pos.x + (Math.random()-0.5)*0.3,
                        pos.y + (Math.random()-0.5)*0.3,
                        pos.z + (Math.random()-0.5)*0.3,
                        Math.cos(ang)*0.06, 0.05, Math.sin(ang)*0.06);
            }
            // Arcane szikrák
            for (int i = 0; i < 20; i++) {
                cl.addParticle(ModParticles.ARCANE_SPARK.get(),
                        pos.x, pos.y, pos.z,
                        (Math.random()-0.5)*0.15, Math.random()*0.1, (Math.random()-0.5)*0.15);
            }
        }

        // FOV punch befelé
        com.arcanezenith.client.effect.CameraShake.startFovPunch(-8f, 10);

        // Screen megnyitás 12 tick késéssel (animáció lefut)
        pendingOpenTick = 12;
    }

    /** Hívd minden client tick-ben (ClientSetup tick event-ből). */
    public static void tick() {
        if (pendingOpenTick <= 0) return;
        pendingOpenTick--;
        if (pendingOpenTick == 0) {
            pendingOpenTick = -1;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // Második hang a nyitáskor
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.2f, false);
                mc.setScreen(new ArcaneCodexScreen());
            }
        }
    }
}
