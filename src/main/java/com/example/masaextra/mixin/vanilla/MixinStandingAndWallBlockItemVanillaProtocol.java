package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockPlacer;
import com.example.masaextra.protocol.CarpetExtraCompat;
import com.example.masaextra.protocol.ClientOptions;
import com.example.masaextra.server.ExactPlaceServer;
import com.example.masaextra.server.ServerConfig;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verbatim port of chenjunfu2/EarlyCompat
 * {@code VerticallyAttachableBlockItemMixin_VanillaProtocolCompat}.
 *
 * <p>Original (1.20.1):
 * <pre>
 * void getAlternatePlacement(ItemPlacementContext context, CallbackInfoReturnable&lt;BlockState&gt; cir) {
 *     if (IS_SERVER_ENV) {
 *         if (!(context.getPlayer() instanceof ServerPlayerEntity player)
 *                 || !EarlyCompatC2ServerHandler.isExtraProtocolPlayer(player)
 *                 || !CarpetExtraSettingsAccessor.getAccurateBlockPlacement()) {
 *             return;
 *         }
 *     }
 *
 *     BlockState tryAlternative = BlockPlacer.alternativeBlockPlacement(
 *             this.getBlock(), wallBlock, verticalAttachmentDirection, context);
 *     if (tryAlternative == null) {
 *         return;
 *     }
 *
 *     if (!this.canPlace(context, tryAlternative)) {
 *         tryAlternative = null;
 *     }
 *
 *     cir.setReturnValue(tryAlternative);
 *     cir.cancel();
 * }
 * </pre>
 *
 * <p>Name mapping only: {@code ItemPlacementContext -> BlockPlaceContext},
 * {@code ServerPlayerEntity -> ServerPlayer}, {@code Settings -> Item.Properties},
 * {@code verticalAttachmentDirection -> attachmentDirection}.
 *
 * <p>The one deviation is the client branch. EarlyCompat's {@code IS_SERVER_ENV}
 * means "dedicated server", so a singleplayer or LAN host counts as a client and
 * runs this unconditionally; 1.20.4 has no equivalent constant and
 * {@code isClientSide()} is true in integrated servers too. The client therefore
 * checks the same option that gates the rest of the protocol, so the client's
 * prediction matches what the server will do. The handler body is unchanged.
 */
@Mixin(StandingAndWallBlockItem.class)
public abstract class MixinStandingAndWallBlockItemVanillaProtocol extends BlockItem {
    @Final
    @Shadow
    protected Block wallBlock;
    @Final
    @Shadow
    private Direction attachmentDirection;

    public MixinStandingAndWallBlockItemVanillaProtocol(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void masaextra$getAlternatePlacement(BlockPlaceContext context,
                                                 CallbackInfoReturnable<BlockState> cir) {
        // this mixin is registered for both sides, and ExactPlaceConfig lives in
        // malilib which a dedicated server does not have. Client-only options may
        // only be read inside the client branch.
        boolean clientSide = context.getLevel().isClientSide();

        // EarlyCompat: if (IS_SERVER_ENV) { ... }
        if (!clientSide) {
            if (!(context.getPlayer() instanceof ServerPlayer player)
                    || !ExactPlaceServer.isExtraProtocolPlayer(player)
                    || !CarpetExtraCompat.getAccurateBlockPlacement()) {
                return;
            }
        } else if (!ClientOptions.enabled
                || !ClientOptions.useServerExact
                || !ServerConfig.easyPlaceV2ProtocolExtra) {
            // See the class note: integrated servers are "client side" here.
            return;
        }

        // EarlyCompat: BlockPlacer.alternativeBlockPlacement(...)
        BlockState tryAlternative = BlockPlacer.alternativeBlockPlacement(
                this.getBlock(), wallBlock, attachmentDirection, context);
        if (tryAlternative == null) {
            return;
        }

        // EarlyCompat: if (!this.canPlace(context, tryAlternative)) tryAlternative = null;
        if (!this.canPlace(context, tryAlternative)) {
            BlockPlacer.logRejected(tryAlternative, context);
            // The server always refuses (EarlyCompat's original behaviour), since it
            // is authoritative and must not substitute a different block. Only the
            // client may opt into letting vanilla fall back.
            if (!clientSide || ClientOptions.suppressMissingSupport) {
                tryAlternative = null;
            } else {
                return;
            }
        }

        cir.setReturnValue(tryAlternative);
        cir.cancel();
    }
}
