package com.example.masaextra.mixin.litematica;

import com.example.masaextra.ExactPlaceConfig;
import com.example.masaextra.network.ServerSupport;
import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import fi.dy.masa.litematica.util.PlacementHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.decodeProtocolValueFromHitDim;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.extraProtocolValueToRawProtocolValue;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.getRelativeHitX;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.isExtraProtocol;

/**
 * Port of chenjunfu2/EarlyCompat {@code PlacementHandlerMixin_LitematicaProtocolCompat}.
 *
 * <p>Litematica predicts the state a placement will produce so the client can
 * render it immediately. Without this the prediction only knows the vanilla
 * facing, so the block visibly changes once the server answers.
 *
 * <p>1.20.4 simplification: EarlyCompat patched the middle and the return of
 * {@code applyPlacementProtocolV2} with a {@code RETURN ordinal=2, shift=-1}
 * @ModifyVariable. That offset depends on exact bytecode layout and breaks
 * silently across Litematica versions, so this takes over the whole method at
 * HEAD for the states it understands and defers to vanilla otherwise.
 */
@Mixin(PlacementHandler.class)
@Environment(EnvType.CLIENT)
public abstract class MixinPlacementHandlerLitematicaProtocol {
    private static boolean masaextra$protocolDisabled() {
        return !ServerSupport.isSupportsExtraProtocol()
                || !ExactPlaceConfig.EASY_PLACE_V2_PROTOCOL_EXTRA.getBooleanValue();
    }

    @Inject(method = "applyPlacementProtocolV2", cancellable = true, at = @At(value = "HEAD"))
    private static void masaextra$applyExactState(BlockState state, PlacementHandler.UseContext context,
                                                 CallbackInfoReturnable<BlockState> cir) {
        if (masaextra$protocolDisabled()) {
            return;
        }

        Block block = state.getBlock();
        if (!(block instanceof BlockProtocolStateAdapter adapter)) {
            return;
        }

        double relativeHitX = getRelativeHitX(context.getHitVec(), context.getPos());
        int protocolValue = decodeProtocolValueFromHitDim(relativeHitX);
        if (!isExtraProtocol(protocolValue)) {
            return;
        }

        BlockState predicted;
        if (adapter.getProtocolType() == BlockProtocolStateAdapter.ProtocolType.REPLACE) {
            // The value replaces the protocol entirely.
            int rawProtocolValue = extraProtocolValueToRawProtocolValue(protocolValue);
            predicted = adapter.fromProtocolValue(rawProtocolValue, state, context.getItemPlacementContext());
        } else {
            // Additive: let vanilla work out facing/slab/stair first, then layer on
            // the custom bits from the unmodified value.
            predicted = adapter.fromProtocolValue(protocolValue, state, context.getItemPlacementContext());
        }

        if (predicted != null) {
            cir.setReturnValue(predicted);
            cir.cancel();
        }
    }
}
