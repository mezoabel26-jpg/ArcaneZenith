package com.arcanezenith.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * ArcaneSpirePiece — procedurális deepslate torony.
 * placeBlock(WorldGenLevel, BlockState, int, int, int, BoundingBox) helyes API.
 */
public class ArcaneSpirePiece extends StructurePiece {

    private static final int W = 7;
    private static final int H = 22;

    private static final BlockState BRICK  = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState CRACK  = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState TILE   = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState OBS    = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState AME    = Blocks.AMETHYST_BLOCK.defaultBlockState();
    private static final BlockState AIR    = Blocks.AIR.defaultBlockState();
    private static final BlockState LANTERN= Blocks.SOUL_LANTERN.defaultBlockState();
    private static final BlockState CHAIN  = Blocks.CHAIN.defaultBlockState();
    private static final BlockState BARS   = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState CANDLE = Blocks.PURPLE_CANDLE.defaultBlockState()
            .setValue(CandleBlock.LIT, true);

    // Origin stored as ints (NBT safe)
    private final int ox, oy, oz;

    public ArcaneSpirePiece(BlockPos origin, int rotation) {
        super(ModStructures.ARCANE_SPIRE_PIECE, 0, new BoundingBox(
                origin.getX() - 6, origin.getY() - 3, origin.getZ() - 6,
                origin.getX() + 6, origin.getY() + H + 6, origin.getZ() + 6));
        this.ox = origin.getX();
        this.oy = origin.getY();
        this.oz = origin.getZ();
    }

    public ArcaneSpirePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.ARCANE_SPIRE_PIECE, tag);
        this.ox = tag.getInt("ox");
        this.oy = tag.getInt("oy");
        this.oz = tag.getInt("oz");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("ox", ox); tag.putInt("oy", oy); tag.putInt("oz", oz);
    }

    // Helper: placeBlock with absolute BlockPos -> convert to relative ints
    private void place(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        // StructurePiece.placeBlock wants: level, state, relX, relY, relZ, box
        // But we work with absolute coords -> use level.setBlock directly if in bounds
        BlockPos pos = new BlockPos(x, y, z);
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    @Override
    public void postProcess(WorldGenLevel level,
                             net.minecraft.world.level.StructureManager sm,
                             net.minecraft.world.level.chunk.ChunkGenerator gen,
                             RandomSource rng,
                             BoundingBox box,
                             ChunkPos cp,
                             BlockPos pivot) {
        foundation(level, box, rng);
        tower(level, box, rng);
        roof(level, box);
        loot(level, box, rng);
    }

    private void foundation(WorldGenLevel level, BoundingBox box, RandomSource rng) {
        int half = W / 2 + 1;
        for (int dy = -3; dy <= 0; dy++) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    place(level, dy == 0 ? TILE : OBS, ox+dx, oy+dy, oz+dz, box);
                }
            }
        }
        for (int[] c : new int[][]{{-half+1,-half+1},{-half+1,half-1},{half-1,-half+1},{half-1,half-1}}) {
            place(level, AME, ox+c[0], oy+1, oz+c[1], box);
        }
    }

    private void tower(WorldGenLevel level, BoundingBox box, RandomSource rng) {
        int half = W / 2;
        for (int y = 1; y <= H; y++) {
            boolean cren   = (y == H);
            boolean window = (y % 3 == 0 && y > 2 && y < H - 1);
            boolean floor  = (y % 5 == 0);
            boolean bast   = (y == 5 || y == 10 || y == 15);

            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    boolean wall = (Math.abs(dx) == half || Math.abs(dz) == half);
                    if (wall) {
                        BlockState bs = rng.nextInt(5) == 0 ? CRACK : BRICK;
                        if (cren && (dx + dz) % 2 == 0) bs = AIR;
                        if (window && ((dx == 0 && Math.abs(dz) == half) || (dz == 0 && Math.abs(dx) == half))) bs = BARS;
                        place(level, bs, ox+dx, oy+y, oz+dz, box);
                    } else {
                        place(level, AIR, ox+dx, oy+y, oz+dz, box);
                    }
                }
            }

            // Spiral stair
            double a = y * (Math.PI * 2.0 / 8.0);
            place(level, TILE, ox + (int)Math.round(Math.cos(a)*2), oy+y, oz + (int)Math.round(Math.sin(a)*2), box);

            // Floor every 5 levels
            if (floor && y < H) {
                for (int dx = -(half-1); dx <= half-1; dx++)
                    for (int dz = -(half-1); dz <= half-1; dz++)
                        place(level, TILE, ox+dx, oy+y, oz+dz, box);
                place(level, CHAIN,  ox, oy+y+1, oz, box);
                place(level, LANTERN,ox, oy+y,   oz, box);
            }

            // Bastions
            if (bast) {
                for (int[] c : new int[][]{{-4,-4},{-4,4},{4,-4},{4,4}}) {
                    for (int dx = c[0]-1; dx <= c[0]+1; dx++)
                        for (int dz = c[1]-1; dz <= c[1]+1; dz++)
                            place(level, BRICK, ox+dx, oy+y, oz+dz, box);
                    place(level, AME,   ox+c[0], oy+y+1, oz+c[1], box);
                    place(level, CANDLE,ox+c[0], oy+y+2, oz+c[1], box);
                }
            }

            // Candles
            if (y % 4 == 2) {
                for (int[] p : new int[][]{{-2,0},{2,0},{0,-2},{0,2}})
                    place(level, CANDLE, ox+p[0], oy+y, oz+p[1], box);
            }
        }
    }

    private void roof(WorldGenLevel level, BoundingBox box) {
        int ry = oy + H + 1;
        for (int dx = -5; dx <= 5; dx++)
            for (int dz = -5; dz <= 5; dz++)
                place(level, (Math.abs(dx)==5||Math.abs(dz)==5) ? BRICK : TILE, ox+dx, ry, oz+dz, box);
        for (int[] c : new int[][]{{-5,-5},{-5,5},{5,-5},{5,5}})
            for (int dy = 1; dy <= 4; dy++)
                place(level, AME, ox+c[0], ry+dy, oz+c[1], box);
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI * 2.0 / 12.0;
            place(level, AME, ox+(int)Math.round(Math.cos(ang)*3), ry, oz+(int)Math.round(Math.sin(ang)*3), box);
        }
    }

    private void loot(WorldGenLevel level, BoundingBox box, RandomSource rng) {
        placeChest(level, box, rng, ox+2, oy+6, oz+2, BuiltInLootTables.SIMPLE_DUNGEON);
        placeChest(level, box, rng, ox-2, oy+11, oz-2, BuiltInLootTables.STRONGHOLD_LIBRARY);
        var customKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath("arcanezenith","chests/arcane_spire_top"));
        placeChest(level, box, rng, ox+1, oy+H+1, oz+1, customKey);
    }

    private void placeChest(WorldGenLevel level, BoundingBox box, RandomSource rng,
                              int x, int y, int z,
                              ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootKey) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity cbe)
            cbe.setLootTable(lootKey, rng.nextLong());
    }
}
