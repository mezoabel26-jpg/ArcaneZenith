package com.arcanezenith.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Per-player spell progression: Arcane Points balance, which spells have been unlocked,
 * and which spell is currently "equipped" as the wand's primary cast.
 */
public class SpellProgress {

    private int points = 0;
    private final Set<ResourceLocation> unlocked = new LinkedHashSet<>();
    private ResourceLocation selected;

    public SpellProgress() {
        // Everyone starts with the free starter spell unlocked and selected.
        unlocked.add(com.arcanezenith.spell.ArcaneBoltSpell.ID);
        selected = com.arcanezenith.spell.ArcaneBoltSpell.ID;
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int amount) {
        if (amount <= 0) return;
        points += amount;
    }

    public boolean isUnlocked(ResourceLocation id) {
        return unlocked.contains(id);
    }

    public Set<ResourceLocation> getUnlocked() {
        return unlocked;
    }

    /** @return true if the unlock succeeded (enough points, not already unlocked). */
    public boolean tryUnlock(ResourceLocation id, int cost) {
        if (unlocked.contains(id)) return false;
        if (points < cost) return false;
        points -= cost;
        unlocked.add(id);
        return true;
    }

    public ResourceLocation getSelected() {
        return selected;
    }

    public boolean select(ResourceLocation id) {
        if (!unlocked.contains(id)) return false;
        selected = id;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("points", points);
        tag.putString("selected", selected.toString());
        ListTag list = new ListTag();
        for (ResourceLocation id : unlocked) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put("unlocked", list);
        return tag;
    }

    public static SpellProgress load(CompoundTag tag) {
        SpellProgress data = new SpellProgress();
        data.points = tag.getInt("points");
        if (tag.contains("selected")) {
            data.selected = ResourceLocation.parse(tag.getString("selected"));
        }
        if (tag.contains("unlocked")) {
            data.unlocked.clear();
            ListTag list = tag.getList("unlocked", 8); // 8 = StringTag id
            for (int i = 0; i < list.size(); i++) {
                data.unlocked.add(ResourceLocation.parse(list.getString(i)));
            }
        }
        return data;
    }
}
