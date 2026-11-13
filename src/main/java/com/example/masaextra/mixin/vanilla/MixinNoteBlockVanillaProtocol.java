package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Port of chenjunfu2/EarlyCompat $Doc.
 */
@Mixin(net.minecraft.world.level.block.NoteBlock.class)
public abstract class MixinNoteBlockVanillaProtocol implements BlockProtocolStateAdapter {
    @Override
    public int toProtocolValue(int protocolValue, BlockState fromState) {
        int note = fromState.getValue(net.minecraft.world.level.block.NoteBlock.NOTE);
        return note & 0b0001_1111;
    }

    @Override
    public @Nullable BlockState fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int note = (extraProtocolValue & 0b0001_1111) % 25;
        return fromState.setValue(net.minecraft.world.level.block.NoteBlock.NOTE, note);
    }

    @Override
    public @NotNull ProtocolType getProtocolType() {
        return ProtocolType.REPLACE;
    }
}