package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.server.ServerConfig;
import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.PlaceStateAccessor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code DetectorRailBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Detector rails have 6 shapes, so only 3 bits are needed; bit 3 carries the
 * "do not re-shape" request.
 */
@Mixin(DetectorRailBlock.class)
public abstract class MixinDetectorRailBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int shapeOrdinal = fromState.getValue(DetectorRailBlock.SHAPE).ordinal();
        boolean noUpdate = ServerConfig.easyPlaceRailBlockNoShapeUpdate;
        return (shapeOrdinal & 0b0000_0111) | (noUpdate ? 0b0000_1000 : 0b0000_0000);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int shapeOrdinal = (extraProtocolValue & 0b0000_0111) % 6;
        boolean noUpdate = (extraProtocolValue & 0b0000_1000) == 0b0000_1000;

        if (noUpdate && (fromState.getBlock().asItem() instanceof PlaceStateAccessor placement)) {
            placement.setPlaceFlag(PlaceStateAccessor.EASY_PLACE_RAIL_BLOCK_NO_SHAPE_UPDATE);
        }

        return fromState.setValue(DetectorRailBlock.SHAPE, RailShape.values()[shapeOrdinal]);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}
