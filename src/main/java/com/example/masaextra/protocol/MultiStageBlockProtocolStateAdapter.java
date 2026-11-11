package com.example.masaextra.protocol;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of chenjunfu2/EarlyCompat {@code MultiStageBlockProtocolStateAdapter}.
 *
 * <p>For blocks that can only grow one step per placement (vine, glow lichen,
 * snow layers, candles, sea pickles, turtle eggs, pink petals). The client
 * decides how many times to click and which protocol value each click needs.
 */
public interface MultiStageBlockProtocolStateAdapter {
    /** Decide how many placements are required for {@code ctx}. */
    void setLoopCount(LoopContext ctx);

    /** Protocol value for {@code ctx.loopIndex}. */
    int toProtocolValueLoop(LoopContext ctx);

    final class LoopContext {
        @Nullable
        public BlockState stateSchematic;
        @Nullable
        public BlockState stateClient;
        public int loopCount;
        public int loopIndex;
        /** Free slot for per-adapter scratch data. */
        public Object data;
    }
}
