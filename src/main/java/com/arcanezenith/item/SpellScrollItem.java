package com.arcanezenith.item;

import com.arcanezenith.progression.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A Spell Scroll encodes a single spell ID in its NBT.
 * To be usable, it must be placed in the Arcane Infusion Table together with a Wand.
 * This is what you find in loot chests, buy from traders, or receive as a reward.
 */
public class SpellScrollItem extends Item {

    public static final String NBT_SPELL_ID = "SpellId";

    public SpellScrollItem(Properties props) {
        super(props);
    }

    /** Factory: create a scroll pre-loaded with a given spell ID. */
    public static ItemStack of(ResourceLocation spellId) {
        ItemStack stack = new ItemStack(ModItems.SPELL_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_SPELL_ID, spellId.toString());
        stack.setTag(tag);
        return stack;
    }

    /** @return the spell ID encoded in this scroll, or null if not set. */
    public static ResourceLocation getSpellId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_SPELL_ID)) return null;
        try {
            return ResourceLocation.parse(tag.getString(NBT_SPELL_ID));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        ResourceLocation spellId = getSpellId(stack);
        if (spellId == null) {
            lines.add(Component.literal("(No spell encoded)").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        SpellRegistry.Definition def = SpellRegistry.get(spellId);
        if (def == null) {
            lines.add(Component.literal("Unknown spell: " + spellId).withStyle(ChatFormatting.RED));
            return;
        }
        lines.add(Component.literal("Spell: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(def.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE)));
        lines.add(Component.literal("Tier " + def.tier()).withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("Mana cost: " + def.spell().manaCost()).withStyle(ChatFormatting.AQUA));
        lines.add(Component.empty());
        lines.add(Component.literal("Place in Arcane Infusion Table").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("with your Wand to learn this spell.").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation spellId = getSpellId(stack);
        if (spellId != null) {
            SpellRegistry.Definition def = SpellRegistry.get(spellId);
            if (def != null) {
                return Component.literal(def.displayName() + " Scroll").withStyle(ChatFormatting.LIGHT_PURPLE);
            }
        }
        return Component.literal("Spell Scroll").withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}
