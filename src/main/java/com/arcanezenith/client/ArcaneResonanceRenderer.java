package com.arcanezenith.client;

import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.progression.SpellProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Arcane Resonance — passzív aura a játékos körül az Arcane Point szint alapján.
 *
 * Szintek:
 *   0–99   pt  → semmi
 *   100–199 pt → halvány ARCANE_SPARK szikrák (1 per 20 tick)
 *   200–299 pt → ARCANE_SPARK + RUNE_RING halvány forgás (1 per 10 tick)
 *   300–399 pt → ARCANE_SPARK + RUNE_RING + GOLDEN_LIGHT (1 per 5 tick)
 *   400–499 pt → ARCANE_SPARK + kettős RUNE_RING + GOLDEN_LIGHT (1 per 3 tick)
 *   500+ pt    → ARCANE_SPARK + RUNE_RING + HEAVEN_BEAM + teljes arany glow (1 per 1 tick)
 *
 * Minden szintnél a particlek különböző mintázatban jelennek meg:
 *  - Szikrák: véletlenszerűen a test körül
 *  - Rúna-gyűrű: vízszintes sík, forgó
 *  - Heaven Beam: felfelé szálló fény 500+pt-nál
 */
public final class ArcaneResonanceRenderer {

    private static int tickCounter = 0;
    private static float ringAngle = 0f;

    private ArcaneResonanceRenderer() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        Player player = mc.player;
        ClientLevel level = mc.level;
        tickCounter++;

        // Arcane Points lekérése a kliens cache-ből
        int points = ClientProgressionCache.getPoints();

        if (points < 100) return;

        // Forgási szög
        ringAngle += 4.5f;
        if (ringAngle >= 360f) ringAngle -= 360f;

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // ── SZINT 1: 100–199 pt — halvány szikrák ──────────────────────────
        if (points >= 100 && tickCounter % 20 == 0) {
            spawnSparks(level, px, py, pz, 3, 0.8);
        }

        // ── SZINT 2: 200–299 pt — szikrák + halvány rúna-gyűrű ─────────────
        if (points >= 200 && tickCounter % 10 == 0) {
            spawnSparks(level, px, py, pz, 4, 1.0);
            spawnRingArc(level, px, py + 0.5, pz, 1.2, ringAngle, 8, false);
        }

        // ── SZINT 3: 300–399 pt — szikrák + gyűrű + arany fény ─────────────
        if (points >= 300 && tickCounter % 5 == 0) {
            spawnSparks(level, px, py, pz, 5, 1.2);
            spawnRingArc(level, px, py + 0.5, pz, 1.4, ringAngle, 12, false);
            spawnGoldenMotes(level, px, py, pz, 3);
        }

        // ── SZINT 4: 400–499 pt — kettős gyűrű + erős glow ─────────────────
        if (points >= 400 && tickCounter % 3 == 0) {
            spawnSparks(level, px, py, pz, 6, 1.4);
            spawnRingArc(level, px, py + 0.3, pz, 1.2, ringAngle,      16, false);
            spawnRingArc(level, px, py + 0.9, pz, 1.5, -ringAngle * 0.7f, 12, false);
            spawnGoldenMotes(level, px, py, pz, 5);
        }

        // ── SZINT 5: 500+ pt — teljes arany glow, heaven beam, duplagyűrű ───
        if (points >= 500 && tickCounter % 1 == 0) {
            spawnSparks(level, px, py, pz, 4, 1.6);
            // Két gyűrű ellentétes forgással
            spawnRingArc(level, px, py + 0.2, pz, 1.3, ringAngle,      20, true);
            spawnRingArc(level, px, py + 1.0, pz, 1.6, -ringAngle * 0.8f, 16, true);
            spawnGoldenMotes(level, px, py, pz, 6);
            // Heaven beam felfelé
            if (tickCounter % 8 == 0) {
                level.addParticle(ModParticles.HEAVEN_BEAM.get(),
                        px, py + 2.2, pz,
                        0, 0.06, 0);
            }
        }
    }

    private static void spawnSparks(ClientLevel level, double px, double py, double pz,
                                     int count, double radius) {
        for (int i = 0; i < count; i++) {
            double ang = Math.random() * Math.PI * 2;
            double r   = Math.random() * radius;
            double oy  = Math.random() * 2.2;
            level.addParticle(ModParticles.ARCANE_SPARK.get(),
                    px + Math.cos(ang)*r, py + oy, pz + Math.sin(ang)*r,
                    (Math.random()-0.5)*0.02, 0.02, (Math.random()-0.5)*0.02);
        }
    }

    private static void spawnRingArc(ClientLevel level, double cx, double cy, double cz,
                                      double radius, float startAngle, int points,
                                      boolean golden) {
        for (int i = 0; i < points; i++) {
            double ang = Math.toRadians(startAngle + i * 360.0 / points);
            double x   = cx + Math.cos(ang) * radius;
            double z   = cz + Math.sin(ang) * radius;
            if (golden) {
                level.addParticle(ModParticles.GOLDEN_LIGHT.get(), x, cy, z, 0, 0.01, 0);
            } else {
                level.addParticle(ModParticles.RUNE_RING.get(), x, cy, z, 0, 0, 0);
            }
        }
    }

    private static void spawnGoldenMotes(ClientLevel level, double px, double py, double pz,
                                          int count) {
        for (int i = 0; i < count; i++) {
            double ang = Math.random() * Math.PI * 2;
            double r   = 0.5 + Math.random() * 0.8;
            double oy  = 0.5 + Math.random() * 1.5;
            level.addParticle(ModParticles.GOLDEN_LIGHT.get(),
                    px + Math.cos(ang)*r, py + oy, pz + Math.sin(ang)*r,
                    (Math.random()-0.5)*0.01, 0.025, (Math.random()-0.5)*0.01);
        }
    }

    /** Arcane Points cache frissítése (S2C packet kapásakor hívandó). */
    public static void reset() {
        tickCounter = 0;
        ringAngle   = 0f;
    }
}
