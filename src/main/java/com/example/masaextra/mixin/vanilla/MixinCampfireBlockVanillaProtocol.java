package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat {@code CampfireBlockMixin_VanillaProtocolCompat}.
 *
 * <p>Campfires are always placed lit, but a schematic can contain an unlit one
 * (or one lit by hand), so the lit flag rides on bit 4.
 */
@Mixin(CampfireBlock.class)
public abstract class MixinCampfireBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isLit = fromState.getValue(CampfireBlock.LIT);
        return protocolValue | (isLit ? 0b0001_0000 : 0b0000_0000);
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isLit = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
        return fromState.setValue(CampfireBlock.LIT, isLit);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.ADDED;
    }
}
