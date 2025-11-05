package com.mceteams.xiidays.utils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

@EventBusSubscriber(modid = MODID) // ← AJOUTE CETTE LIGNE
public class TaskScheduler {
    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    public static void schedule(int ticks, Runnable action) {
        TASKS.add(new ScheduledTask(ticks, action));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        Iterator<ScheduledTask> it = TASKS.iterator();
        while (it.hasNext()) {
            ScheduledTask task = it.next();
            task.ticks--;
            if (task.ticks <= 0) {
                try {
                    task.action.run();
                } catch (Exception e) {
                    // Log les erreurs pour éviter le crash
                    System.err.println("Error executing scheduled task: " + e.getMessage());
                    e.printStackTrace();
                }
                it.remove();
            }
        }
    }

    private static class ScheduledTask {
        int ticks;
        Runnable action;
        ScheduledTask(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }
}