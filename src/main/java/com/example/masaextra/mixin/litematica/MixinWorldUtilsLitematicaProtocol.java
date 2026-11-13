package com.example.masaextra.mixin.litematica;

import com.example.masaextra.ExactPlaceConfig;
import com.example.masaextra.mixin.AttachableAccessor;
import com.example.masaextra.network.ServerSupport;
import com.example.masaextra.protocol.BlockPlacer;
import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.ItemStackProtocolDataAdapter;
import com.example.masaextra.protocol.MultiStageBlockProtocolStateAdapter;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.addExtraProtocolBit;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.decodeProtocolValueFromHitDim;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.encodeExtraProtocolValueToHitVecX;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.encodeProtocolValueToHitVecX;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.encodeProtocolValueToHitVecZ;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.removeExtraProtocolBit;

/**
 * Port of chenjunfu2/EarlyCompat {@code WorldUtilsMixin_LitematicaProtocolCompat}.
 *
 * <p>This is the client half of the exact placement feature: it rewrites the
 * hit vector Litematica sends to the server so the server can decode the exact
 * target state. Three independent things happen here:
 *
 * <ol>
 *   <li>{@code applyCarpetProtocolHitVec}: encode the custom protocol value for
 *       the schematic state, replacing Litematica's V2 vanilla-only encoding.</li>
 *   <li>{@code addExtraProtocol}: OR the additive bits for {@code ADDED}
 *       adapters on top of Litematica's own protocol value.</li>
 *   <li>{@code doEasyPlaceAction}: multi-stage blocks (vine, snow, candles...)
 *       need several clicks, so the whole action is handled here.</li>
 * </ol>
 *
 * <p>Only the 1.20.1 -> 1.20.4 name mapping changed:
 * {@code Vec3d -> Vec3}, {@code MinecraftClient -> Minecraft},
 * {@code Hand -> InteractionHand}, {@code ActionResult -> InteractionResult},
 * {@code World -> Level}, {@code VerticallyAttachableBlockItem -> StandingAndWallBlockItem}.
 */
// priority 800: MasaGadget's fixAccurateProtocol mixin (priority 900) also
// @Injects applyCarpetProtocolHitVec at HEAD with cancellable=true. Mixin runs
// lower priorities first, and a cancelled HEAD callback skips everything after
// it, so without a lower priority here our encoder never ran at all.
@Mixin(value = WorldUtils.class, priority = 800)
@Environment(EnvType.CLIENT)
public abstract class MixinWorldUtilsLitematicaProtocol {
    @Invoker("cacheEasyPlacePosition")
    private static void masaextra$cacheEasyPlacePosition(BlockPos pos) {
        throw new AssertionError();
    }

    private static boolean masaextra$protocolDisabled() {
        return !ExactPlaceConfig.ENABLED.getBooleanValue()
                || !ExactPlaceConfig.USE_SERVER_EXACT.getBooleanValue()
                || !ServerSupport.isSupportsExtraProtocol()
                || !ExactPlaceConfig.EASY_PLACE_V2_PROTOCOL_EXTRA.getBooleanValue();
    }

    /**
     * Full replacement of Litematica's V2 hit-vector encoding.
     *
     * <p>Two shapes are produced:
     * <ul>
     *   <li>wall-attached items keep the plain (non-extra) protocol, because the
     *       low bits are already spent on "wall vs floor" and the facing;</li>
     *   <li>everything else uses the extra protocol with the marker bit.</li>
     * </ul>
     */
    @Inject(
            method = "applyCarpetProtocolHitVec(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private static void masaextra$replaceExtraProtocol(BlockPos pos, BlockState state, Vec3 hitVecIn,
                                                       CallbackInfoReturnable<Vec3> cir) {
        BlockPlacer.debugEncode("HEAD pos={} state={} relX={}", pos, state, hitVecIn.x - pos.getX());
        if (masaextra$protocolDisabled()) {
            BlockPlacer.debugEncode("SKIP disabled server={} extra={}", ServerSupport.isSupportsExtraProtocol(),
                    ExactPlaceConfig.EASY_PLACE_V2_PROTOCOL_EXTRA.getBooleanValue());
            return;
        }

        Block block = state.getBlock();
        BlockPlacer.debugEncode("ENTER pos={} state={} item={}", pos, state, block.asItem());

        // Default behaviour for wall-attached items: bit 0 = wall, bits 1-2 = facing.
        int wallProtocolValue = 0;
        boolean isWallBlock = false;
        if (block.asItem() instanceof StandingAndWallBlockItem attachable) {
            isWallBlock = true;

            if (block.equals(((AttachableAccessor) attachable).masaextra$getWallBlock())) {
                EnumProperty<net.minecraft.core.Direction> dir = BlockPlacer.getFirstDirectionProperty(state);
                if (dir != null) {
                    int facingIndex = state.getValue(dir).ordinal() - 2; // 2-based index
                    wallProtocolValue = (facingIndex & 0b0000_0011) << 1;
                }
                wallProtocolValue |= 0b0000_0001;
            }
        }

        if (!(block instanceof BlockProtocolStateAdapter adapter)) {
            if (isWallBlock) {
                // Not a custom type, but still a wall-attachable item: use the
                // plain protocol so the wall/facing bits survive.
                cir.setReturnValue(encodeProtocolValueToHitVecX(wallProtocolValue, hitVecIn));
                cir.cancel();
            }
            return;
        }

        if (adapter.getProtocolType() != BlockProtocolStateAdapter.ProtocolType.REPLACE) {
            // ADDED adapters are handled in masaextra$addExtraProtocol below.
            return;
        }

        int protocolRawValue = adapter.toProtocolValue(0, state);

        Vec3 returnValue;
        if (isWallBlock) {
            if ((wallProtocolValue & 0b0000_0001) == 0b0000_0001) {
                // Wall variant: three extra low bits are free, append them.
                protocolRawValue = (protocolRawValue << 3) | (wallProtocolValue & 0b0000_0111);
            } else {
                // Floor variant: only the wall/floor bit is used.
                protocolRawValue <<= 1;
            }
            returnValue = encodeProtocolValueToHitVecX(protocolRawValue, hitVecIn);
        } else {
            returnValue = encodeExtraProtocolValueToHitVecX(protocolRawValue, hitVecIn);
        }

        cir.setReturnValue(returnValue);
        cir.cancel();
    }

    /**
     * Additive adapters ride on Litematica's own protocol value. The marker bit
     * tells the server that custom bits are present.
     */
    @ModifyVariable(
            method = "applyCarpetProtocolHitVec(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lfi/dy/masa/litematica/util/WorldUtils;applySlabOrStairHitVecY(DLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)D"
            ),
            name = "protocolValue"
    )
    private static int masaextra$addExtraProtocol(int protocolValue, @Local(argsOnly = true, index = 1) BlockState state) {
        if (masaextra$protocolDisabled()) {
            return protocolValue;
        }

        if (!(state.getBlock() instanceof BlockProtocolStateAdapter adapter)) {
            return protocolValue;
        }

        if (adapter.getProtocolType() != BlockProtocolStateAdapter.ProtocolType.ADDED) {
            return protocolValue;
        }

        return addExtraProtocolBit(adapter.toProtocolValue(protocolValue, state));
    }

    /**
     * Blocks whose item stack carries data (decorated pot, crafter...) encode
     * that data in the Z channel, because X is already used by the block state.
     *
     * <p>Nothing is captured: the released Litematica jar has no
     * LocalVariableTable, and {@code @Local(ordinal=...)} would pick the n-th
     * local of that type rather than the slot we mean. The position and schematic
     * state are re-derived from the current aim instead.
     */
    @ModifyVariable(
            method = "doEasyPlaceAction(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lfi/dy/masa/litematica/util/WorldUtils;applyCarpetProtocolHitVec(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
            ),
            name = "hitPos"
    )
    private static Vec3 masaextra$replaceHitPos(Vec3 hitPos) {
        if (masaextra$protocolDisabled()) {
            return hitPos;
        }

        Minecraft mc = Minecraft.getInstance();
        PlacedTarget target = masaextra$resolveSchematicTarget(mc);
        if (target == null) {
            return hitPos;
        }

        if (!(target.state().getBlock() instanceof ItemStackProtocolDataAdapter adapter)) {
            return hitPos;
        }

        BlockEntity blockEntity = mc.level.getBlockEntity(target.pos());
        if (blockEntity == null) {
            return hitPos;
        }

        // Mirror the block entity's data onto a throw-away stack.
        //
        // 1.21 removed BlockEntity.saveToItem(ItemStack); block entity data now
        // travels as the BLOCK_ENTITY_DATA component. TagValueOutput writes into
        // that shape and BlockItem.setBlockEntityData attaches it, which is the
        // closest equivalent - saveCustomOnly skips position and id metadata the
        // same way saveToItem did.
        ItemStack itemStack = target.state().getBlock().asItem().getDefaultInstance();
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, mc.level.registryAccess());
        blockEntity.saveCustomOnly(output);
        BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), output);

        int protocolAdditionValue = adapter.toProtocolValueAddition(itemStack);
        return encodeProtocolValueToHitVecZ(protocolAdditionValue, hitPos);
    }

    /**
     * Multi-stage blocks: click as many times as needed, then cancel the rest of
     * Litematica's action so it does not click a second time itself.
     */
    @Inject(
            method = "doEasyPlaceAction(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lfi/dy/masa/litematica/util/PlacementHandler;getEffectiveProtocolVersion()Lfi/dy/masa/litematica/util/EasyPlaceProtocol;"
            ),
            cancellable = true
    )
    private static void masaextra$processMultiStageBlock(Minecraft mc, CallbackInfoReturnable<InteractionResult> cir) {
        if (masaextra$protocolDisabled()) {
            return;
        }

        // The whole action is driven from the aimed schematic position, so no
        // Litematica local variables are needed.
        PlacedTarget target = masaextra$resolveSchematicTarget(mc);
        if (target == null) {
            return;
        }

        if (!(target.state().getBlock() instanceof MultiStageBlockProtocolStateAdapter adapter)) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(target.pos())) {
            return;
        }

        // Litematica puts the required item into a hand; use whichever holds it.
        InteractionHand hand = mc.player.getMainHandItem().getItem() == target.state().getBlock().asItem()
                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;
        if (mc.player.getItemInHand(hand).getItem() != target.state().getBlock().asItem()) {
            return; // the item is not in hand yet: let Litematica pick it first
        }

        BlockState stateClient = mc.level.getBlockState(target.pos());

        MultiStageBlockProtocolStateAdapter.LoopContext ctx = new MultiStageBlockProtocolStateAdapter.LoopContext();
        ctx.stateSchematic = target.state();
        ctx.stateClient = stateClient;

        adapter.setLoopCount(ctx);

        int stackCount = Integer.MAX_VALUE;
        if (!mc.player.isCreative()) {
            stackCount = mc.player.getItemInHand(hand).getCount();
        }
        int loopCount = Math.min(stackCount, ctx.loopCount);

        for (int i = 0; i < loopCount; i++) {
            ctx.loopIndex = i;
            int protocolRawValue = adapter.toProtocolValueLoop(ctx);
            // Always the same target position, only the protocol value changes.
            Vec3 protocolHitVec = encodeExtraProtocolValueToHitVecX(protocolRawValue, hit.getLocation());

            BlockHitResult click = new BlockHitResult(protocolHitVec, hit.getDirection(), target.pos(), false);
            mc.gameMode.useItemOn(mc.player, hand, click);
            ctx.stateClient = mc.level.getBlockState(target.pos());
        }

        // Put the position on cooldown so Litematica does not immediately retry.
        masaextra$cacheEasyPlacePosition(target.pos());

        cir.setReturnValue(InteractionResult.SUCCESS);
        cir.cancel();
    }

    /** A schematic position together with the state the schematic wants there. */
    private record PlacedTarget(BlockPos pos, BlockState state) {
    }

    /**
     * Where the player is aiming in the schematic, and what the schematic wants
     * there. Returns null when the aim is not a schematic block.
     */
    private static PlacedTarget masaextra$resolveSchematicTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit)) {
            return null;
        }

        BlockPos clicked = hit.getBlockPos();
        BlockPos target = mc.level.getBlockState(clicked).canBeReplaced()
                ? clicked.relative(hit.getDirection())
                : clicked;

        // Prefer the looked-at position itself when the schematic has a block
        // there; multi-stage blocks grow at the clicked position.
        BlockState atClicked = masaextra$schematicStateAt(clicked);
        if (atClicked != null && !atClicked.isAir()) {
            return new PlacedTarget(clicked, atClicked);
        }

        BlockState atTarget = masaextra$schematicStateAt(target);
        if (atTarget != null && !atTarget.isAir()) {
            return new PlacedTarget(target, atTarget);
        }
        return null;
    }

    private static BlockState masaextra$schematicStateAt(BlockPos pos) {
        var schematicWorld = fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            return null;
        }
        return schematicWorld.getBlockState(pos);
    }
}

