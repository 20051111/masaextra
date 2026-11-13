package com.example.masaextra.mixin.litematica;

import com.example.masaextra.ExactPlaceConfig;
import com.example.masaextra.compat.PlacementSelectionHelper;
import com.example.masaextra.compat.PlacementSelectionState;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stores the selected placement by name instead of only by list index.
 *
 * <p>Litematica saves the selection as an <em>array index</em>:
 *
 * <pre>
 *   "selected": 3
 * </pre>
 * and reads it back with {@code schematicPlacements.get(selectedIndex)}. That only
 * survives a reload if the list comes back in the same order.
 *
 * <p>Syncmatica shares make that fail twice over: its placements are added in
 * whatever order the server sends them, and they are not part of the list when
 * {@code loadFromJson} runs - they are rendered in during the handshake,
 * afterwards. So a name match at load time misses them.
 *
 * <p>The name is therefore parked in {@link PlacementSelectionState} and retried
 * from the two points where the list can have become complete:
 * {@code SyncmaticaResync} right after it finishes rendering, and
 * {@code MixinGuiSchematicPlacementsListSelection} when the list is opened.
 *
 * <p>Client only: Litematica is a client mod.
 */
@Mixin(SchematicPlacementManager.class)
public class MixinPlacementSelectionPersist {
    private static final String SELECTED_NAME_KEY = "masaextraSelectedName";

    @Inject(method = "toJson", at = @At("RETURN"))
    private void masaextra$saveSelectedName(CallbackInfoReturnable<JsonObject> cir) {
        if (!ExactPlaceConfig.RESTORE_PLACEMENT_SELECTION.getBooleanValue()) {
            return;
        }
        SchematicPlacementManager self = (SchematicPlacementManager) (Object) this;
        SchematicPlacement selected = self.getSelectedSchematicPlacement();
        JsonObject json = cir.getReturnValue();
        if (selected != null && json != null && selected.getName() != null) {
            json.addProperty(SELECTED_NAME_KEY, selected.getName());
        }
    }

    @Inject(method = "loadFromJson", at = @At("RETURN"))
    private void masaextra$rememberSelectedName(JsonObject json, CallbackInfo ci) {
        if (!ExactPlaceConfig.RESTORE_PLACEMENT_SELECTION.getBooleanValue()) {
            return;
        }
        if (json == null || !json.has(SELECTED_NAME_KEY)) {
            return;
        }
        String name;
        try {
            name = json.get(SELECTED_NAME_KEY).getAsString();
        } catch (Exception ignored) {
            return;
        }
        if (name == null || name.isEmpty()) {
            return;
        }

        // Park it even when the match fails: shared placements join later.
        PlacementSelectionState.remember(name);
        PlacementSelectionHelper.restoreByName((SchematicPlacementManager) (Object) this, name);
    }

}
