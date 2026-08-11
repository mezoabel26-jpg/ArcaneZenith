package com.arcanezenith.network;

import com.arcanezenith.ArcaneZenith;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Pushes the authoritative server-side mana state to the owning client so the HUD can render
 * it live. Sent on regen tick and immediately after any successful spend (see
 * ModNetworking#syncMana and SpellCastManager#tryCast).
 */
public record S2CSyncManaPacket(float mana, float maxMana, float regenPerSecond) implements CustomPacketPayload {

    public static final Type<S2CSyncManaPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "sync_mana"));

    public static final StreamCodec<ByteBuf, S2CSyncManaPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, S2CSyncManaPacket::mana,
            ByteBufCodecs.FLOAT, S2CSyncManaPacket::maxMana,
            ByteBufCodecs.FLOAT, S2CSyncManaPacket::regenPerSecond,
            S2CSyncManaPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
