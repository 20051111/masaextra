package com.example.masaextra.compat;

import com.example.masaextra.ExactPlaceConfig;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Keeps Litematica's easy place enabled only while the crosshair is on a
 * schematic block, and off the rest of the time.
 *
 * <p>Uses the same test Litematica applies in {@code doEasyPlaceAction}: a ray
 * trace against the schematic world, and the hit must be reported as
 * {@code SCHEMATIC_BLOCK}. Reusing that instead of inventing a distance check
 * means "aiming at the schematic" has exactly the same meaning here as it does
 * for the placement itself, and the reach is the same value the placement uses.
 *
 * <p>The hotkey is handled entirely by malilib: {@code ConfigBooleanHotkeyed}
 * carries its own {@code KeyCallbackToggleBooleanConfigWithMessage}, and
 * {@code MasaExtraKeybindProvider} makes sure the keybind receives key events.
 * This class only reads the resulting boolean, so nothing toggles twice.
 *
 * <p>The user's own setting is captured the first time the automation engages
 * and written back when it is switched off or the world is left, so enabling
 * this option never permanently changes their config.
 */
public final class AutoEasyPlaceCompat {
    /** The user's easy place value before the automation took over, null when idle. */
    private static Boolean baseline;

    private AutoEasyPlaceCompat() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoEasyPlaceCompat::onEndClientTick);
    }

    private static void onEndClientTick(Minecraft mc) {
        if (!ExactPlaceConfig.ENABLED.getBooleanValue()
                || !ExactPlaceConfig.AUTO_TOGGLE_EASY_PLACE.getBooleanValue()
                || mc.player == null || mc.level == null) {
            restore();
            return;
        }

        if (baseline == null) {
            baseline = Configs.Generic.EASY_PLACE_MODE.getBooleanValue();
        }

        boolean aiming = isAimingSchematic(mc);
        if (aiming != Configs.Generic.EASY_PLACE_MODE.getBooleanValue()) {
            Configs.Generic.EASY_PLACE_MODE.setBooleanValue(aiming);
        }
    }

    /** Same test Litematica itself runs: the trace must land on a schematic block. */
    private static boolean isAimingSchematic(Minecraft mc) {
        double range = ExactPlaceConfig.AUTO_TOGGLE_EASY_PLACE_RANGE.getDoubleValue();
        RayTraceWrapper trace = RayTraceUtils.getGenericTrace(mc.level, mc.player, range);
        return trace != null && trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK;
    }

    /** Puts easy place back to whatever the user had configured. */
    private static void restore() {
        if (baseline != null) {
            Configs.Generic.EASY_PLACE_MODE.setBooleanValue(baseline);
            baseline = null;
        }
    }
}
