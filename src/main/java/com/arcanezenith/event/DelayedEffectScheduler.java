package com.arcanezenith.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal tick-based delayed task queue. Not persisted across restarts (fine for VFX-only tasks
 * like "explode this decoy in 2.5s"). Ticked from ManaRegenHandler's server tick listener.
 */
public final class DelayedEffectScheduler {

    private record Task(long dueTick, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static long currentTick = 0L;

    private DelayedEffectScheduler() {}

    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new Task(currentTick + delayTicks, action));
    }

    public static void tick() {
        currentTick++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.dueTick() <= currentTick) {
                due.add(t);
                return true;
            }
            return false;
        });
        for (Task t : due) {
            try {
                t.action().run();
            } catch (Exception e) {
                com.arcanezenith.ArcaneZenith.LOGGER.error("Delayed effect task failed", e);
            }
        }
    }
}
