package com.arcanezenith.network;

import com.arcanezenith.ArcaneZenith;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * General-purpose client visual effect trigger.
 * effectId maps to a string key:
 *   "shake"      - camera shake (intensity, duration ticks)
 *   "fov_punch"  - FOV punch (delta, duration ticks)
 *   "heat_haze"  - heat haze post-process (intensity=float1, duration=float2 as ticks)
 *   "gravity"    - gravitational lensing post-process
 *   "time_stop"  - time-stop desaturation
 *   "time_resume"- aberration burst then restore
 */
public record S2CVisualEffectPacket(String effectId, float param1, float param2)
        implements CustomPacketPayload {

    public static final Type<S2CVisualEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "visual_effect"));

    public static final StreamCodec<ByteBuf, S2CVisualEffectPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, S2CVisualEffectPacket::effectId,
            ByteBufCodecs.FLOAT,       S2CVisualEffectPacket::param1,
            ByteBufCodecs.FLOAT,       S2CVisualEffectPacket::param2,
            S2CVisualEffectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
