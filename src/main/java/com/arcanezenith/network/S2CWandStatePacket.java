package com.arcanezenith.network;

import com.arcanezenith.ArcaneZenith;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells the owning client which spell is selected and how far along its cooldown is, right
 * after a cast. The client interpolates the remaining cooldown locally between packets (see
 * ClientWandState) rather than requiring a packet every tick - this is purely a display feed
 * for the diegetic wand-tip HUD, never used for any authoritative cast decision (the server
 * still independently validates cooldown/mana in SpellCastManager regardless of what the
 * client displays).
 */
public record S2CWandStatePacket(ResourceLocation spellId, int cooldownTotalTicks, int cooldownRemainingTicks)
        implements CustomPacketPayload {

    public static final Type<S2CWandStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "wand_state"));

    public static final StreamCodec<ByteBuf, S2CWandStatePacket> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, S2CWandStatePacket::spellId,
            ByteBufCodecs.VAR_INT, S2CWandStatePacket::cooldownTotalTicks,
            ByteBufCodecs.VAR_INT, S2CWandStatePacket::cooldownRemainingTicks,
            S2CWandStatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
