package com.arcanezenith.combat;

import com.arcanezenith.ArcaneZenith;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Registry keys for this mod's custom DamageTypes. DamageTypes are a datapack registry in
 * 1.21.x — the actual definitions live in
 * src/main/resources/data/arcanezenith/damage_type/*.json (see ARCANE_PIERCE.json and
 * TRUE_DAMAGE.json). This class only holds the ResourceKeys used to look them up plus small
 * helper factories, following the exact pattern from NeoForge's own damage-types docs.
 *
 * - ARCANE_PIERCE: used by God's Spear. Tagged bypass_armor in its JSON, so
 *   LivingEntity#hurt applies none of the target's armor toughness/value reduction —
 *   a real armor bypass, not a flat damage-bonus approximation.
 * - ARCANE_TRUE: used by Singularity Collapse's supernova burst. Tagged bypass_armor AND
 *   bypass_magic (so magic resistance potions don't reduce it either) — the closest vanilla-
 *   datapack-driven equivalent of "true damage" that ignores all standard mitigation.
 */
public final class ModDamageTypes {

    public static final ResourceKey<net.minecraft.world.damagesource.DamageType> ARCANE_PIERCE =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "arcane_pierce"));

    public static final ResourceKey<net.minecraft.world.damagesource.DamageType> ARCANE_TRUE =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "arcane_true"));

    private ModDamageTypes() {}

    public static DamageSource arcanePierce(ServerLevel level, Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ARCANE_PIERCE),
                attacker, attacker);
    }

    public static DamageSource arcaneTrue(ServerLevel level, Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ARCANE_TRUE),
                attacker, attacker);
    }
}
