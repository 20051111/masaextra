package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.server.ServerConfig;
import com.example.masaextra.protocol.PlaceStateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Port of chenjunfu2/EarlyCompat {@code AbstractRailBlockMixin_VanillaProtocolCompat}.
 *
 * <p>A rail's shape is recomputed from its neighbours right after placement, so
 * a curved or sloped schematic rail immediately snaps back to whatever the world
 * allows - which is exactly the wrong direction in a half-built area. When the
 * placement came from the exact protocol and the "keep exact rail shape" option
 * is on, the update is suppressed once so the schematic shape survives.
 */
@Mixin(BaseRailBlock.class)
public abstract class MixinBaseRailBlockVanillaProtocol {
    @Inject(method = "updateState", at = @At("HEAD"), cancellable = true)
    private void masaextra$cancelShapeUpdate(BlockState state, Level level, BlockPos pos, boolean forceUpdate,
                                             CallbackInfoReturnable<BlockState> cir) {
        if (!(state.getBlock().asItem() instanceof PlaceStateAccessor placement)) {
            return;
        }

        // Only for placements that came from us, not for ordinary player placing.
        if (!placement.isEasyPlaceState()) {
            return;
        }

        if (!placement.hasPlaceFlag(PlaceStateAccessor.EASY_PLACE_RAIL_BLOCK_NO_SHAPE_UPDATE)) {
            return;
        }

        if (!ServerConfig.easyPlaceRailBlockNoShapeUpdate) {
            return;
        }

        // Keep the shape as placed.
        cir.setReturnValue(state);
        cir.cancel();
    }
}
