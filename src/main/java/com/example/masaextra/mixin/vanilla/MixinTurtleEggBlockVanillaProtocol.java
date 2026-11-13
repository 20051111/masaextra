package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.MultiStageBlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code TurtleEggBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Turtle eggs hold 1-4, added one per click.
 */
@Mixin(TurtleEggBlock.class)
public abstract class MixinTurtleEggBlockVanillaProtocol
        implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    @Override
    public void setLoopCount(LoopContext ctx) {
        BlockState client = ctx.stateClient;
        boolean isEgg = client != null && client.is(((TurtleEggBlock) (Object) this));
        int current = isEgg ? client.getValue(TurtleEggBlock.EGGS) : 0;
        int target = ctx.stateSchematic.getValue(TurtleEggBlock.EGGS);
        ctx.loopCount = target > current ? target - current : 0;
    }

    @Override
    public int toProtocolValueLoop(LoopContext ctx) {
        return (ctx.stateSchematic.getValue(TurtleEggBlock.EGGS) - 1) & 0b0011;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int maxEggs = (extraProtocolValue & 0b0011) + 1;
        if (fromState.getValue(TurtleEggBlock.EGGS) > maxEggs) {
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
