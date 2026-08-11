package com.arcanezenith.event;

import com.arcanezenith.spell.TimeSilenceSpell;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Intercepts damage dealt to entities frozen by Time Silence and redirects it into the
 * spell's damage buffer instead of applying it immediately. When time resumes (30s later)
 * the entire buffer detonates at once as the design doc describes.
 *
 * Uses LivingIncomingDamageEvent (the 1.21.x NeoForge name for what was LivingHurtEvent in
 * older versions) because we need to cancel the damage and re-route it, not just observe it.
 */
public class TimeSilenceDamageHandler {

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!TimeSilenceSpell.isFrozen(event.getEntity().getUUID())) return;

        float amount = event.getAmount();
        if (amount <= 0) return;

        // Buffer the damage instead of letting it land
        TimeSilenceSpell.bufferDamage(event.getEntity().getUUID(), amount);
        event.setCanceled(true);
    }
}
