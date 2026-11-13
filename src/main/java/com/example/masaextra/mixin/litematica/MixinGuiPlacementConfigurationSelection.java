package com.example.masaextra.mixin.litematica;

import com.example.masaextra.ExactPlaceConfig;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Restores the sub-region highlight in the placement configuration screen.
 *
 * <p>Unlike the placement list, this screen's selection <em>is</em> written to
 * disk: {@code onSelectionChange} stores the name via
 * {@code setSelectedSubRegionName} and {@code SchematicPlacement.toJson} saves
 * it. Only the reload side is missing - {@code initGui} builds a fresh widget
 * with no selected entry, so the name survives but nothing is highlighted when
 * the screen opens, which reads as the selection not being saved.
 *
 * <p>The entry is matched by name against {@code getAllSubRegionsPlacements()},
 * which is exactly what {@code WidgetListPlacementSubRegions.getAllEntries()}
 * returns, so the index lines up with the list the widget builds.
 *
 * <p>Client only: Litematica is a client mod.
 */
@Mixin(fi.dy.masa.litematica.gui.GuiPlacementConfiguration.class)
public class MixinGuiPlacementConfigurationSelection {
    @Shadow
    @Final
    public SchematicPlacement placement;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void masaextra$restoreSubRegionHighlight(CallbackInfo ci) {
        if (!ExactPlaceConfig.RESTORE_PLACEMENT_SELECTION.getBooleanValue()) {
            return;
        }

        String name = placement.getSelectedSubRegionName();
        if (name == null || name.isEmpty()) {
            return;
        }

        SubRegionPlacement target = null;
        int index = 0;
        int i = 0;
        for (SubRegionPlacement candidate : placement.getAllSubRegionsPlacements()) {
            if (candidate != null && name.equals(candidate.getName())) {
                target = candidate;
                index = i;
                break;
            }
            i++;
        }
        if (target == null) {
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
                ((WidgetListBase<SubRegionPlacement, ?>) list).setLastSelectedEntry(target, index);
            }
        } catch (Throwable ignored) {
            // Renamed field in another malilib build; only the highlight is lost.
        }
    }
}
