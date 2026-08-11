package com.arcanezenith.block;

import com.arcanezenith.ArcaneZenith;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Four Arcane Ores from the design doc:
 *  - Astralit Ore          deepslate-tier, blue-purple glow, drops Astralit Crystal
 *  - Void-Quartz Ore       deepslate-tier, dark void tones, drops Void Quartz
 *  - Ignis Pyrite Ore      stone-tier, fiery red-orange, drops Ignis Pyrite
 *  - Etherium Crystal Ore  deepslate-tier, pale cyan glow, drops Etherium Shard
 *
 * World generation is handled by data-driven BiomeModifier JSON (no Java needed).
 * Call ModOres.init() from the mod constructor to trigger class loading before
 * the DeferredRegister fires.
 */
public class ModOres {

    /** Separate register — avoids touching ModBlocks static init at the wrong time. */
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ArcaneZenith.MOD_ID);

    // ── Ore blocks ───────────────────────────────────────────────────────────

    public static final Supplier<Block> ASTRALIT_ORE = BLOCKS.register("astralit_ore",
            () -> new DropExperienceBlock(
                    UniformInt.of(3, 7),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .strength(4.5f, 12.0f)
                            .sound(SoundType.DEEPSLATE)
                            .lightLevel(s -> 6)
                            .requiresCorrectToolForDrops()));

    public static final Supplier<Block> VOID_QUARTZ_ORE = BLOCKS.register("void_quartz_ore",
            () -> new DropExperienceBlock(
                    UniformInt.of(2, 5),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(4.5f, 12.0f)
                            .sound(SoundType.DEEPSLATE)
                            .lightLevel(s -> 3)
                            .requiresCorrectToolForDrops()));

    public static final Supplier<Block> IGNIS_PYRITE_ORE = BLOCKS.register("ignis_pyrite_ore",
            () -> new DropExperienceBlock(
                    UniformInt.of(1, 4),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(3.0f, 8.0f)
                            .sound(SoundType.STONE)
                            .lightLevel(s -> 4)
                            .requiresCorrectToolForDrops()));

    public static final Supplier<Block> ETHERIUM_CRYSTAL_ORE = BLOCKS.register("etherium_crystal_ore",
            () -> new DropExperienceBlock(
                    UniformInt.of(4, 9),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(5.0f, 15.0f)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(s -> 8)
                            .requiresCorrectToolForDrops()));

    /** Touch to force static init before the register fires. */
    public static void init() {}
}
