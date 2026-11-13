package com.example.masaextra.mixin;

import com.example.masaextra.ExactPlaceConfig;
import fi.dy.masa.litematica.util.WorldUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WorldUtils.class)
public class MixinEasyPlaceCooldown {
    @ModifyConstant(
            method = "cacheEasyPlacePosition",
            constant = @Constant(longValue = 2000000000L)
    )
    private static long masaextra$cooldown(long original) {
        return (long) ExactPlaceConfig.EASY_PLACE_COOLDOWN_MS.getIntegerValue() * 1_000_000L;
    }
}
