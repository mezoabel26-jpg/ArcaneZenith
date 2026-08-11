package com.arcanezenith.network;

import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.client.effect.CameraShake;
import com.arcanezenith.client.effect.PostEffectManager;
import com.arcanezenith.progression.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");

        // C2S: select spell
        reg.playToServer(C2SSelectSpellPacket.TYPE, C2SSelectSpellPacket.CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer player) {
                        var def = SpellRegistry.get(pkt.spellId());
                        if (def == null) return;
                        if (player.getData(ModAttachments.SPELL_PROGRESS).select(pkt.spellId()))
                            syncProgression(player);
                    }
                }));

        // C2S: unlock spell
        reg.playToServer(C2SUnlockSpellPacket.TYPE, C2SUnlockSpellPacket.CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer player) {
                        var def = SpellRegistry.get(pkt.spellId());
                        if (def == null) return;
                        if (player.getData(ModAttachments.SPELL_PROGRESS)
                                .tryUnlock(pkt.spellId(), def.unlockCost()))
                            syncProgression(player);
                    }
                }));

        // S2C: sync progression
        reg.playToClient(S2CSyncProgressionPacket.TYPE, S2CSyncProgressionPacket.CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() ->
                        com.arcanezenith.client.ClientProgressionCache.update(pkt)));

        // S2C: sync mana (for the mana HUD bar)
        reg.playToClient(S2CSyncManaPacket.TYPE, S2CSyncManaPacket.CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() ->
                        com.arcanezenith.client.ClientManaCache.update(pkt)));

        // S2C: sync wand cast state (for the diegetic wand-tip HUD)
        reg.playToClient(S2CWandStatePacket.TYPE, S2CWandStatePacket.CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() ->
                        com.arcanezenith.client.ClientWandState.update(pkt)));

        // S2C: visual effects (camera shake, FOV punch, post-process)
        reg.playToClient(S2CVisualEffectPacket.TYPE, S2CVisualEffectPacket.CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() -> handleVisualEffect(pkt)));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleVisualEffect(S2CVisualEffectPacket pkt) {
        switch (pkt.effectId()) {
            case "shake"          -> CameraShake.startShake(pkt.param1(), (int) pkt.param2());
            case "fov_punch"      -> CameraShake.startFovPunch(pkt.param1(), (int) pkt.param2());
            case "heat_haze"      -> PostEffectManager.activate(PostEffectManager.Effect.HEAT_HAZE,         (int) pkt.param2());
            case "gravity"        -> PostEffectManager.activate(PostEffectManager.Effect.GRAVITY_LENS,      (int) pkt.param2());
            case "time_stop"      -> PostEffectManager.activate(PostEffectManager.Effect.TIME_STOP,         (int) pkt.param2());
            case "lightning_flash"-> PostEffectManager.activate(PostEffectManager.Effect.LIGHTNING_FLASH,   (int) pkt.param2());
            case "holy_bloom"     -> PostEffectManager.activate(PostEffectManager.Effect.HOLY_BLOOM,        (int) pkt.param2());
            case "void_rift"      -> PostEffectManager.activate(PostEffectManager.Effect.VOID_RIFT,         (int) pkt.param2());
            case "blood_curse"    -> { PostEffectManager.bloodCurseGreenPulse = pkt.param1();
                                       PostEffectManager.activate(PostEffectManager.Effect.BLOOD_CURSE,     (int) pkt.param2()); }
            case "stellar_fire"   -> PostEffectManager.activate(PostEffectManager.Effect.STELLAR_FIRE,      (int) pkt.param2());
            case "arcane_overdrive"->PostEffectManager.activate(PostEffectManager.Effect.ARCANE_OVERDRIVE,  (int) pkt.param2());
            case "dark_fantasy"   -> PostEffectManager.setCombatIntensity(pkt.param1());
            case "time_resume" -> {
                PostEffectManager.timeSlopAberration = pkt.param1();
                PostEffectManager.deactivate();
            }
        }
    }

    public static void syncProgression(ServerPlayer player) {
        var p = player.getData(ModAttachments.SPELL_PROGRESS);
        player.connection.send(new S2CSyncProgressionPacket(
                p.getPoints(), List.copyOf(p.getUnlocked()), p.getSelected()));
    }

    /** Call server-side after any change to a player's mana (spend or regen) to push it to their client. */
    public static void syncMana(ServerPlayer player) {
        var mana = player.getData(ModAttachments.MANA);
        player.connection.send(new S2CSyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getRegenPerSecond()));
    }

    /** Call server-side right after a successful cast to update the client's diegetic wand HUD. */
    public static void syncWandState(ServerPlayer player, com.arcanezenith.spell.Spell spell) {
        int remaining = com.arcanezenith.spell.SpellCastManager.ticksRemaining(player, spell);
        player.connection.send(new S2CWandStatePacket(spell.id(), spell.cooldownTicks(), remaining));
    }

    /** Call on login/respawn to give the client an initial wand-HUD state for the currently selected spell. */
    public static void syncWandStateForSelected(ServerPlayer player) {
        var progress = player.getData(ModAttachments.SPELL_PROGRESS);
        var selectedId = progress.getSelected();
        if (selectedId == null) return;
        var def = SpellRegistry.get(selectedId);
        if (def == null) return;
        int remaining = com.arcanezenith.spell.SpellCastManager.ticksRemaining(player, def.spell());
        player.connection.send(new S2CWandStatePacket(selectedId, def.spell().cooldownTicks(), remaining));
    }

    /** Send a visual effect packet to one player. */
    public static void sendEffect(ServerPlayer player, String effectId, float p1, float p2) {
        PacketDistributor.sendToPlayer(player, new S2CVisualEffectPacket(effectId, p1, p2));
    }
}
