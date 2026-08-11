package com.arcanezenith.network;

import com.arcanezenith.ArcaneZenith;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record S2CSyncProgressionPacket(int points, List<ResourceLocation> unlocked, ResourceLocation selected)
        implements CustomPacketPayload {

    public static final Type<S2CSyncProgressionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "sync_progression"));

    public static final StreamCodec<ByteBuf, S2CSyncProgressionPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2CSyncProgressionPacket::points,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), S2CSyncProgressionPacket::unlocked,
            ResourceLocation.STREAM_CODEC, S2CSyncProgressionPacket::selected,
            S2CSyncProgressionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
