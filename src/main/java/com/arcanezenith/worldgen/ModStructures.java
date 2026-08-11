package com.arcanezenith.worldgen;

import com.arcanezenith.ArcaneZenith;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, ArcaneZenith.MOD_ID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, ArcaneZenith.MOD_ID);

    /** A teljes Arcane Spire struktúra típusa */
    public static final Supplier<StructureType<ArcaneSpireStructure>> ARCANE_SPIRE_TYPE =
            STRUCTURE_TYPES.register("arcane_spire",
                    () -> () -> ArcaneSpireStructure.CODEC);

    /** Az Arcane Spire egyedi piece típusa */
    public static final StructurePieceType ARCANE_SPIRE_PIECE =
            register("arcane_spire_piece", ArcaneSpirePiece::new);

    private static StructurePieceType register(String name, StructurePieceType type) {
        STRUCTURE_PIECE_TYPES.register(name, () -> type);
        return type;
    }

    private ModStructures() {}
}
