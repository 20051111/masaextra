package com.example.masaextra.compat;

import com.example.masaextra.ExactPlaceConfig;
import fi.dy.masa.litematica.config.Configs;
import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Turns Litematica's easy place off while Tweakermore's "auto collect material
 * list item" is running, and puts it back afterwards.
 *
 * <p>That feature opens containers and pulls out the items the schematic needs.
 * With easy place still armed, holding the use key can also try to place blocks
 * into the very container being emptied, so the two features fight each other.
 * Suspending easy place for the duration removes the conflict without touching
 * the user's own setting: the previous value is captured when the suppression
 * starts and written back when it ends, so "was on" comes back on and "was off"
 * stays off.
 *
 * <p>Loaded only when Tweakermore is present - {@link #register()} is called
 * behind an {@code isModLoaded("tweakermore")} check, so this class and its
 * Tweakermore references are never resolved on a client without the mod.
 */
public final class TweakermoreAutoCollectCompat {
    /** True while easy place is currently held off by this integration. */
    private static boolean suppressed;
    /** Easy place value captured when the suppression started. */
    private static boolean savedEasyPlaceMode;

    private TweakermoreAutoCollectCompat() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TweakermoreAutoCollectCompat::onEndClientTick);
    }

    private static void onEndClientTick(Minecraft mc) {
        if (!ExactPlaceConfig.SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT.getBooleanValue()) {
            restore();
            return;
        }
        if (mc.player == null || mc.level == null) {
            // Leaving the world must not leave easy place switched off.
            restore();
            return;
        }

        boolean collecting = TweakerMoreConfigs.AUTO_COLLECT_MATERIAL_LIST_ITEM.getBooleanValue();

        if (collecting && !suppressed) {
            // Capture first, then clear, so "was already off" restores to off.
            savedEasyPlaceMode = Configs.Generic.EASY_PLACE_MODE.getBooleanValue();
            Configs.Generic.EASY_PLACE_MODE.setBooleanValue(false);
            suppressed = true;
        } else if (!collecting && suppressed) {
            Configs.Generic.EASY_PLACE_MODE.setBooleanValue(savedEasyPlaceMode);
            suppressed = false;
        }
    }

    /** Puts easy place back to whatever it was before the suppression. */
    private static void restore() {
        if (suppressed) {
            Configs.Generic.EASY_PLACE_MODE.setBooleanValue(savedEasyPlaceMode);
            suppressed = false;
        }
    }
}
