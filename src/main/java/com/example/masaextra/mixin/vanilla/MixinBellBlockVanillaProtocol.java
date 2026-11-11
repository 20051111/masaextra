package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code BellBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Litematica sends the facing; the attachment type (floor, wall, ceiling)
 * still has to travel, so it is packed into bits 4-5.
 */
@Mixin(BellBlock.class)
public abstract class MixinBellBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int attachmentOrdinal = fromState.getValue(BellBlock.ATTACHMENT).ordinal();
        return protocolValue | (attachmentOrdinal & 0b0000_0011) << 4;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int attachmentOrdinal = (extraProtocolValue & 0b0011_0000) >>> 4;
        return fromState.setValue(BellBlock.ATTACHMENT, BellAttachType.values()[attachmentOrdinal]);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.ADDED;
    }
}
