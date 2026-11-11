package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.MultiStageBlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code SeaPickleBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Sea pickles hold 1-4, added one per click.
 */
@Mixin(SeaPickleBlock.class)
public abstract class MixinSeaPickleBlockVanillaProtocol
        implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    @Override
    public void setLoopCount(LoopContext ctx) {
        BlockState client = ctx.stateClient;
        boolean isPickle = client != null && client.is(((SeaPickleBlock) (Object) this));
        int current = isPickle ? client.getValue(SeaPickleBlock.PICKLES) : 0;
        int target = ctx.stateSchematic.getValue(SeaPickleBlock.PICKLES);
        ctx.loopCount = target > current ? target - current : 0;
    }

    @Override
    public int toProtocolValueLoop(LoopContext ctx) {
        return (ctx.stateSchematic.getValue(SeaPickleBlock.PICKLES) - 1) & 0b0011;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int maxPickles = (extraProtocolValue & 0b0011) + 1;
        if (fromState.getValue(SeaPickleBlock.PICKLES) > maxPickles) {
            return null;
        }
        return fromState;
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
