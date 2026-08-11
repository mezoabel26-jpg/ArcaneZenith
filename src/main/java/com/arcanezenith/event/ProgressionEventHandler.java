package com.arcanezenith.event;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ArcaneZenith.MOD_ID)
public class ProgressionEventHandler {

    private static final int POINTS_PER_HOSTILE_KILL = 3;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        var progress = player.getData(ModAttachments.SPELL_PROGRESS);
        progress.addPoints(POINTS_PER_HOSTILE_KILL);
        ModNetworking.syncProgression(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.syncProgression(player);
            ModNetworking.syncMana(player);
            ModNetworking.syncWandStateForSelected(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.syncMana(player);
        }
    }
}
