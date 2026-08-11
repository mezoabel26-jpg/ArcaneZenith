package com.arcanezenith.network;

import com.arcanezenith.ArcaneZenith;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SUnlockSpellPacket(ResourceLocation spellId) implements CustomPacketPayload {

    public static final Type<C2SUnlockSpellPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "unlock_spell"));

    public static final StreamCodec<ByteBuf, C2SUnlockSpellPacket> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, C2SUnlockSpellPacket::spellId,
            C2SUnlockSpellPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
