package com.arcanezenith.registry;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.item.ModItems;
import com.arcanezenith.item.OreItems;
import com.arcanezenith.progression.SpellRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ArcaneZenith.MOD_ID);

    public static final Supplier<CreativeModeTab> ARCANE_ZENITH_TAB = TABS.register("arcane_zenith",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.arcanezenith"))
                    .icon(() -> ModItems.ARCANE_WAND.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        // Original tools & misc
                        output.accept(ModItems.ARCANE_WAND.get());
                        output.accept(ModItems.ARCANE_CODEX.get());
                        output.accept(ModItems.ARCANE_SHARD.get());
                        output.accept(ModItems.ARCANE_INFUSION_TABLE.get());
                        // Spell scrolls
                        for (SpellRegistry.Definition def : SpellRegistry.all()) {
                            output.accept(com.arcanezenith.item.SpellScrollItem.of(def.id()));
                        }
                        // Ore materials
                        output.accept(OreItems.ASTRALIT_CRYSTAL.get());
                        output.accept(OreItems.VOID_QUARTZ.get());
                        output.accept(OreItems.IGNIS_PYRITE.get());
                        output.accept(OreItems.ETHERIUM_SHARD.get());
                        // Ore blocks
                        output.accept(ModItems.ASTRALIT_ORE.get());
                        output.accept(ModItems.VOID_QUARTZ_ORE.get());
                        output.accept(ModItems.IGNIS_PYRITE_ORE.get());
                        output.accept(ModItems.ETHERIUM_CRYSTAL_ORE.get());
                    })
                    .build());
}
