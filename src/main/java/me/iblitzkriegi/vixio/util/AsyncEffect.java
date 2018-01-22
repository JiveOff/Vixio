package me.iblitzkriegi.vixio.util;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.timings.SkriptTimings;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Effects that extend this class are ran asynchronously. Next trigger item will be ran
 * in main server thread, as if there had been a delay before.
 * <p>
 * Majority of Skript and Minecraft APIs are not thread-safe, so be careful.
 */
public abstract class AsyncEffect extends Effect {

    @Override
    @Nullable
    protected TriggerItem walk(Event e) {
        debug(e, true);
        TriggerItem next = getNext();
        Delay.addDelayedEvent(e);
        Bukkit.getScheduler().runTaskAsynchronously(Skript.getInstance(), new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                execute(e); // Execute this effect
                if (next != null) {
                    Bukkit.getScheduler().runTask(Skript.getInstance(), new Runnable() {
                        @Override
                        public void run() { // Walk to next item synchronously
                            Object timing = null;
                            if (SkriptTimings.enabled()) { // getTrigger call is not free, do it only if we must
                                Trigger trigger = getTrigger();
                                if (trigger != null) {
                                    timing = SkriptTimings.start(trigger.getDebugLabel());
                                }
                            }

                            TriggerItem.walk(next, e);

                            SkriptTimings.stop(timing); // Stop timing if it was even started
                        }
                    });
                }
            }
        });
        return null;
    }
}
