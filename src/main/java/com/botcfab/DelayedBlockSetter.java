package com.botcfab;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class DelayedBlockSetter {
    private final ServerWorld world;
    private final BlockPos pos;
    private final BlockState newState;
    private int ticksRemaining;

    public DelayedBlockSetter(ServerWorld world, BlockPos pos, BlockState newState, int delayTicks) {
        this.world = world;
        this.pos = pos;
        this.newState = newState;
        this.ticksRemaining = delayTicks;
    }

    public boolean tick() {
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            world.setBlockState(pos, newState, 3);
            return true;
        }
        return false;
    }
}
