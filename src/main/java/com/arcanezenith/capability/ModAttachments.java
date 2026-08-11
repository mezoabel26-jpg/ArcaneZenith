package com.arcanezenith.capability;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.progression.SpellProgress;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArcaneZenith.MOD_ID);

    public static final Supplier<AttachmentType<SpellProgress>> SPELL_PROGRESS =
            ATTACHMENT_TYPES.register("spell_progress", () ->
                    AttachmentType.builder(SpellProgress::new)
                            .serialize(new IAttachmentSerializer<CompoundTag, SpellProgress>() {
                                @Override
                                public @NotNull SpellProgress read(@NotNull IAttachmentHolder holder,
                                        @NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
                                    return SpellProgress.load(tag);
                                }
                                @Override
                                public @NotNull CompoundTag write(@NotNull SpellProgress attachment,
                                        @NotNull HolderLookup.Provider provider) {
                                    return attachment.save();
                                }
                            })
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<ManaData>> MANA =
            ATTACHMENT_TYPES.register("mana", () ->
                    AttachmentType.builder(ManaData::new)
                            .serialize(new IAttachmentSerializer<CompoundTag, ManaData>() {
                                @Override
                                public @NotNull ManaData read(@NotNull IAttachmentHolder holder,
                                        @NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
                                    return ManaData.load(tag);
                                }
                                @Override
                                public @NotNull CompoundTag write(@NotNull ManaData attachment,
                                        @NotNull HolderLookup.Provider provider) {
                                    return attachment.save();
                                }
                            })
                            .copyOnDeath()
                            .build());
}
