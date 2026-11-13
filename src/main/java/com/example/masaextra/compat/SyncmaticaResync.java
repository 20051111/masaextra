package com.example.masaextra.compat;

import com.example.masaextra.ExactPlaceConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Re-registers Syncmatica placements that failed to render during the join
 * handshake.
 *
 * <p>Syncmatica's {@code LitematicManager} keeps a {@code preLoadList} of
 * placements that arrive before its context is started, then {@code commitLoad()}
 * drains it and sets the field to {@code null}. Any placement that reaches
 * {@code preLoad} after that point hits a {@code if (preLoadList != null)} guard
 * and is silently dropped - no error, no log, the schematic simply does not
 * appear until the user loads it again by hand.
 *
 * <p>{@code VersionHandshakeClient} calls {@code commitLoad()} <em>before</em>
 * {@code context.startup()}, so the window between the two calls is where this
 * happens. Which placements land in it depends on network and world-load timing,
 * which is why the symptom is intermittent.
 *
 * <p>Only placements in the player's current dimension are touched.
 * {@code renderSyncmatic} answers a placement from another dimension with a chat
 * message ("the player must be in the same dimension as the schematic"), so
 * walking the whole list would spam that line every tick. The dimension is
 * therefore compared up front and mismatches are skipped silently - they are not
 * a failure, just something to render after travelling.
 *
 * <p>All access is reflective. Syncmatica is not a compile dependency, and the
 * whole class is inert when the mod is absent.
 */
public final class SyncmaticaResync {
    private static final String LITEMATIC_MANAGER = "ch.endte.syncmatica.litematica.LitematicManager";
    private static final String SERVER_PLACEMENT = "ch.endte.syncmatica.ServerPlacement";
    private static final String CONTEXT = "ch.endte.syncmatica.Context";
    private static final String SYNCMATIC_MANAGER = "ch.endte.syncmatica.SyncmaticManager";

    /** Roughly 60 seconds of ticks; after that give up until the next join or dimension change. */
    private static final int MAX_ATTEMPTS = 1200;

    /**
     * Placements rendered per tick. Each one loads a schematic from disk and
     * registers it with Litematica, which is heavy enough that a whole batch in
     * one tick stalls the frame and can starve other resource loads - the
     * observed symptom was Iris failing to upload its dynamic light map right
     * afterwards, which then crashed an unrelated HUD render.
     */
    private static final int MAX_PER_TICK = 1;

    private static boolean available;
    private static boolean disabled;
    private static boolean pending;
    private static int attempts;
    private static String lastDimension;

    private static Class<?> lmClass;
    private static Class<?> spClass;
    private static Class<?> ctxClass;
    private static Method getInstance;
    private static Method getActiveContext;
    private static Method isRendered;
    private static Method renderSyncmatic;
    private static Method spGetDimension;
    private static Method ctxIsStarted;
    private static Method ctxGetManager;
    private static Method mgrGetAll;

    private SyncmaticaResync() {
    }

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded("syncmatica")) {
            return;
        }
        try {
            lmClass = Class.forName(LITEMATIC_MANAGER);
            spClass = Class.forName(SERVER_PLACEMENT);
            ctxClass = Class.forName(CONTEXT);
            Class<?> managerClass = Class.forName(SYNCMATIC_MANAGER);

            getInstance = lmClass.getMethod("getInstance");
            getActiveContext = lmClass.getMethod("getActiveContext");
            isRendered = lmClass.getMethod("isRendered", spClass);
            renderSyncmatic = lmClass.getMethod("renderSyncmatic", spClass);
            spGetDimension = spClass.getMethod("getDimension");
            ctxIsStarted = ctxClass.getMethod("isStarted");
            ctxGetManager = ctxClass.getMethod("getSyncmaticManager");
            mgrGetAll = managerClass.getMethod("getAll");
            available = true;
        } catch (Throwable t) {
            // Different Syncmatica build: the option simply does nothing.
            disabled = true;
        }
    }

    public static boolean isAvailable() {
        return available && !disabled;
    }

    /** Called on join; the flush waits until the world and the context are ready. */
    public static void onJoin() {
        pending = isAvailable() && ExactPlaceConfig.SYNCMATICA_RESYNC.getBooleanValue();
        attempts = 0;
        lastDimension = null;
    }

    public static void onDisconnect() {
        pending = false;
        attempts = 0;
        lastDimension = null;
    }

    public static void onTick() {
        if (!isAvailable() || !ExactPlaceConfig.SYNCMATICA_RESYNC.getBooleanValue()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            // renderSyncmatic reads the dimension from the world; wait for it.
            return;
        }

        // A dimension change brings a fresh set of placements into scope, so the
        // pass has to run again - and the attempt budget resets with it.
        String dimension = mc.level.dimension().location().toString();
        if (!dimension.equals(lastDimension)) {
            lastDimension = dimension;
            pending = true;
            attempts = 0;
        }
        if (!pending) {
            return;
        }
        if (++attempts > MAX_ATTEMPTS) {
            pending = false;
            return;
        }

        try {
            Object manager = getInstance.invoke(null);
            Object context = getActiveContext.invoke(manager);
            if (context == null || !(Boolean) ctxIsStarted.invoke(context)) {
                // Handshake not finished; preLoad would still file these away.
                return;
            }

            Object syncManager = ctxGetManager.invoke(context);
            Collection<?> all = (Collection<?>) mgrGetAll.invoke(syncManager);
            if (all == null || all.isEmpty()) {
                // Nothing shared on this server: the local list is already complete.
                pending = false;
                restoreSelection();
                return;
            }

            boolean anyMissingHere = false;
            int processedThisTick = 0;
            for (Object placement : all) {
                if (placement == null || !spClass.isInstance(placement)) {
                    continue;
                }
                if ((Boolean) isRendered.invoke(manager, placement)) {
                    continue;
                }

                // Placements in another dimension are not missing, just out of
                // reach. Calling renderSyncmatic on them would post a chat
                // message every single tick.
                String placementDimension = (String) spGetDimension.invoke(placement);
                if (placementDimension == null || !placementDimension.equals(dimension)) {
                    continue;
                }

                anyMissingHere = true;
                if (processedThisTick >= MAX_PER_TICK) {
                    // Still missing, but leave it for the next tick. Rendering a
                    // placement loads its schematic from disk and registers it,
                    // so doing a whole batch in one tick stalls the frame badly
                    // enough to starve other resource loads.
                    continue;
                }
                processedThisTick++;
                try {
                    renderSyncmatic.invoke(manager, placement);
                } catch (Throwable ignored) {
                    // A single bad placement must not abort the rest.
                }
            }

            if (!anyMissingHere) {
                // Every placement in this dimension is registered: this is the
                // "reload finished" point the selection restore waits for.
                pending = false;
                restoreSelection();
            }
        } catch (Throwable ignored) {
            // Syncmatica mid-reload; try again next tick.
        }
    }

    /**
     * Applies a selection that was saved by name but could not be matched while
     * the shared placements were still missing.
     *
     * <p>Reflective for the same reason the rest of this class is: Litematica is
     * not a compile dependency here. The mixin that owns the actual restore is
     * not called directly - a mixin class is not a reliable collaborator from
     * outside.
     */
    private static void restoreSelection() {
        String name = PlacementSelectionState.pending();
        if (name == null || name.isEmpty()) {
            return;
        }
        try {
            Class<?> dataManagerClass = Class.forName("fi.dy.masa.litematica.data.DataManager");
            Object manager = dataManagerClass.getMethod("getSchematicPlacementManager").invoke(null);
            if (manager == null) {
                return;
            }
            Class<?> placementClass =
                    Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacement");
            Method getAll = manager.getClass().getMethod("getAllSchematicsPlacements");
            Method getName = placementClass.getMethod("getName");
            Method setSelected = manager.getClass()
                    .getMethod("setSelectedSchematicPlacement", placementClass);

            for (Object placement : (Iterable<?>) getAll.invoke(manager)) {
                if (placement != null && name.equals(getName.invoke(placement))) {
                    setSelected.invoke(manager, placement);
                    PlacementSelectionState.clear();
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Litematica absent or an unexpected build; the GUI open path retries.
        }
    }
}
