package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import com.example.masaextra.protocol.MultiStageBlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code SnowBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Snow layers 1-8 are built one layer per click.
 */
@Mixin(SnowLayerBlock.class)
public abstract class MixinSnowLayerBlockVanillaProtocol
        implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    @Override
    public void setLoopCount(LoopContext ctx) {
        BlockState client = ctx.stateClient;
        boolean isSnow = client != null && client.is(((SnowLayerBlock) (Object) this));
        int current = isSnow ? client.getValue(SnowLayerBlock.LAYERS) : 0;
        int target = ctx.stateSchematic.getValue(SnowLayerBlock.LAYERS);
        ctx.loopCount = target > current ? target - current : 0;
    }

    @Override
    public int toProtocolValueLoop(LoopContext ctx) {
        // 1..8 -> 0..7, three bits
        return (ctx.stateSchematic.getValue(SnowLayerBlock.LAYERS) - 1) & 0b0111;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int maxLayers = (extraProtocolValue & 0b0111) + 1;
        if (fromState.getValue(SnowLayerBlock.LAYERS) > maxLayers) {
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
