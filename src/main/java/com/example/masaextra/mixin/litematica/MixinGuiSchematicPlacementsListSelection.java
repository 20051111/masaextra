package com.example.masaextra.mixin.litematica;

import com.example.masaextra.ExactPlaceConfig;
import com.example.masaextra.compat.PlacementSelectionHelper;
import com.example.masaextra.compat.PlacementSelectionState;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Restores the placement selection when the list is opened, and highlights it.
 *
 * <p>Two independent selections exist: {@code SchematicPlacementManager} tracks
 * the current placement (used by operations), and {@code WidgetListBase} tracks
 * what is drawn as selected. Opening the GUI builds a fresh widget with nothing
 * selected, so even a correct manager selection shows up as "nothing is
 * selected".
 *
 * <p>This is also the point where a selection that could not be restored earlier
 * gets a second chance: placements shared through Syncmatica are only registered
 * during the handshake, after {@code loadFromJson} has already run, so the name
 * had nothing to match at load time. By the time the user opens this screen they
 * are present.
 *
 * <p>Client only: Litematica is a client mod.
 */
@Mixin(fi.dy.masa.litematica.gui.GuiSchematicPlacementsList.class)
public class MixinGuiSchematicPlacementsListSelection {
    @Shadow
    @Final
    public SchematicPlacementManager manager;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void masaextra$restoreHighlight(CallbackInfo ci) {
        if (!ExactPlaceConfig.RESTORE_PLACEMENT_SELECTION.getBooleanValue()) {
            return;
        }

        // Retry the pending name first: shared placements have been rendered by now.
        String pending = PlacementSelectionState.pending();
        if (pending != null && manager.getSelectedSchematicPlacement() == null) {
            PlacementSelectionHelper.restoreByName(manager, pending);
        }

        SchematicPlacement selected = manager.getSelectedSchematicPlacement();
        if (selected == null) {
            return;
        }

        List<SchematicPlacement> all = manager.getAllSchematicsPlacements();
        int index = all.indexOf(selected);
        if (index < 0) {
            return;
        }

        try {
            Field widgetField = GuiListBase.class.getDeclaredField("widget");
            widgetField.setAccessible(true);
            Object widget = widgetField.get(this);
            if (widget instanceof WidgetListBase<?, ?> list) {
                // setLastSelectedEntry toggles when multi-select is on, so clear first.
                list.getSelectedEntries().clear();
                //noinspection unchecked
                ((WidgetListBase<SchematicPlacement, ?>) list).setLastSelectedEntry(selected, index);
            }
        } catch (Throwable ignored) {
            // Another malilib build renamed the field; the manager state still works,
            // only the highlight is missing.
        }
    }
}
