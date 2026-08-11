package com.arcanezenith.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Shared, chunk-safe terrain destruction used by the AoE spells that are supposed to
 * actually reshape the ground (Mage's Hammer crater, Cataclysmic Rift fissure,
 * Gravitational Collapse's block-pull). Mirrors how vanilla explosions decide what to
 * destroy - checking blast resistance before removing a block - rather than force-setting
 * air on everything in a radius, which would happily eat bedrock/obsidian/other explosion-
 * proof blocks and could reach into unloaded chunks.
 *
 * Kept intentionally simple (no drops, no TNT-style chain reactions) since this mod's
 * spells don't need those - the point is a plausible, non-destructive-to-hard-blocks crater/
 * rift shape, not a full explosion engine reimplementation.
 */
public final class TerrainDestruction {

    /** Blast resistance above this is treated as "explosion proof" and left alone (matches vanilla's bedrock/obsidian-tier cutoff). */
    private static final float MAX_DESTRUCTIBLE_RESISTANCE = 60.0f;

    private TerrainDestruction() {}

    /**
     * Carves a rough sphere/crater centered at (cx, cy, cz) with the given radius, replacing
     * destructible blocks with air. Skips anything not currently loaded and anything at or
     * above MAX_DESTRUCTIBLE_RESISTANCE (bedrock, end portal frames, obsidian-tier, etc).
     */
    public static void carveCrater(ServerLevel level, double cx, double cy, double cz, double radius) {
        carveShape(level, cx, cy, cz, radius, (dx, dy, dz) -> (dx * dx + dy * dy + dz * dz) <= radius * radius);
    }

    /**
     * Carves a narrow linear fissure along the given horizontal direction (dirX, dirZ should
     * be normalized), `length` blocks long and `width` blocks wide, `depth` blocks deep from
     * the surface downward. Used by Cataclysmic Rift.
     */
    public static void carveFissure(ServerLevel level, double cx, double cy, double cz,
                                     double dirX, double dirZ, double length, double width, double depth) {
        double halfWidth = width / 2.0;
        int steps = (int) Math.ceil(length);
        for (int i = -steps / 2; i <= steps / 2; i++) {
            double px = cx + dirX * i;
            double pz = cz + dirZ * i;
            for (double w = -halfWidth; w <= halfWidth; w += 1.0) {
                // perpendicular offset
                double ox = px - dirZ * w;
                double oz = pz + dirX * w;
                for (int d = 0; d < depth; d++) {
                    tryRemove(level, new BlockPos((int) Math.round(ox), (int) (cy - d), (int) Math.round(oz)));
                }
            }
        }
    }

    private static void carveShape(ServerLevel level, double cx, double cy, double cz, double radius,
                                    TriPredicate withinShape) {
        int r = (int) Math.ceil(radius);
        BlockPos center = BlockPos.containing(cx, cy, cz);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (!withinShape.test(dx, dy, dz)) continue;
                    tryRemove(level, center.offset(dx, dy, dz));
                }
            }
        }
    }

    private static void tryRemove(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return; // never touch unloaded chunks

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        if (state.is(Blocks.BEDROCK)) return; // explicit belt-and-suspenders on top of the resistance check

        float resistance = state.getBlock().getExplosionResistance();
        if (resistance >= MAX_DESTRUCTIBLE_RESISTANCE) return;

        level.removeBlock(pos, false);
    }

    /** Plays a short "crumbling debris" sound/particle cue at the given point - purely cosmetic, called by spells after carving. */
    public static void playCrumbleSound(ServerLevel level, double x, double y, double z) {
        level.playSound(null, x, y, z, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, x, y, z, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    @FunctionalInterface
    private interface TriPredicate {
        boolean test(double dx, double dy, double dz);
    }
}
