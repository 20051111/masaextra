package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code StairsBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Stairs carry facing (2 bits), half (1 bit) and shape (3 bits), which is 6
 * bits - exactly what the extra protocol has room for after its marker bit.
 * The shape matters because a stair can be placed as an outer/inner corner that
 * vanilla would never choose on its own.
 */
@Mixin(StairBlock.class)
public abstract class MixinStairBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int facingOrdinal = fromState.getValue(StairBlock.FACING).ordinal() - 2; // 0..3, 2 bits
        int halfOrdinal = fromState.getValue(StairBlock.HALF).ordinal();          // 0..1, 1 bit
        int shapeOrdinal = fromState.getValue(StairBlock.SHAPE).ordinal();        // 0..4, 3 bits
        return (facingOrdinal & 0b0000_0011)
                | (halfOrdinal & 0b0000_0001) << 2
                | (shapeOrdinal & 0b0000_0111) << 3;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int facingOrdinal = (extraProtocolValue & 0b0000_0011) + 2;
        int halfOrdinal = (extraProtocolValue & 0b0000_0100) >>> 2;
        int shapeOrdinal = (extraProtocolValue & 0b0011_1000) >>> 3;
        return fromState
                .setValue(StairBlock.FACING, Direction.values()[facingOrdinal])
                .setValue(StairBlock.HALF, Half.values()[halfOrdinal])
                .setValue(StairBlock.SHAPE, StairsShape.values()[shapeOrdinal]);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}
