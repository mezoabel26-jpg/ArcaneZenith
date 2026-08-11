package com.arcanezenith.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/**
 * Simple mutable mana container attached to a player via the NeoForge Data Attachment API.
 * Kept intentionally simple (no capability boilerplate) since 1.21.x prefers attachments
 * for small pieces of persistent entity data.
 */
public class ManaData {

    public static final float DEFAULT_MAX_MANA = 100.0f;
    public static final float DEFAULT_REGEN_PER_SECOND = 1.5f;

    private float mana;
    private float maxMana;
    private float regenPerSecond;

    public ManaData() {
        this(DEFAULT_MAX_MANA, DEFAULT_MAX_MANA, DEFAULT_REGEN_PER_SECOND);
    }

    public ManaData(float mana, float maxMana, float regenPerSecond) {
        this.mana = mana;
        this.maxMana = maxMana;
        this.regenPerSecond = regenPerSecond;
    }

    public float getMana() {
        return mana;
    }

    public float getMaxMana() {
        return maxMana;
    }

    public float getRegenPerSecond() {
        return regenPerSecond;
    }

    /** @return true if there was enough mana and it was spent. */
    public boolean spend(float amount) {
        if (amount <= 0f) return true;
        if (mana < amount) return false;
        mana -= amount;
        return true;
    }

    public void restore(float amount) {
        mana = Math.min(maxMana, mana + amount);
    }

    public void tickRegen(float seconds) {
        restore(regenPerSecond * seconds);
    }

    public void setMaxMana(float maxMana) {
        this.maxMana = maxMana;
        this.mana = Math.min(this.mana, this.maxMana);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("mana", mana);
        tag.putFloat("maxMana", maxMana);
        tag.putFloat("regen", regenPerSecond);
        return tag;
    }

    public static ManaData load(CompoundTag tag) {
        return new ManaData(
                tag.getFloat("mana"),
                tag.getFloat("maxMana"),
                tag.getFloat("regen")
        );
    }

    public ManaData copy() {
        return new ManaData(mana, maxMana, regenPerSecond);
    }
}
