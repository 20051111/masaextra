package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.MultiStageBlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code CandleBlockMixin_VanillaProtocolCompat}.
 *
 * <p>A candle block holds 1-4 candles and one placement only ever adds one, so
 * this adapter also drives the multi-click loop: click until the schematic count
 * is reached. Each click carries the target count, which the server checks to
 * reject "grow beyond the schematic" values.
 */
@Mixin(CandleBlock.class)
public abstract class MixinCandleBlockVanillaProtocol
        implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    @Override
    public void setLoopCount(LoopContext ctx) {
        BlockState client = ctx.stateClient;
        boolean isCandle = client != null && client.is(((CandleBlock) (Object) this));
        int current = isCandle ? client.getValue(CandleBlock.CANDLES) : 0;
        int target = ctx.stateSchematic.getValue(CandleBlock.CANDLES);
        ctx.loopCount = target > current ? target - current : 0;
    }

    @Override
    public int toProtocolValueLoop(LoopContext ctx) {
        // 1..4 -> 0..3, two bits
        return (ctx.stateSchematic.getValue(CandleBlock.CANDLES) - 1) & 0b0011;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int maxCandles = (extraProtocolValue & 0b0011) + 1;
        // Vanilla already produced the next step; refuse to overshoot the target.
        if (fromState.getValue(CandleBlock.CANDLES) > maxCandles) {
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
