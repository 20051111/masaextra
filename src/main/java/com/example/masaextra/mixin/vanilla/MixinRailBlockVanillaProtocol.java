package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.server.ServerConfig;
import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.PlaceStateAccessor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code RailBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Rail shape is 4 bits; bit 4 asks the placement to skip the post-place shape
 * recalculation (see {@link MixinBaseRailBlockVanillaProtocol}).
 */
@Mixin(RailBlock.class)
public abstract class MixinRailBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int shapeOrdinal = fromState.getValue(RailBlock.SHAPE).ordinal();
        boolean noUpdate = ServerConfig.easyPlaceRailBlockNoShapeUpdate;
        return (shapeOrdinal & 0b0000_1111) | (noUpdate ? 0b0001_0000 : 0b0000_0000);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        // Plain rails have 10 shapes; anything above wraps instead of throwing.
        int shapeOrdinal = (extraProtocolValue & 0b0000_1111) % 10;
        boolean noUpdate = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;

        if (noUpdate && (fromState.getBlock().asItem() instanceof PlaceStateAccessor placement)) {
            placement.setPlaceFlag(PlaceStateAccessor.EASY_PLACE_RAIL_BLOCK_NO_SHAPE_UPDATE);
        }

        return fromState.setValue(RailBlock.SHAPE, RailShape.values()[shapeOrdinal]);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}
