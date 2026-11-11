package com.example.masaextra.protocol;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Port of chenjunfu2/EarlyCompat {@code ItemStackProtocolDataAdapter}.
 *
 * <p>For blocks whose item stack carries data besides the block state
 * (e.g. decorated pot / crafter in EarlyCompat). The value travels in the
 * hit-vector Z channel instead of X.
 */
public interface ItemStackProtocolDataAdapter {
    /** Encode the stack data. Must not mutate {@code fromStack}. */
    int toProtocolValueAddition(ItemStack fromStack);

    /** Decode the stack data. Must return a copy, never mutate {@code fromStack}. */
    @NotNull
    ItemStack fromProtocolValueAddition(int extraProtocolValue, ItemStack fromStack);
}
