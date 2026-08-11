package com.arcanezenith.registry;

import com.arcanezenith.ArcaneZenith;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;


import java.util.function.Supplier;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, ArcaneZenith.MOD_ID);

    public static final Supplier<SoundEvent> ARCANE_BOLT_CAST = register("arcane_bolt_cast");
    public static final Supplier<SoundEvent> ARCANE_BOLT_IMPACT = register("arcane_bolt_impact");
    public static final Supplier<SoundEvent> TELEPORT_DASH = register("teleport_dash");
    public static final Supplier<SoundEvent> TELEPORT_ARRIVE = register("teleport_arrive");

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, name)));
    }
}
