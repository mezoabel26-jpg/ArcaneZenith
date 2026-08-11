package com.arcanezenith.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Raw material drops from the four Arcane Ores.
 * Registered into the same Items DeferredRegister as ModItems.
 * Call OreItems.init() (touching this class) from ArcaneZenith constructor
 * so the static fields are initialised before the register is fired.
 */
public class OreItems {

    public static final DeferredItem<Item> ASTRALIT_CRYSTAL = ModItems.ITEMS.register("astralit_crystal",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> VOID_QUARTZ = ModItems.ITEMS.register("void_quartz",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> IGNIS_PYRITE = ModItems.ITEMS.register("ignis_pyrite",
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));

    public static final DeferredItem<Item> ETHERIUM_SHARD = ModItems.ITEMS.register("etherium_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    /** Touch this class to force static initialisation. */
    public static void init() {}
}
