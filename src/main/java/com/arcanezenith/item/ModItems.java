package com.arcanezenith.item;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.block.ModOres;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneZenith.MOD_ID);

    // ── Original items ────────────────────────────────────────────────────────
    public static final DeferredItem<WandItem> ARCANE_WAND = ITEMS.register("arcane_wand",
            () -> new WandItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<ArcaneShardItem> ARCANE_SHARD = ITEMS.register("arcane_shard",
            () -> new ArcaneShardItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ArcaneCodexItem> ARCANE_CODEX = ITEMS.register("arcane_codex",
            () -> new ArcaneCodexItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<SpellScrollItem> SPELL_SCROLL = ITEMS.register("spell_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<BlockItem> ARCANE_INFUSION_TABLE = ITEMS.register("arcane_infusion_table",
            () -> new BlockItem(com.arcanezenith.block.ModBlocks.ARCANE_INFUSION_TABLE.get(),
                    new Item.Properties()));

    // ── Ore block items (BlockItem wrappers for the four ore blocks) ──────────
    public static final DeferredItem<BlockItem> ASTRALIT_ORE = ITEMS.register("astralit_ore",
            () -> new BlockItem(ModOres.ASTRALIT_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> VOID_QUARTZ_ORE = ITEMS.register("void_quartz_ore",
            () -> new BlockItem(ModOres.VOID_QUARTZ_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> IGNIS_PYRITE_ORE = ITEMS.register("ignis_pyrite_ore",
            () -> new BlockItem(ModOres.IGNIS_PYRITE_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ETHERIUM_CRYSTAL_ORE = ITEMS.register("etherium_crystal_ore",
            () -> new BlockItem(ModOres.ETHERIUM_CRYSTAL_ORE.get(), new Item.Properties()));
}
