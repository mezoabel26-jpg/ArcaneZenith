package com.arcanezenith.entity;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.entity.boss.ArchonEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ArcaneZenith.MOD_ID);

    /** Arcane Zealot — smart blink-mage, casts elemental barriers, crowd control. */
    public static final Supplier<EntityType<ArcaneZealotEntity>> ARCANE_ZEALOT =
            ENTITY_TYPES.register("arcane_zealot", () ->
                    EntityType.Builder.<ArcaneZealotEntity>of(ArcaneZealotEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(10)
                            .build("arcane_zealot"));

    /** Chrono-Weaver — elite dimension mage, rewinds health, opens rifts. */
    public static final Supplier<EntityType<ChronoWeaverEntity>> CHRONO_WEAVER =
            ENTITY_TYPES.register("chrono_weaver", () ->
                    EntityType.Builder.<ChronoWeaverEntity>of(ChronoWeaverEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(12)
                            .build("chrono_weaver"));

    /** Void-Walker — swaps position with target, freezes projectiles. */
    public static final Supplier<EntityType<VoidWalkerEntity>> VOID_WALKER =
            ENTITY_TYPES.register("void_walker", () ->
                    EntityType.Builder.<VoidWalkerEntity>of(VoidWalkerEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.1f)
                            .clientTrackingRange(12)
                            .build("void_walker"));

    /** Mana-Leech Drake — flying predator, drains mana, breathes plasma. */
    public static final Supplier<EntityType<ManaLeechDrakeEntity>> MANA_LEECH_DRAKE =
            ENTITY_TYPES.register("mana_leech_drake", () ->
                    EntityType.Builder.<ManaLeechDrakeEntity>of(ManaLeechDrakeEntity::new, MobCategory.MONSTER)
                            .sized(1.4f, 0.8f)
                            .clientTrackingRange(16)
                            .build("mana_leech_drake"));

    /** Ethereal Familiar — player companion, collects drops, boosts regen. */
    public static final Supplier<EntityType<EtherealFamiliarEntity>> ETHEREAL_FAMILIAR =
            ENTITY_TYPES.register("ethereal_familiar", () ->
                    EntityType.Builder.<EtherealFamiliarEntity>of(EtherealFamiliarEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(10)
                            .build("ethereal_familiar"));

    /** Archon of the Shattered Sky — 3-phase ultimate boss. */
    public static final Supplier<EntityType<ArchonEntity>> ARCHON =
            ENTITY_TYPES.register("archon", () ->
                    EntityType.Builder.<ArchonEntity>of(ArchonEntity::new, MobCategory.MONSTER)
                            .sized(1.8f, 3.0f)
                            .clientTrackingRange(20)
                            .build("archon"));

    private ModEntities() {}
}
