package com.arcanezenith.client;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.client.effect.CameraShake;
import com.arcanezenith.client.effect.PostEffectManager;
import com.arcanezenith.network.C2SSelectSpellPacket;
import com.arcanezenith.progression.SpellRegistry;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = ArcaneZenith.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // Tick cinematic systems
        CameraShake.tick();
        PostEffectManager.tick();
        ArcaneCodexScreenOpener.tick();
        ArcaneResonanceRenderer.tick();

        while (ModKeybinds.OPEN_SPELL_MENU.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new SpellMenuScreen());
        }

        while (ModKeybinds.CYCLE_SPELL.consumeClick()) {
            cycleToNextUnlockedSpell();
        }
    }

    private static void cycleToNextUnlockedSpell() {
        List<SpellRegistry.Definition> all = SpellRegistry.all();
        if (all.isEmpty()) return;
        var currentId = ClientProgressionCache.getSelected();
        int currentIndex = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(currentId)) { currentIndex = i; break; }
        }
        for (int step = 1; step <= all.size(); step++) {
            int idx = (currentIndex + step) % all.size();
            var candidate = all.get(idx);
            if (ClientProgressionCache.isUnlocked(candidate.id())) {
                PacketDistributor.sendToServer(new C2SSelectSpellPacket(candidate.id()));
                return;
            }
        }
    }
}
