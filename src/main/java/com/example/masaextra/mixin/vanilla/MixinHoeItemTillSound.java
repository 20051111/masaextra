package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.ExactPlaceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Plays the hoe tilling sound once per block per tick.
 *
 * <p>{@code HoeItem#useOn} plays {@code HOE_TILL} before the {@code isClientSide}
 * check, and only the server actually converts the block. A client holding the use
 * key with Tweakeroo's fast right click calls {@code doItemUse} ten times per tick
 * (or however {@code fastRightClickCount} is set), and within that tick the client
 * block is still dirt, so every call passes the {@code TILLABLES} check and fires
 * the sound again. One block ends up producing ten overlapping sounds.
 *
 * <p>Deduplicating on block position plus game time collapses those repeats while
 * leaving rapid tilling of <em>different</em> blocks untouched - the sound is only
 * suppressed when the exact same block is retried within the same tick.
 *
 * <p>Registered as a client-only mixin: the server has no reason to skip sounds,
 * and keeping it out of the shared list avoids pulling the malilib-backed config
 * onto a dedicated server.
 */
@Mixin(HoeItem.class)
public class MixinHoeItemTillSound {
    private static BlockPos masaextra$lastTillPos;
    private static long masaextra$lastTillTick = Long.MIN_VALUE;

    @Redirect(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playSound("
                            + "Lnet/minecraft/world/entity/player/Player;"
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/sounds/SoundEvent;"
                            + "Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void masaextra$dedupeTillSound(Level level, Player player, BlockPos pos, SoundEvent sound,
                                           SoundSource source, float volume, float pitch) {
        if (!ExactPlaceConfig.DEDUPE_TILL_SOUND.getBooleanValue() || !level.isClientSide) {
            level.playSound(player, pos, sound, source, volume, pitch);
            return;
        }

        long tick = level.getGameTime();
        if (pos.equals(masaextra$lastTillPos) && tick == masaextra$lastTillTick) {
            // Same block, same tick: this is a repeat from fast right click.
            return;
        }

        masaextra$lastTillPos = pos.immutable();
        masaextra$lastTillTick = tick;
        level.playSound(player, pos, sound, source, volume, pitch);
    }
}
