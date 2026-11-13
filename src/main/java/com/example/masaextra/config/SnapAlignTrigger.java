package com.example.masaextra.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * When the block-centre position snap should apply.
 *
 * <p>Mirrors the style malilib expects for a {@code ConfigOptionList}: the entry
 * itself knows its config string and its translation key.
 */
public enum SnapAlignTrigger implements IConfigOptionListEntry {
    /** Follow Tweakeroo's snap aim toggle. */
    SNAP_AIM("snap_aim", "masaextra.label.snap_align_trigger.snap_aim"),
    /** Only while the dedicated hotkey is held down. */
    HOTKEY("hotkey", "masaextra.label.snap_align_trigger.hotkey");

    private final String configString;
    private final String translationKey;

    SnapAlignTrigger(String configString, String translationKey) {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue() {
        return this.configString;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        int size = values().length;
        int id = this.ordinal() + (forward ? 1 : size - 1);
        return values()[id % size];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        return fromStringStatic(value);
    }

    public static SnapAlignTrigger fromStringStatic(String value) {
        for (SnapAlignTrigger trigger : values()) {
            if (trigger.configString.equalsIgnoreCase(value)) {
                return trigger;
            }
        }
        return SNAP_AIM;
    }
}
