package com.arcanezenith.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Base for all Arcane Zenith custom particles.
 * Subclasses set color, lifetime, scale, and gravity. The sprite animates
 * automatically through the particle's registered sprite set.
 */
public abstract class ArcaneParticleBase extends TextureSheetParticle {

    protected ArcaneParticleBase(ClientLevel level, double x, double y, double z,
                                  double dx, double dy, double dz,
                                  SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.setSpriteFromAge(sprites);
        this.pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // Subclasses can override for custom per-tick behavior
    }

    // ──────────────────────────────────────────────────────────────────
    //  ARCANE SPARK  – cyan-purple, fast fade, slight spread
    // ──────────────────────────────────────────────────────────────────
    public static class ArcaneSpark extends ArcaneParticleBase {

        private final SpriteSet sprites;

        protected ArcaneSpark(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 10 + random.nextInt(6);
            this.quadSize = 0.08f + random.nextFloat() * 0.06f;
            this.gravity = -0.02f;
            this.hasPhysics = false;
            setColor(0.7f + random.nextFloat() * 0.3f, 0.2f, 1.0f);
            this.alpha = 0.9f;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            this.alpha = Math.max(0, 1.0f - (float) age / lifetime);
            this.quadSize *= 0.95f;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            public Provider(SpriteSet sprites) { this.sprites = sprites; }
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new ArcaneSpark(level, x, y, z, dx, dy, dz, sprites);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  RUNE RING  – large, rotating purple ring, slow fade
    // ──────────────────────────────────────────────────────────────────
    public static class RuneRing extends ArcaneParticleBase {

        private final SpriteSet sprites;

        protected RuneRing(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 30 + random.nextInt(15);
            this.quadSize = 1.5f + random.nextFloat() * 0.5f;
            this.gravity = 0f;
            this.hasPhysics = false;
            setColor(0.8f, 0.4f, 1.0f);
            this.alpha = 0.75f;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            this.alpha = Math.max(0, 0.75f - 0.75f * ((float) age / lifetime));
            this.oRoll = this.roll;
            this.roll += 0.05f; // continuous rotation
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            public Provider(SpriteSet sprites) { this.sprites = sprites; }
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new RuneRing(level, x, y, z, dx, dy, dz, sprites);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  VOID CORE  – pitch black with purple glow, stationary, pulsing
    // ──────────────────────────────────────────────────────────────────
    public static class VoidCore extends ArcaneParticleBase {

        private final SpriteSet sprites;
        private final float baseSize;

        protected VoidCore(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0, sprites);
            this.sprites = sprites;
            this.lifetime = 20;
            this.baseSize = 0.6f + random.nextFloat() * 0.3f;
            this.quadSize = baseSize;
            this.gravity = 0f;
            this.hasPhysics = false;
            setColor(0.25f, 0.0f, 0.5f);
            this.alpha = 1.0f;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            // Pulse scale
            float pulse = (float)(1.0 + 0.15 * Math.sin(age * 0.4));
            this.quadSize = baseSize * pulse;
            this.alpha = Math.max(0, 1.0f - (float) age / lifetime * 0.5f);
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            public Provider(SpriteSet sprites) { this.sprites = sprites; }
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new VoidCore(level, x, y, z, dx, dy, dz, sprites);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  GOLDEN LIGHT  – radial golden burst, expands quickly, fast fade
    // ──────────────────────────────────────────────────────────────────
    public static class GoldenLight extends ArcaneParticleBase {

        private final SpriteSet sprites;

        protected GoldenLight(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 15 + random.nextInt(8);
            this.quadSize = 0.3f + random.nextFloat() * 0.4f;
            this.gravity = 0.01f;
            this.hasPhysics = false;
            setColor(1.0f, 0.85f + random.nextFloat() * 0.15f, 0.2f);
            this.alpha = 0.95f;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            float frac = (float) age / lifetime;
            this.quadSize *= 1.04f;
            this.alpha = Math.max(0, 0.95f - frac * 0.95f);
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            public Provider(SpriteSet sprites) { this.sprites = sprites; }
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new GoldenLight(level, x, y, z, dx, dy, dz, sprites);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  PLASMA BEAM  – hot white-pink streak, moves fast, short lived
    // ──────────────────────────────────────────────────────────────────
    public static class PlasmaBeam extends ArcaneParticleBase {

        private final SpriteSet sprites;

        protected PlasmaBeam(ClientLevel level, double x, double y, double z,
                              double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 6 + random.nextInt(4);
            this.quadSize = 0.12f + random.nextFloat() * 0.08f;
            this.gravity = 0f;
            this.hasPhysics = false;
            setColor(1.0f, 0.5f + random.nextFloat() * 0.3f, 0.9f);
            this.alpha = 1.0f;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            this.alpha = Math.max(0, 1.0f - (float) age / lifetime);
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            public Provider(SpriteSet sprites) { this.sprites = sprites; }
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new PlasmaBeam(level, x, y, z, dx, dy, dz, sprites);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  THUNDER SPARK — ThunderWar: cyan-white lightning bolt, fast fade
    // ══════════════════════════════════════════════════════════════════════
    public static class ThunderSpark extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected ThunderSpark(ClientLevel level, double x, double y, double z,
                                double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 8 + random.nextInt(4);
            this.quadSize = 0.18f + random.nextFloat() * 0.12f;
            this.gravity = 0f; this.hasPhysics = false;
            setColor(0.7f + random.nextFloat()*0.3f, 0.9f, 1.0f);
            this.alpha = 1.0f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            this.alpha = Math.max(0, 1.0f - (float)age/lifetime);
            this.quadSize *= 0.92f;
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new ThunderSpark(l, x, y, z, dx, dy, dz, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HOLY STAR — AngelsHelp / JudgmentOfHeaven: gold star burst
    // ══════════════════════════════════════════════════════════════════════
    public static class HolyStar extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected HolyStar(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 20 + random.nextInt(10);
            this.quadSize = 0.25f + random.nextFloat() * 0.35f;
            this.gravity = -0.01f; this.hasPhysics = false;
            setColor(1.0f, 0.9f, 0.4f);
            this.alpha = 0.95f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            float frac = (float)age/lifetime;
            this.quadSize *= 1.02f;
            this.alpha = Math.max(0, 0.95f - frac*frac);
            this.oRoll = this.roll; this.roll += 0.04f;
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new HolyStar(l, x, y, z, dx, dy, dz, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SHADOW WISP — Teleport: dark purple dissolving wisp
    // ══════════════════════════════════════════════════════════════════════
    public static class ShadowWisp extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected ShadowWisp(ClientLevel level, double x, double y, double z,
                              double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 18 + random.nextInt(8);
            this.quadSize = 0.2f + random.nextFloat() * 0.2f;
            this.gravity = -0.005f; this.hasPhysics = false;
            setColor(0.45f, 0.1f, 0.75f);
            this.alpha = 0.85f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            this.alpha = Math.max(0, 0.85f * (1f - (float)age/lifetime));
            this.quadSize *= 1.015f;
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new ShadowWisp(l, x, y, z, dx, dy, dz, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FROST SHARD — HordeOfBlades: ice-blue blade fragment
    // ══════════════════════════════════════════════════════════════════════
    public static class FrostShard extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected FrostShard(ClientLevel level, double x, double y, double z,
                              double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 12 + random.nextInt(6);
            this.quadSize = 0.1f + random.nextFloat() * 0.1f;
            this.gravity = 0.03f; this.hasPhysics = false;
            setColor(0.6f, 0.9f, 1.0f);
            this.alpha = 1.0f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            this.alpha = Math.max(0, 1.0f - (float)age/lifetime);
            this.oRoll = this.roll; this.roll += 0.12f;
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new FrostShard(l, x, y, z, dx, dy, dz, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LAVA GEYSER — CataclysmicRift: orange-red upward spray
    // ══════════════════════════════════════════════════════════════════════
    public static class LavaGeyser extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected LavaGeyser(ClientLevel level, double x, double y, double z,
                              double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 16 + random.nextInt(8);
            this.quadSize = 0.22f + random.nextFloat() * 0.18f;
            this.gravity = -0.02f; this.hasPhysics = false;
            float heat = 0.5f + random.nextFloat()*0.5f;
            setColor(1.0f, 0.35f + heat*0.3f, 0.0f);
            this.alpha = 0.95f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            float frac = (float)age/lifetime;
            this.quadSize *= 1.03f;
            this.alpha = Math.max(0, 0.95f*(1f-frac));
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new LavaGeyser(l, x, y, z, dx, dy, dz, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PLASMA SPIRAL — PlasmaAnnihilator: hot-pink rotating spiral
    // ══════════════════════════════════════════════════════════════════════
    public static class PlasmaSpiral extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected PlasmaSpiral(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 10 + random.nextInt(5);
            this.quadSize = 0.14f + random.nextFloat() * 0.1f;
            this.gravity = 0f; this.hasPhysics = false;
            setColor(1.0f, 0.2f + random.nextFloat()*0.2f, 0.85f);
            this.alpha = 1.0f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            this.alpha = Math.max(0, 1.0f - (float)age/lifetime);
            this.oRoll = this.roll; this.roll += 0.25f;
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new PlasmaSpiral(l, x, y, z, dx, dy, dz, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GRAVITY DUST — GravitationalCollapse: tiny dark-purple inward mote
    // ══════════════════════════════════════════════════════════════════════
    public static class GravityDust extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected GravityDust(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 20 + random.nextInt(10);
            this.quadSize = 0.06f + random.nextFloat() * 0.06f;
            this.gravity = 0f; this.hasPhysics = false;
            setColor(0.5f, 0.0f, 0.85f);
            this.alpha = 0.9f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            this.alpha = Math.max(0, 0.9f*(1f-(float)age/lifetime));
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new GravityDust(l, x, y, z, dx, dy, dz, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HEAVEN BEAM — MagesHammer / JudgmentOfHeaven: vertical gold column
    // ══════════════════════════════════════════════════════════════════════
    public static class HeavenBeam extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected HeavenBeam(ClientLevel level, double x, double y, double z,
                              double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 25 + random.nextInt(10);
            this.quadSize = 0.5f + random.nextFloat() * 0.5f;
            this.gravity = 0f; this.hasPhysics = false;
            setColor(1.0f, 0.95f, 0.5f);
            this.alpha = 0.85f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            float frac = (float)age/lifetime;
            this.alpha = Math.max(0, 0.85f*(1f - frac*frac));
            this.quadSize *= 1.01f;
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new HeavenBeam(l, x, y, z, dx, dy, dz, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SINGULARITY NOVA — SingularityCollapse: expanding supernova ring
    // ══════════════════════════════════════════════════════════════════════
    public static class SingularityNova extends ArcaneParticleBase {
        private final SpriteSet sprites;
        protected SingularityNova(ClientLevel level, double x, double y, double z,
                                   double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.sprites = sprites;
            this.lifetime = 30 + random.nextInt(10);
            this.quadSize = 0.8f;
            this.gravity = 0f; this.hasPhysics = false;
            setColor(0.85f, 0.5f, 1.0f);
            this.alpha = 1.0f;
        }
        @Override public void tick() {
            super.tick(); setSpriteFromAge(sprites);
            float frac = (float)age/lifetime;
            this.quadSize += 0.15f;
            this.alpha = Math.max(0, 1.0f - frac*frac*1.2f);
        }
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet s;
            public Provider(SpriteSet s) { this.s = s; }
            @Override public Particle createParticle(SimpleParticleType t, ClientLevel l,
                    double x, double y, double z, double dx, double dy, double dz) {
                return new SingularityNova(l, x, y, z, dx, dy, dz, s);
            }
        }
    }
}
