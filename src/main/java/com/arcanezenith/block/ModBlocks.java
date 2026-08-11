package com.arcanezenith.block;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.block.entity.ArcaneInfusionBlockEntity;
import com.arcanezenith.menu.ArcaneInfusionMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ArcaneZenith.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArcaneZenith.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, ArcaneZenith.MOD_ID);

    public static final Supplier<Block> ARCANE_INFUSION_TABLE = BLOCKS.register("arcane_infusion_table",
            () -> new ArcaneInfusionTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5f, 10.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(s -> 7)));

    public static final Supplier<BlockEntityType<ArcaneInfusionBlockEntity>> INFUSION_TABLE_BE_TYPE =
            BLOCK_ENTITIES.register("arcane_infusion_table",
                    () -> BlockEntityType.Builder.of(ArcaneInfusionBlockEntity::new,
                            ARCANE_INFUSION_TABLE.get()).build(null));

    public static final Supplier<MenuType<ArcaneInfusionMenu>> INFUSION_MENU_TYPE =
            MENU_TYPES.register("arcane_infusion",
                    () -> IMenuTypeExtension.create((syncId, playerInv, data) ->
                            new ArcaneInfusionMenu(syncId, playerInv,
                                    new net.minecraft.world.SimpleContainer(2))));
}
