package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.MultiStageBlockProtocolStateAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Port of chenjunfu2/EarlyCompat {@code VineBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Vines are placed one face at a time, so a schematic vine covering three
 * walls needs three clicks on the same position. The adapter collects the
 * missing faces on the client and sends one per click; the server rebuilds each
 * face only when the block behind it can actually hold a vine.
 *
 * <p>1.20.4 note: {@code canSupportAtFace} is private there, so the support test
 * goes through the public {@link VineBlock#isAcceptableNeighbour}.
 */
@Mixin(VineBlock.class)
public abstract class MixinVineBlockVanillaProtocol
        implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    @Unique
    private static final Direction[] MASAEXTRA_DIRECTIONS =
            Arrays.stream(Direction.values())
                    .filter(d -> d != Direction.DOWN)
                    .toArray(Direction[]::new);

    @Unique
    private boolean masaextra$hasDirection(BlockState state, Direction direction) {
        if (state == null || !state.is(((VineBlock) (Object) this))) {
            return false;
        }
        BooleanProperty property = VineBlock.getPropertyForFace(direction);
        return state.hasProperty(property) && state.getValue(property);
    }

    @Override
    public void setLoopCount(LoopContext ctx) {
        List<Integer> required = new ArrayList<>();
        ctx.data = required;

        for (int i = 0; i < MASAEXTRA_DIRECTIONS.length; i++) {
            Direction direction = MASAEXTRA_DIRECTIONS[i];
            // Present in the schematic but missing in the world: one more click.
            if (masaextra$hasDirection(ctx.stateSchematic, direction)
                    && !masaextra$hasDirection(ctx.stateClient, direction)) {
                required.add(i);
                ctx.loopCount++;
            }
        }
    }

    @Override
    public int toProtocolValueLoop(LoopContext ctx) {
        @SuppressWarnings("unchecked")
        List<Integer> required = (List<Integer>) ctx.data;
        if (ctx.loopIndex >= required.size()) {
            // Out of range on the server, which turns it into a no-op.
            return 0b0111;
        }
        return required.get(ctx.loopIndex) & 0b0111;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int index = extraProtocolValue & 0b0111;
        if (index >= MASAEXTRA_DIRECTIONS.length) {
            return null;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState worldState = level.getBlockState(pos);
        return masaextra$withDirection(worldState, level, pos, MASAEXTRA_DIRECTIONS[index]);
    }

    @Unique
    private @Nullable BlockState masaextra$withDirection(BlockState state, BlockGetter level, BlockPos pos,
                                                         Direction direction) {
        BooleanProperty property = VineBlock.getPropertyForFace(direction);
        boolean isVine = state.is(((VineBlock) (Object) this));

        // Already there, or nothing behind to cling to.
        if (isVine && state.getValue(property)) {
            return null;
        }
        if (!VineBlock.isAcceptableNeighbour(level, pos.relative(direction), direction)) {
            return null;
        }

        BlockState base = isVine ? state : ((VineBlock) (Object) this).defaultBlockState();
        return base.setValue(property, true);
    }

    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        return 0;
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}
