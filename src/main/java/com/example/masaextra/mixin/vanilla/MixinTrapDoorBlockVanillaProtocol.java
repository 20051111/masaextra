package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code TrapdoorBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Facing (2 bits), half (1 bit) and open (1 bit). Iron trapdoors are excluded
 * from the open bit because the server toggles them by redstone, so forcing the
 * flag would fight the door's own logic.
 */
@Mixin(TrapDoorBlock.class)
public abstract class MixinTrapDoorBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int facingOrdinal = fromState.getValue(TrapDoorBlock.FACING).ordinal() - 2;
        int halfOrdinal = fromState.getValue(TrapDoorBlock.HALF).ordinal();
        boolean isOpen = fromState.getValue(TrapDoorBlock.OPEN); // the client encodes this normally
        return (facingOrdinal & 0b0000_0011)
                | (halfOrdinal & 0b0000_0001) << 2
                | (isOpen ? 0b0000_1000 : 0b0000_0000);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int facingOrdinal = (extraProtocolValue & 0b0000_0011) + 2;
        int halfOrdinal = (extraProtocolValue & 0b0000_0100) >>> 2;
        boolean isOpen = (extraProtocolValue & 0b0000_1000) == 0b0000_1000;

        BlockState newState = fromState
                .setValue(TrapDoorBlock.FACING, Direction.values()[facingOrdinal])
                .setValue(TrapDoorBlock.HALF, Half.values()[halfOrdinal]);

        // Iron trapdoors are opened by redstone only; leave the flag alone.
        if (!fromState.getBlock().equals(Blocks.IRON_TRAPDOOR)) {
            newState = newState.setValue(TrapDoorBlock.OPEN, isOpen);
        }
        return newState;
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}
