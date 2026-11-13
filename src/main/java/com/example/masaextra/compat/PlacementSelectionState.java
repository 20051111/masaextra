package com.example.masaextra.compat;

/**
 * Shared state for the placement-selection restore.
 *
 * <p>Separate from the mixin on purpose: a {@code @Mixin} class's static fields
 * are not reliable from outside the mixin, and the compat code that detects
 * "reload finished" needs to read this. Keeping it in a plain class avoids both
 * problems and keeps this file free of Litematica types, so it loads fine when
 * Litematica is absent.
 */
public final class PlacementSelectionState {
    private static String pendingName;

    private PlacementSelectionState() {
    }

    /** Remembers a name that still needs to be selected once the list is complete. */
    public static void remember(String name) {
        pendingName = name;
    }

    /** The name still waiting to be matched, or null. */
    public static String pending() {
        return pendingName;
    }

    public static void clear() {
        pendingName = null;
    }
}
