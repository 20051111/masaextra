package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code RedstoneWireBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Only the "dot" shape matters: a wire with no connections is drawn as a dot
 * and vanilla will happily connect it to neighbouring wires. The schematic
 * dot has to be forced back.
 */
@Mixin(RedStoneWireBlock.class)
public abstract class MixinRedStoneWireBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isDot = fromState.getValue(RedStoneWireBlock.SOUTH) == RedstoneSide.NONE
                && fromState.getValue(RedStoneWireBlock.WEST) == RedstoneSide.NONE
                && fromState.getValue(RedStoneWireBlock.NORTH) == RedstoneSide.NONE
                && fromState.getValue(RedStoneWireBlock.EAST) == RedstoneSide.NONE;
        return isDot ? 0b0001 : 0b0000;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isDot = (extraProtocolValue & 0b0001) == 0b0001;
        return isDot
                ? fromState
                        .setValue(RedStoneWireBlock.EAST, RedstoneSide.NONE)
                        .setValue(RedStoneWireBlock.WEST, RedstoneSide.NONE)
                        .setValue(RedStoneWireBlock.NORTH, RedstoneSide.NONE)
                        .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.NONE)
                : fromState;
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}
