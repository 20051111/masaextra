package com.example.masaextra.compat;

import com.example.masaextra.ExactPlaceConfig;
import com.example.masaextra.config.SnapAlignTrigger;
import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/**
 * Snaps the player to the centre of the block they stand on, either while
 * Tweakeroo's snap aim is active or while a dedicated hotkey is held.
 *
 * <p>Snap aim only rotates the view; the ray origin stays wherever the player
 * happens to stand inside the block. Aligning the position as well makes
 * placement repeatable - the same aim then produces the same hit point no matter
 * where inside the block the player stopped.
 *
 * <p>The write happens <b>only on the rising edge</b> of the trigger: the single
 * tick where snap aim goes from off to on (or the hotkey from released to held).
 * Every other tick returns immediately without touching the position.
 *
 * <p>Comparing the position against the block centre each tick instead looks
 * equivalent but is not: the position is constantly nudged by the server's move
 * correction and by collision resolution, so the tick after a snap writes the old
 * value back and the next tick yanks the position to the centre again. The
 * visible result is the coordinate jumping on every view turn. A rising-edge
 * trigger has no such loop, and turning the view costs nothing.
 *
 * <p>Loaded only when Tweakeroo is present: {@link #register()} is called behind
 * an {@code isModLoaded("tweakeroo")} check, so this class and its Tweakeroo
 * references are never resolved on a client without the mod.
 */
public final class TweakerooSnapAimCompat {
    /** Below this distance from the centre the position already counts as snapped. */
    private static final double SNAPPED_EPSILON = 1.0E-5;

    /** Whether the trigger was already active on the previous tick. */
    private static boolean wasActive;

    private TweakerooSnapAimCompat() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TweakerooSnapAimCompat::onEndClientTick);
    }

    private static void onEndClientTick(Minecraft mc) {
        if (!ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_ENABLED.getBooleanValue()) {
            wasActive = false;
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            wasActive = false;
            return;
        }
        if (!isTriggerActive(mc)) {
            wasActive = false;
            return;
        }

        boolean justActivated = !wasActive;
        wasActive = true;
        if (!justActivated) {
            // Steady state: nothing to do, no matter how the view is turned.
            return;
        }

        double centerX = Math.floor(player.getX()) + 0.5D;
        double centerZ = Math.floor(player.getZ()) + 0.5D;
        if (Math.abs(player.getX() - centerX) < SNAPPED_EPSILON
                && Math.abs(player.getZ() - centerZ) < SNAPPED_EPSILON) {
            return;
        }

        player.setPos(centerX, player.getY(), centerZ);
        // Camera interpolation reads the previous-tick position; leaving it stale
        // makes the view visibly jump for one frame.
        player.xo = centerX;
        player.zo = centerZ;
    }

    /** Whether the configured trigger is currently asking for a snap. */
    private static boolean isTriggerActive(Minecraft mc) {
        SnapAlignTrigger trigger =
                (SnapAlignTrigger) ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_TRIGGER.getOptionListValue();
        if (trigger == SnapAlignTrigger.SNAP_AIM) {
            return FeatureToggle.TWEAK_SNAP_AIM.getBooleanValue();
        }
        return isHotkeyHeld(mc);
    }

    /**
     * Reads the bound keys straight from the config keybind instead of going
     * through malilib's hotkey dispatch, which would require registering a
     * callback for something that is only a held-key state.
     */
    private static boolean isHotkeyHeld(Minecraft mc) {
        List<Integer> keys = ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_HOTKEY.getKeybind().getKeys();
        if (keys.isEmpty()) {
            return false;
        }
        long window = mc.getWindow().getWindow();
        for (int key : keys) {
            if (!InputConstants.isKeyDown(window, key)) {
                return false;
            }
        }
        return true;
    }
}
