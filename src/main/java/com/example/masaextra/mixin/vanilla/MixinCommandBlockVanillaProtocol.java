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
@Mixin(net.minecraft.world.level.block.CommandBlock.class)
public abstract class MixinCommandBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isConditional = fromState.getValue(net.minecraft.world.level.block.CommandBlock.CONDITIONAL);
        return protocolValue | (isConditional ? 0b0001_0000 : 0b0000_0000);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isConditional = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
        return fromState.setValue(net.minecraft.world.level.block.CommandBlock.CONDITIONAL, isConditional);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.ADDED;
    }
}