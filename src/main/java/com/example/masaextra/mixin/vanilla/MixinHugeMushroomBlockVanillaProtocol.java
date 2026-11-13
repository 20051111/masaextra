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
@Mixin(net.minecraft.world.level.block.HugeMushroomBlock.class)
public abstract class MixinHugeMushroomBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        return (fromState.getValue(net.minecraft.world.level.block.HugeMushroomBlock.NORTH) ? 0b0000_0001 : 0)
                | (fromState.getValue(net.minecraft.world.level.block.HugeMushroomBlock.EAST) ? 0b0000_0010 : 0)
                | (fromState.getValue(net.minecraft.world.level.block.HugeMushroomBlock.SOUTH) ? 0b0000_0100 : 0)
                | (fromState.getValue(net.minecraft.world.level.block.HugeMushroomBlock.WEST) ? 0b0000_1000 : 0)
                | (fromState.getValue(net.minecraft.world.level.block.HugeMushroomBlock.UP) ? 0b0001_0000 : 0)
                | (fromState.getValue(net.minecraft.world.level.block.HugeMushroomBlock.DOWN) ? 0b0010_0000 : 0);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        return fromState
                .setValue(net.minecraft.world.level.block.HugeMushroomBlock.NORTH, (extraProtocolValue & 0b0000_0001) == 0b0000_0001)
                .setValue(net.minecraft.world.level.block.HugeMushroomBlock.EAST, (extraProtocolValue & 0b0000_0010) == 0b0000_0010)
                .setValue(net.minecraft.world.level.block.HugeMushroomBlock.SOUTH, (extraProtocolValue & 0b0000_0100) == 0b0000_0100)
                .setValue(net.minecraft.world.level.block.HugeMushroomBlock.WEST, (extraProtocolValue & 0b0000_1000) == 0b0000_1000)
                .setValue(net.minecraft.world.level.block.HugeMushroomBlock.UP, (extraProtocolValue & 0b0001_0000) == 0b0001_0000)
                .setValue(net.minecraft.world.level.block.HugeMushroomBlock.DOWN, (extraProtocolValue & 0b0010_0000) == 0b0010_0000);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}