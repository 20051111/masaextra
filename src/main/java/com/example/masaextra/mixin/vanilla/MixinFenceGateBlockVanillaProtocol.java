package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code FenceGateBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Litematica already sends the facing; only the open flag is missing, so this
 * rides along with {@link ProtocolType#ADDED} on a free high bit.
 */
@Mixin(FenceGateBlock.class)
public abstract class MixinFenceGateBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isOpen = fromState.getValue(FenceGateBlock.OPEN);
        return protocolValue | (isOpen ? 0b0001_0000 : 0b0000_0000);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isOpen = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
        return fromState.setValue(FenceGateBlock.OPEN, isOpen);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.ADDED;
    }
}
