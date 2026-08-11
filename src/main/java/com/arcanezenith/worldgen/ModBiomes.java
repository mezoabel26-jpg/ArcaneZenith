package com.arcanezenith.worldgen;

import com.arcanezenith.ArcaneZenith;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Shattered Ley-Line Wastes — arcane energia által torzított overworld biom.
 *
 * Vizuális jellemzők:
 *  - Ég: sötét lila-kék (fog_color, sky_color)
 *  - Víz: sötét teal
 *  - Fű/levelek: szürke-lila
 *  - Ambient: END_ROD particlek (arcane energiaszivárgás)
 *  - Hangulat: cave ambient
 *
 * Generálás: biome_modifier-rel hozzáadva az overworld-höz.
 * A lebegő sziklatömbök és kristályok configured_feature-ként adódnak hozzá.
 */
public final class ModBiomes {

    public static final DeferredRegister<Biome> BIOMES =
            DeferredRegister.create(Registries.BIOME, ArcaneZenith.MOD_ID);

    public static final Supplier<Biome> SHATTERED_LEY_LINE_WASTES =
            BIOMES.register("shattered_ley_line_wastes", ModBiomes::build);

    private static Biome build() {
        // ── Mob spawns ──────────────────────────────────────────────────────
        MobSpawnSettings spawnSettings = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER,
                        new MobSpawnSettings.SpawnerData(EntityType.ENDERMAN, 4, 1, 2))
                .addSpawn(MobCategory.MONSTER,
                        new MobSpawnSettings.SpawnerData(EntityType.WITCH,    2, 1, 1))
                .addSpawn(MobCategory.CREATURE,
                        new MobSpawnSettings.SpawnerData(EntityType.BAT,      2, 1, 4))
                .build();

        // ── Generation — EMPTY, biome_modifier adja hozzá az érc/struktúra feature-öket ──
        BiomeGenerationSettings generationSettings = BiomeGenerationSettings.EMPTY;

        // ── Vizuális effektek ────────────────────────────────────────────────
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(0x1a0a2e)                        // sötét lila köd
                .waterColor(0x1a3344)                      // teal-fekete víz
                .waterFogColor(0x0d1a22)                   // nagyon sötét vízi köd
                .skyColor(0x0e0420)                        // majdnem fekete ég
                .grassColorOverride(0x4a3d5c)              // szürke-lila fű
                .foliageColorOverride(0x3d2e52)            // sötét lila levelek
                .ambientParticle(new AmbientParticleSettings(
                        ParticleTypes.END_ROD, 0.0028f))   // arcane energiakibocsátás
                .ambientLoopSound(SoundEvents.AMBIENT_CAVE)
                .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0))
                .build();

        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.3f)
                .downfall(0.1f)
                .specialEffects(effects)
                .mobSpawnSettings(spawnSettings)
                .generationSettings(generationSettings)
                .build();
    }

    private ModBiomes() {}
}
