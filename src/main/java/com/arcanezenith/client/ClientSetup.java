package com.arcanezenith.client;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.client.particle.ArcaneParticleBase;
import com.arcanezenith.client.particle.ModParticles;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = ArcaneZenith.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.OPEN_SPELL_MENU);
        event.register(ModKeybinds.CYCLE_SPELL);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "mana_hud"),
                new ManaHudOverlay()
        );
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(com.arcanezenith.block.ModBlocks.INFUSION_MENU_TYPE.get(),
                com.arcanezenith.screen.ArcaneInfusionScreen::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ARCANE_SPARK.get(),
                ArcaneParticleBase.ArcaneSpark.Provider::new);
        event.registerSpriteSet(ModParticles.RUNE_RING.get(),
                ArcaneParticleBase.RuneRing.Provider::new);
        event.registerSpriteSet(ModParticles.VOID_CORE.get(),
                ArcaneParticleBase.VoidCore.Provider::new);
        event.registerSpriteSet(ModParticles.GOLDEN_LIGHT.get(),
                ArcaneParticleBase.GoldenLight.Provider::new);
        event.registerSpriteSet(ModParticles.PLASMA_BEAM.get(),
                ArcaneParticleBase.PlasmaBeam.Provider::new);
        // New spell-specific particles
        event.registerSpriteSet(ModParticles.THUNDER_SPARK.get(),
                ArcaneParticleBase.ThunderSpark.Provider::new);
        event.registerSpriteSet(ModParticles.HOLY_STAR.get(),
                ArcaneParticleBase.HolyStar.Provider::new);
        event.registerSpriteSet(ModParticles.SHADOW_WISP.get(),
                ArcaneParticleBase.ShadowWisp.Provider::new);
        event.registerSpriteSet(ModParticles.FROST_SHARD.get(),
                ArcaneParticleBase.FrostShard.Provider::new);
        event.registerSpriteSet(ModParticles.LAVA_GEYSER.get(),
                ArcaneParticleBase.LavaGeyser.Provider::new);
        event.registerSpriteSet(ModParticles.PLASMA_SPIRAL.get(),
                ArcaneParticleBase.PlasmaSpiral.Provider::new);
        event.registerSpriteSet(ModParticles.GRAVITY_DUST.get(),
                ArcaneParticleBase.GravityDust.Provider::new);
        event.registerSpriteSet(ModParticles.HEAVEN_BEAM.get(),
                ArcaneParticleBase.HeavenBeam.Provider::new);
        event.registerSpriteSet(ModParticles.SINGULARITY_NOVA.get(),
                ArcaneParticleBase.SingularityNova.Provider::new);
        // Tier 5 legendary spell particles
        event.registerSpriteSet(ModParticles.ELDRITCH_WHIP.get(),   ArcaneParticleBase.ArcaneSpark.Provider::new);
        event.registerSpriteSet(ModParticles.DEATH_FLASH.get(),      ArcaneParticleBase.HolyStar.Provider::new);
        event.registerSpriteSet(ModParticles.METEOR_TRAIL.get(),     ArcaneParticleBase.LavaGeyser.Provider::new);
        event.registerSpriteSet(ModParticles.EXCALIBUR_BEAM.get(),   ArcaneParticleBase.HeavenBeam.Provider::new);
        event.registerSpriteSet(ModParticles.VORTEX_RING.get(),      ArcaneParticleBase.GravityDust.Provider::new);
        event.registerSpriteSet(ModParticles.CHAOS_BLADE.get(),      ArcaneParticleBase.LavaGeyser.Provider::new);
        event.registerSpriteSet(ModParticles.STORM_BOLT.get(),       ArcaneParticleBase.ThunderSpark.Provider::new);
        event.registerSpriteSet(ModParticles.CHIDORI_SPARK.get(),    ArcaneParticleBase.ThunderSpark.Provider::new);
        event.registerSpriteSet(ModParticles.GLINT_DAGGER.get(),     ArcaneParticleBase.FrostShard.Provider::new);
        event.registerSpriteSet(ModParticles.CRIMSON_CHAIN.get(),    ArcaneParticleBase.ArcaneSpark.Provider::new);
    }
}
