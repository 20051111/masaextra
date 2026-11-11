package com.example.masaextra.mixin;

import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Verbatim port of chenjunfu2/EarlyCompat VerticallyAttachableBlockItemAccessor.
 * Exposes the wall variant so the client can tell wall vs standing apart with
 * block.equals(wallBlock), exactly like EarlyCompat does.
 */
@Mixin(StandingAndWallBlockItem.class)
public interface AttachableAccessor {
    @Accessor("wallBlock")
    Block masaextra$getWallBlock();
}