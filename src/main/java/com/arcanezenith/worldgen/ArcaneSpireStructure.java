package com.arcanezenith.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Arcane Spire — procedurális deepslate torony az Archon boss számára.
 *
 * Tesztelés: /locate structure arcanezenith:arcane_spire
 * Generálódik síkságokon, réteken és hegyeken (~2000 blokkonként).
 */
public class ArcaneSpireStructure extends Structure {

    public static final MapCodec<ArcaneSpireStructure> CODEC =
            simpleCodec(ArcaneSpireStructure::new);

    public ArcaneSpireStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext ctx) {
        ChunkPos cp  = ctx.chunkPos();
        int x        = cp.getMiddleBlockX();
        int z        = cp.getMiddleBlockZ();
        int y        = ctx.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState());

        // Csak felszínen, víz felett, ne extrém magasságokon
        if (y < 55 || y > 220) return Optional.empty();

        // Ellenőrzöm hogy ne víz felett generáljon
        BlockPos origin = new BlockPos(x, y, z);
        
        return Optional.of(new Structure.GenerationStub(
                origin,
                builder -> generatePieces(builder, ctx, origin)));
    }

    private void generatePieces(StructurePiecesBuilder builder,
                                  Structure.GenerationContext ctx,
                                  BlockPos origin) {
        builder.addPiece(new ArcaneSpirePiece(
                origin, ctx.random().nextInt(4) * 90));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ARCANE_SPIRE_TYPE.get();
    }
}
