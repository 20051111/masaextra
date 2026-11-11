package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code DoorBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Facing (2 bits), hinge (1 bit) and open (1 bit). Iron doors ignore the open
 * bit because redstone owns that flag.
 */
@Mixin(DoorBlock.class)
public abstract class MixinDoorBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int facingOrdinal = fromState.getValue(DoorBlock.FACING).ordinal() - 2;
        int hingeOrdinal = fromState.getValue(DoorBlock.HINGE).ordinal();
        boolean isOpen = fromState.getValue(DoorBlock.OPEN); // the client encodes this normally
        return (facingOrdinal & 0b0000_0011)
                | (hingeOrdinal & 0b0000_0001) << 2
                | (isOpen ? 0b0000_1000 : 0b0000_0000);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int facingOrdinal = (extraProtocolValue & 0b0000_0011) + 2;
        int hingeOrdinal = (extraProtocolValue & 0b0000_0100) >>> 2;
        boolean isOpen = (extraProtocolValue & 0b0000_1000) == 0b0000_1000;

        BlockState newState = fromState
                .setValue(DoorBlock.FACING, Direction.values()[facingOrdinal])
                .setValue(DoorBlock.HINGE, DoorHingeSide.values()[hingeOrdinal]);

        // Iron doors are driven by redstone only.
        if (!fromState.getBlock().equals(Blocks.IRON_DOOR)) {
            newState = newState.setValue(DoorBlock.OPEN, isOpen);
        }
        return newState;
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}
