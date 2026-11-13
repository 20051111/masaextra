package com.example.masaextra.compat;

import com.example.masaextra.ExactPlaceConfig;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers this mod's keybinds with malilib, exactly the way Tweakeroo and
 * Tweakermore do.
 *
 * <p>Nothing else is needed: {@code ConfigBooleanHotkeyed} installs its own
 * {@code KeyCallbackToggleBooleanConfigWithMessage} in its constructor, and that
 * is what flips the boolean on a key press. Setting a callback here would only
 * replace malilib's implementation, and toggling from a tick loop as well flips
 * the switch twice per press.
 *
 * <p>{@link #addKeysToMap} is the part that makes keys functional - it puts the
 * keybind into malilib's key map so key events reach it. {@link #addHotkeys}
 * only lists it in malilib's keybind category view.
 */
public final class MasaExtraKeybindProvider implements IKeybindProvider {
    private static final String CATEGORY_NAME = "Masa Extra";
    private static final String CATEGORY_KEY = "masaextra.hotkeys.category";

    private static final List<IHotkey> HOTKEYS = new ArrayList<>();

    static {
        HOTKEYS.add(ExactPlaceConfig.AUTO_TOGGLE_EASY_PLACE);
        HOTKEYS.add(ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_HOTKEY);
    }

    private MasaExtraKeybindProvider() {
    }

    public static void register() {
        InputEventHandler.getKeybindManager().registerKeybindProvider(new MasaExtraKeybindProvider());
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : HOTKEYS) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(CATEGORY_NAME, CATEGORY_KEY, HOTKEYS);
    }
}
