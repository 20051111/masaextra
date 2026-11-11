package com.example.masaextra.protocol;

/**
 * Port of chenjunfu2/EarlyCompat {@code CarpetExtraSettingsAccessor}.
 *
 * <p>Gates whether wall-attached placement may reinterpret the hit vector the way
 * the protocol needs. EarlyCompat ships two versions of this:
 *
 * <ul>
 *   <li>the default, which returns {@code true}, and</li>
 *   <li>a mixin that overwrites it to read carpet-extra's
 *       {@code accurateBlockPlacement} rule, applied only when carpet-extra is
 *       installed.</li>
 * </ul>
 *
 * <p>In other words: <b>the absence of carpet-extra must enable the feature, not
 * disable it.</b> Returning false when the class is missing silently turned every
 * wall torch into a floor torch on servers without carpet-extra.
 *
 * <p>carpet-extra is read reflectively so masaextra never hard-depends on it.
 */
public final class CarpetExtraCompat {
    private static final String SETTINGS_CLASS = "carpetextra.CarpetExtraSettings";
    private static final String RULE_FIELD = "accurateBlockPlacement";

    private static Boolean present;

    private CarpetExtraCompat() {
    }

    public static boolean isClassPresent() {
        if (present == null) {
            try {
                Class.forName(SETTINGS_CLASS);
                present = Boolean.TRUE;
            } catch (Throwable t) {
                present = Boolean.FALSE;
            }
        }
        return present;
    }

    /**
     * True when the exact wall/floor protocol may be decoded.
     *
     * <p>Default {@code true} when carpet-extra is absent, matching EarlyCompat's
     * default accessor. When it is installed, its own rule decides.
     */
    public static boolean getAccurateBlockPlacement() {
        if (!isClassPresent()) {
            return true;
        }
        try {
            Class<?> settings = Class.forName(SETTINGS_CLASS);
            return settings.getField(RULE_FIELD).getBoolean(null);
        } catch (Throwable t) {
            // Rule renamed or removed in a newer carpet-extra: keep the default
            // rather than disabling the whole feature.
            return true;
        }
    }

    /** Kept for callers written against the old name. */
    public static boolean isAccurateBlockPlacementEnabled() {
        return getAccurateBlockPlacement();
    }
}
