package com.arcanezenith.network;

import com.arcanezenith.ArcaneZenith;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SSelectSpellPacket(ResourceLocation spellId) implements CustomPacketPayload {

    public static final Type<C2SSelectSpellPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "select_spell"));

    public static final StreamCodec<ByteBuf, C2SSelectSpellPacket> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, C2SSelectSpellPacket::spellId,
            C2SSelectSpellPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
