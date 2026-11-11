package com.example.masaextra;

import net.minecraft.client.Minecraft;

/**
 * Tweakeroo applies its gamma override only when the feature is toggled.
 * After a world join the vanilla options may overwrite gamma again, so
 * re-apply the configured value once, shortly after the first join.
 */
public final class GammaRefresher {
    private static boolean refreshed;
    private static int ticksUntilRefresh;

    private GammaRefresher() {
    }

    public static void onJoin() {
        if (refreshed
                || !ExactPlaceConfig.ENABLED.getBooleanValue()
                || !ExactPlaceConfig.REFRESH_GAMMA_ON_JOIN.getBooleanValue()) {
            return;
        }
        ticksUntilRefresh = 20;
    }

    public static void tick(Minecraft mc) {
        if (refreshed || ticksUntilRefresh <= 0) {
            return;
        }
        if (--ticksUntilRefresh == 0) {
            refreshed = true;
            apply(mc);
        }
    }

    private static void apply(Minecraft mc) {
        try {
            Class<?> toggleClass = Class.forName("fi.dy.masa.tweakeroo.config.FeatureToggle");
            Object toggle = toggleClass.getField("TWEAK_GAMMA_OVERRIDE").get(null);
            boolean enabled = (boolean) toggleClass.getMethod("getBooleanValue").invoke(toggle);
            if (!enabled) {
                return;
            }

            Class<?> genericClass = Class.forName("fi.dy.masa.tweakeroo.config.Configs$Generic");
            Object valueConfig = genericClass.getField("GAMMA_OVERRIDE_VALUE").get(null);
            double value = (double) valueConfig.getClass().getMethod("getDoubleValue").invoke(valueConfig);

            Object gammaOption = mc.options.gamma();
            try {
                Class<?> mixin = Class.forName("fi.dy.masa.tweakeroo.mixin.IMixinSimpleOption");
                mixin.getMethod("tweakeroo_setValueWithoutCheck", Object.class)
                        .invoke(gammaOption, value);
            } catch (Throwable ignored) {
                mc.options.gamma().set(value);
            }
        } catch (Throwable ignored) {
            // Tweakeroo not installed, or its API changed; nothing to refresh.
        }
    }
}
