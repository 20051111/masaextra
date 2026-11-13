package com.example.masaextra.protocol;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Port of chenjunfu2/EarlyCompat {@code BlockProtocolStateAdapter}.
 *
 * <p>Implemented by (mixin-)patched vanilla blocks so the exact placement
 * protocol can carry every property of the block state.
 *
 * <p>Mappings: {@code ItemPlacementContext -> BlockPlaceContext},
 * {@code BlockState.with -> setValue}.
 */
public interface BlockProtocolStateAdapter {
    /**
     * Encode the extra properties of {@code fromState} into a protocol value.
     *
     * @param protocolValue the protocol value produced by Litematica so far
     *                      (only meaningful for {@link ProtocolType#ADDED})
     */
    int toProtocolValue(int protocolValue, BlockState fromState);

    /**
     * Rebuild the exact state from the protocol value. Returning {@code null}
     * means "this placement must fail", which is how the server rejects a
     * value that does not describe a legal state.
     */
    @Nullable
    BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context);

    @NotNull
    ProtocolType getProtocolType();

    enum ProtocolType {
        /**
         * Additive mode: the custom bits are OR-ed on top of Litematica's own
         * protocol value. The protocol marker bit (bit 3) and the vanilla
         * direction bits (bits 0-2) stay reserved.
         */
        ADDED,
        /**
         * Replace mode: the custom value replaces the protocol entirely, so the
         * whole value space is usable.
         */
        REPLACE
    }
}
