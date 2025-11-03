package com.mceteams.xiidays.utils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TaskScheduler {


    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    public static void schedule(int ticks, Runnable action) {
        TASKS.add(new ScheduledTask(ticks, action));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        Iterator<ScheduledTask> it = TASKS.iterator();
        while (it.hasNext()) {
            ScheduledTask task = it.next();
            task.ticks--;
            if (task.ticks <= 0) {
                task.action.run();
                it.remove();
            }
        }
    }

    private static class ScheduledTask {
        public int ticks; // modifiable
        public final Runnable action;

        public ScheduledTask(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }

}
