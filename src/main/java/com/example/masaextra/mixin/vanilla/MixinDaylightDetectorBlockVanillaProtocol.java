package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat $Doc.
 */
@Mixin(net.minecraft.world.level.block.DaylightDetectorBlock.class)
public abstract class MixinDaylightDetectorBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isInverted = fromState.getValue(net.minecraft.world.level.block.DaylightDetectorBlock.INVERTED);
        return isInverted ? 0b0001 : 0b0000;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isInverted = (extraProtocolValue & 0b0001) == 0b0001;
        return fromState.setValue(net.minecraft.world.level.block.DaylightDetectorBlock.INVERTED, isInverted);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}