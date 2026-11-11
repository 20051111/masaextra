package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code LeverBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Litematica already sends the facing, so only the attachment face (bits 4-5)
 * and the powered flag (bit 6) are added.
 *
 * <p>1.20.4 note: EarlyCompat also injected a neighbour update into
 * {@code onPlaced}. That is not portable here - {@code LeverBlock} does not
 * override {@code setPlacedBy}/{@code onPlace} in 1.20.4 (it inherits them from
 * {@code Block}), and Mixin cannot inject into a method the target class does not
 * declare, which failed with "could not find any targets matching method_9567".
 * The behaviour it provided is not needed either: lever placement goes through
 * {@code Block.updateNeighboursOnBlockPlaced}, which already notifies the six
 * surrounding positions, so the block a powered lever hangs on is updated.
 */
@Mixin(LeverBlock.class)
public abstract class MixinLeverBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int faceOrdinal = fromState.getValue(LeverBlock.FACE).ordinal();
        boolean isPowered = fromState.getValue(LeverBlock.POWERED);
        return protocolValue
                | (faceOrdinal & 0b0000_0011) << 4
                | (isPowered ? 0b0100_0000 : 0b0000_0000);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int faceOrdinal = ((extraProtocolValue & 0b0011_0000) >>> 4) % 3;
        boolean isPowered = (extraProtocolValue & 0b0100_0000) == 0b0100_0000;
        return fromState
                .setValue(LeverBlock.FACE, AttachFace.values()[faceOrdinal])
                .setValue(LeverBlock.POWERED, isPowered);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.ADDED;
    }
}
