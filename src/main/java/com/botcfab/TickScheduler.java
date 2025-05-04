package com.botcfab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TickScheduler {
    private static final List<DelayedBlockSetter> tasks = new LinkedList<>();
    private static final List<Runnable> callbacks = new LinkedList<>();

    public static void schedule(ServerWorld world, BlockPos pos, BlockState state, int delayTicks) {
        tasks.add(new DelayedBlockSetter(world, pos, state, delayTicks));
    }

    public static void scheduleGroup(List<DelayedBlockSetter> newTasks, Runnable onComplete) {
        tasks.addAll(newTasks);
        callbacks.add(onComplete);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<DelayedBlockSetter> it = tasks.iterator();
            while (it.hasNext()) {
                DelayedBlockSetter task = it.next();
                if (task.tick()) {
                    it.remove();
                }
            }

            if (tasks.isEmpty() && !callbacks.isEmpty()) {
                // All scheduled tasks done
                for (Runnable r : callbacks) {
                    r.run();
                }
                callbacks.clear();
            }
        });
    }
}
