package com.example.masaextra.mixin.carpetextra;

import com.example.masaextra.server.ServerConfig;
import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.server.ExactPlaceServer;
import com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Port of chenjunfu2/EarlyCompat
 * {@code BlockPlacerMixin_CarpetExtraProtocolCompat}.
 *
 * <p>carpet-extra's server-side {@code accurateBlockPlacement} decodes the simple
 * cases (facing, slab half, rotation) from the hit vector. This layers the full
 * block-state protocol on top, but only for players that completed our handshake:
 * otherwise a vanilla client sending a large coordinate would be reinterpreted as
 * a protocol value.
 *
 * <p>1.20.4 simplification: EarlyCompat spliced into the middle of the method and
 * used two @ModifyVariable hooks keyed to a specific bytecode layout and to local
 * variable names. The shipped carpet-extra jar has neither a LocalVariableTable
 * nor a stable layout across versions, so this takes over at HEAD for the states
 * it understands and leaves everything else to carpet-extra untouched.
 *
 * <p>Guarded by the mixin plugin: carpet-extra is optional.
 */
@Mixin(targets = "carpetextra.utils.BlockPlacer")
public abstract class MixinCarpetExtraBlockPlacer {
    @Inject(method = "alternativeBlockPlacement", cancellable = true, at = @At(value = "HEAD"))
    private static void masaextra$applyExactState(Block block, BlockPlaceContext context,
                                                 CallbackInfoReturnable<BlockState> cir) {
        boolean enabled = ServerConfig.easyPlaceV2ProtocolExtra;
        if (!enabled) {
            return;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)
                || !ExactPlaceServer.isExtraProtocolPlayer(player)) {
            return;
        }

        if (!(block instanceof BlockProtocolStateAdapter adapter)) {
            return; // not a block we know how to describe
        }

        // Rebuilt from the arguments: the carpet jar has no local variable table.
        double relativeHitX = context.getClickLocation().x - context.getClickedPos().getX();
        int protocolValue = EasyPlaceExtraProtocolHelper.decodeProtocolValueFromHitDim(relativeHitX);
        if (!EasyPlaceExtraProtocolHelper.isExtraProtocol(protocolValue)) {
            return; // not our protocol; leave carpet-extra's own decoding alone
        }

        BlockState base = block.getStateForPlacement(context);
        if (base == null) {
            return;
        }

        BlockState exact;
        if (adapter.getProtocolType() == BlockProtocolStateAdapter.ProtocolType.REPLACE) {
            int rawProtocolValue = EasyPlaceExtraProtocolHelper.extraProtocolValueToRawProtocolValue(protocolValue);
            exact = adapter.fromProtocolValue(rawProtocolValue, base, context);
        } else {
            exact = adapter.fromProtocolValue(protocolValue, base, context);
        }

        if (exact == null) {
            // The value does not describe a legal state; refuse rather than place
            // a wrong one. Returning null here means "cannot place".
            cir.setReturnValue(null);
        } else {
            cir.setReturnValue(exact);
        }
        cir.cancel();
    }
}
