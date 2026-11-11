package com.example.masaextra.protocol;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.decodeProtocolValueFromHitDim;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.getRelativeHitX;

/**
 * Verbatim port of chenjunfu2/EarlyCompat {@code BlockPlacer}.
 *
 * <p>Reproduced method for method. The only changes are 1.20.4 names:
 * {@code ItemPlacementContext -> BlockPlaceContext},
 * {@code getHitPos() -> getClickLocation()}, {@code getBlockPos() -> getClickedPos()},
 * {@code getPlacementState() -> getStateForPlacement()},
 * {@code with() -> setValue()}. The {@code debug(...)} calls print nothing unless
 * the debug option is enabled.
 */
public final class BlockPlacer {
    /** Set from the client and server entrypoints; diagnostics only. */
    public static volatile boolean DEBUG_ENABLED = false;

    private BlockPlacer() {
    }

    public static @Nullable BlockState alternativeBlockPlacement(
            Block block, Block wallBlock, Direction verticalAttachmentDirection, BlockPlaceContext context) {
        double relativeHitX = getRelativeHitX(context.getClickLocation(), context.getClickedPos());
        if (relativeHitX < 2.0D) { // not a protocol value
            debug("no protocol relX={} pos={}", relativeHitX, context.getClickedPos());
            return null;
        }

        // bit 0 is reserved for float tolerance; decodeProtocolValueFromHitDim strips it
        int protocolValue = decodeProtocolValueFromHitDim(relativeHitX);
        debug("protocolValue={} bin={}", protocolValue, Integer.toBinaryString(protocolValue));

        BlockState blockState = null;
        boolean isWallType = (protocolValue & 0b0000_0001) == 0b0000_0001;
        protocolValue >>>= 1; // consumed
        if (isWallType) {
            BlockState blockState2 = wallBlock.getStateForPlacement(context);
            if (blockState2 == null) {
                // vanilla WallTorchBlock#getStateForPlacement walks the player's
                // looking directions and returns the first facing whose support
                // block exists - it has nothing to do with the schematic, and it
                // returns null when the schematic's wall is behind the player.
                // EarlyCompat only needs the property set from this call, never its
                // facing (the facing is overwritten below), so fall back to the
                // block's default state exactly as that method starts out.
                blockState2 = wallBlock.defaultBlockState();
                debug("wall getStateForPlacement null; using defaultBlockState {}", blockState2);
            }

            // default protocol: 6 directions, facing starts at index 2
            DirectionProperty directionProperty = getFirstDirectionProperty(blockState2);
            if (directionProperty != null) {
                int facingIndex = (protocolValue & 0b0000_0011) + 2;
                Direction facing = Direction.values()[facingIndex];
                blockState2 = blockState2.setValue(directionProperty, facing);
                debug("wall facing -> {}", facing);
            }
            protocolValue >>>= 2; // consumed

            if (wallBlock instanceof BlockProtocolStateAdapter wallBlockProtocolStateAdapter) {
                blockState = wallBlockProtocolStateAdapter.fromProtocolValue(protocolValue, blockState2, context);
            } else {
                blockState = blockState2;
            }
        } else {
            if (!(block instanceof BlockProtocolStateAdapter blockProtocolStateAdapter)) {
                return null;
            }

            BlockState blockState2 = block.getStateForPlacement(context);

            if (blockState2 == null) {
                debug("floor getStateForPlacement null block={}", block);
                return null;
            }

            blockState = blockProtocolStateAdapter.fromProtocolValue(protocolValue, blockState2, context);
        }

        debug("decoded state={}", blockState);
        return blockState;
    }

    public static @Nullable DirectionProperty getFirstDirectionProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof DirectionProperty) {
                return (DirectionProperty) prop;
            }
        }
        return null;
    }

    /** Diagnostic hook called when canPlace rejected a decoded state. */
    public static void logRejected(BlockState state, BlockPlaceContext context) {
        if (!DEBUG_ENABLED) {
            return;
        }
        org.slf4j.LoggerFactory.getLogger("masaextra").info(
                "[masaextra/proto] canPlace rejected state={} pos={} face={} canSurvive={}",
                state, context.getClickedPos(), context.getClickedFace(),
                state.canSurvive(context.getLevel(), context.getClickedPos()));
    }

    private static void debug(String format, Object... args) {
        if (!DEBUG_ENABLED) {
            return;
        }
        org.slf4j.LoggerFactory.getLogger("masaextra").info("[masaextra/proto] " + format, args);
    }

    /** Client-side encoder diagnostics. */
    public static void debugEncode(String format, Object... args) {
        if (!DEBUG_ENABLED) {
            return;
        }
        org.slf4j.LoggerFactory.getLogger("masaextra").info("[masaextra/encode] " + format, args);
    }
}
