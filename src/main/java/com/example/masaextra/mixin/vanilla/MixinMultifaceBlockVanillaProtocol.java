package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.MultiStageBlockProtocolStateAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of chenjunfu2/EarlyCompat {@code MultifaceGrowthBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Covers glow lichen and the other multiface growth blocks: each placement
 * adds exactly one face, so the client clicks once per missing face.
 */
@Mixin(MultifaceBlock.class)
public abstract class MixinMultifaceBlockVanillaProtocol
        implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    @Shadow
    @Final
    protected static Direction[] DIRECTIONS;

    @Unique
    private boolean masaextra$hasDirection(BlockState state, Direction direction) {
        if (state == null || !state.is(((MultifaceBlock) (Object) this))) {
            return false;
        }
        return MultifaceBlock.availableFaces(state).contains(direction);
    }

    @Override
    public void setLoopCount(LoopContext ctx) {
        List<Integer> required = new ArrayList<>();
        ctx.data = required;

        for (int i = 0; i < DIRECTIONS.length; i++) {
            Direction direction = DIRECTIONS[i];
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
        if (index >= DIRECTIONS.length) {
            return null;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState worldState = level.getBlockState(pos);

        // Vanilla already knows how to attach one face to the existing block.
        return ((MultifaceBlock) fromState.getBlock()).getStateForPlacement(worldState, level, pos, DIRECTIONS[index]);
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
