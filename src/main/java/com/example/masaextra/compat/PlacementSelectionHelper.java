package com.example.masaextra.compat;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;

/**
 * Applies a saved placement selection by name.
 *
 * <p>Lives here rather than inside the mixin for two reasons: a {@code @Mixin}
 * class may not contain non-private static methods (Mixin treats them as members
 * to merge into the target and rejects them), and the compat code that detects
 * "reload finished" needs to call it too.
 *
 * <p>This class references Litematica types directly. That is safe because every
 * caller is either a Litematica mixin or already guarded by a mod-present check;
 * it is never touched when Litematica is absent.
 */
public final class PlacementSelectionHelper {
    private PlacementSelectionHelper() {
    }

    /**
     * Selects the placement with this name, if the manager already knows it.
     *
     * @return whether the match succeeded
     */
    public static boolean restoreByName(SchematicPlacementManager manager, String name) {
        if (manager == null || name == null || name.isEmpty()) {
            return false;
        }
        for (SchematicPlacement placement : manager.getAllSchematicsPlacements()) {
            if (placement != null && name.equals(placement.getName())) {
                // Overrides whatever the index-based restore picked.
                manager.setSelectedSchematicPlacement(placement);
                PlacementSelectionState.clear();
                return true;
            }
        }
        return false;
    }
}
