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
@Mixin(net.minecraft.world.level.block.SkullBlock.class)
public abstract class MixinSkullBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int rotation = fromState.getValue(net.minecraft.world.level.block.SkullBlock.ROTATION);
        return rotation & 0b0000_1111;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int rotation = extraProtocolValue & 0b0000_1111;
        return fromState.setValue(net.minecraft.world.level.block.SkullBlock.ROTATION, rotation);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}