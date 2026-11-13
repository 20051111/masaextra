package com.example.masaextra.compat;

import com.example.masaextra.ExactPlaceConfig;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Integrations with Tweakermore's "auto collect material list item".
 *
 * <p>Both hooks key off the <em>switch state</em> of that feature, not its hotkey
 * binding. The option is a {@code ConfigBooleanHotkeyed}: pressing the key toggles
 * the boolean, and the feature runs while it is true. Watching the switch means
 * the behaviour follows the feature itself - however it was turned on (hotkey,
 * config GUI, or a config reload) - and it fires exactly once per activation
 * rather than on every key press or every tick.
 *
 * <p>Two things happen around an activation:
 *
 * <ol>
 *   <li>Litematica's easy place is suspended while it runs. It opens containers
 *       and pulls items out, and with easy place still armed, holding the use key
 *       can also try to place blocks into the very container being emptied. The
 *       previous easy place value is captured when suppression starts and written
 *       back when it ends, so "was on" comes back on and "was off" stays off.</li>
 *   <li>Optionally, the material list is recomputed on the off-to-on transition.
 *       The collect routine reads that list, and it is only rebuilt when
 *       Litematica decides to - a schematic edited since it was last built would
 *       otherwise be collected against stale counts.</li>
 * </ol>
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
    /** Previous value of the auto-collect switch, for off-to-on detection. */
    private static boolean wasCollecting;

    private TweakermoreAutoCollectCompat() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TweakermoreAutoCollectCompat::onEndClientTick);
    }

    private static void onEndClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            // Leaving the world must not leave easy place switched off, and the
            // edge detector must not carry a stale "true" into the next world.
            restore();
            wasCollecting = false;
            return;
        }

        boolean collecting = TweakerMoreConfigs.AUTO_COLLECT_MATERIAL_LIST_ITEM.getBooleanValue();

        // Off-to-on: the feature just started. Rebuild the list once, before the
        // collect routine has moved anything.
        if (collecting && !wasCollecting
                && ExactPlaceConfig.REFRESH_MATERIAL_LIST_ON_COLLECT.getBooleanValue()) {
            MaterialListBase materialList = DataManager.getMaterialList();
            if (materialList != null) {
                materialList.reCreateMaterialList();
            }
        }
        wasCollecting = collecting;

        if (!ExactPlaceConfig.SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT.getBooleanValue()) {
            restore();
            return;
        }

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
