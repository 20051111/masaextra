package com.example.masaextra;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import com.example.masaextra.config.SnapAlignTrigger;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ExactPlaceConfig implements IConfigHandler {
    public static final ExactPlaceConfig INSTANCE = new ExactPlaceConfig();

    public static final ConfigBoolean ENABLED =
            new ConfigBoolean("masaextraEnabled", false, "Master switch for Exact Place");
    public static final ConfigBoolean USE_SERVER_EXACT =
            new ConfigBoolean("masaextraUseServerExact", false,
                    "Use the server exact-placement protocol when the server supports it");
    public static final ConfigBoolean DEBUG_MODE =
            new ConfigBoolean("masaextraDebugMode", false, "Print diagnostic messages to the game log");
    public static final ConfigBoolean SUPPRESS_MISSING_SUPPORT =
            new ConfigBoolean("masaextraSuppressMissingSupport", false,
                    "Cancel placement instead of letting easy place put a floor torch when the wall is missing");
    public static final ConfigBoolean SNAP_TO_BLOCK_CENTER_ENABLED =
            new ConfigBoolean("masaextraSnapAlignEnabled", false,
                    "Snap your position to the centre of the block you stand on");
    public static final ConfigOptionList SNAP_TO_BLOCK_CENTER_TRIGGER =
            new ConfigOptionList("masaextraSnapAlignTrigger", SnapAlignTrigger.SNAP_AIM,
                    "When the position snap applies: with Tweakeroo's snap aim, or while the hotkey is held");
    public static final ConfigHotkey SNAP_TO_BLOCK_CENTER_HOTKEY =
            new ConfigHotkey("masaextraSnapAlignHotkey", "",
                    "Hold to snap your position to the centre of the block you stand on");
    public static final ConfigBoolean SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT =
            new ConfigBoolean("masaextraSuppressEasyPlaceOnAutoCollect", false,
                    "Turn easy place off while Tweakermore's auto collect material list item is running, then restore it");
    /** Read-only status line, refreshed when the config GUI opens. */
    public static final ConfigString SERVER_INFO =
            new ConfigString("masaextraServerInfo", "",
                    "Server-side MasaExtra presence and version (refreshed when the config screen opens)");
    public static final ConfigBooleanHotkeyed AUTO_TOGGLE_EASY_PLACE =
            new ConfigBooleanHotkeyed("masaextraAutoToggleEasyPlace", false, "",
                    "Turn easy place on only while aiming at a schematic block, and off otherwise. "
                            + "Hold the hotkey on the same row to engage it temporarily.");
    public static final ConfigDouble AUTO_TOGGLE_EASY_PLACE_RANGE =
            new ConfigDouble("masaextraAutoToggleEasyPlaceRange", 6.0D, 1.0D, 32.0D,
                    "Schematic block search distance for the automatic easy place toggle, in blocks");
    public static final ConfigBoolean DEDUPE_TILL_SOUND =
            new ConfigBoolean("masaextraDedupeTillSound", false,
                    "Play the hoe tilling sound once per block per tick, so fast right click stops stacking it");
    public static final ConfigBoolean BILINGUAL_SEARCH =
            new ConfigBoolean("masaextraBilingualSearch", false,
                    "Allow Chinese, English and pinyin in search boxes at once - search a Chinese client by English "
                            + "or pinyin. Also enables pinyin when that switch is off");
    public static final ConfigBoolean SYNCMATICA_RESYNC =
            new ConfigBoolean("masaextraSyncmaticaResync", false,
                    "Re-register Syncmatica placements that were dropped during the join handshake. "
                            + "Fixes shared schematics occasionally not appearing until reloaded by hand");
    public static final ConfigBoolean RESTORE_PLACEMENT_SELECTION =
            new ConfigBoolean("masaextraRestorePlacementSelection", false,
                    "Remember the selected schematic placement by name. Litematica stores it as a list index, "
                            + "which shifts when Syncmatica adds shared placements in a different order");
    public static final ConfigBoolean REFRESH_MATERIAL_LIST_ON_COLLECT =
            new ConfigBoolean("masaextraRefreshMaterialListOnCollect", false,
                    "Recompute the loaded schematic material list on the rising edge of Tweakermore's "
                            + "auto-collect hotkey, so it collects against fresh numbers");
    public static final ConfigBoolean REFRESH_GAMMA_ON_JOIN =
            new ConfigBoolean("masaextraRefreshGammaOnJoin", false,
                    "Re-apply Tweakeroo's gamma override after the first world join");
    public static final ConfigBoolean CHAT_DEBUG =
            new ConfigBoolean("masaextraChatDebug", false,
                    "Print Exact Place trigger details (block state and block info) to the chat");
    public static final ConfigBoolean OMMC_HIGHLIGHT_STRICT_MATCH =
            new ConfigBoolean("masaextraOmmcHighlightStrictMatch", false,
                    "OMMC highlightEntity: use strict entity id matching instead of substring matching");
    public static final ConfigInteger EASY_PLACE_COOLDOWN_MS =
            new ConfigInteger("masaextraCooldownMs", 250, 0, 5000,
                    "Litematica easy place position cache time in milliseconds (0 disables the cooldown)");

    // --- EarlyCompat "easyPlaceV2ProtocolExtra" equivalents -----------------
    public static final ConfigBoolean EASY_PLACE_V2_PROTOCOL_EXTRA =
            new ConfigBoolean("easyPlaceV2ProtocolExtra", false,
                    "Encode the full block state into the easy place V2 protocol, so stairs, rails, "
                            + "trapdoors, levers and similar blocks are placed in the exact schematic orientation");

    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("masaextra-client.json");

    private ExactPlaceConfig() {
    }

    @Override
    public void load() {
        try {
            if (!Files.exists(FILE)) {
                save();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            read(root, "masaextraEnabled", ENABLED);
            read(root, "masaextraUseServerExact", USE_SERVER_EXACT);
            read(root, "masaextraDebugMode", DEBUG_MODE);
            read(root, "masaextraSuppressMissingSupport", SUPPRESS_MISSING_SUPPORT);
            read(root, "masaextraSnapAlignEnabled", SNAP_TO_BLOCK_CENTER_ENABLED);
            if (root.has("masaextraSnapAlignTrigger")) {
                SNAP_TO_BLOCK_CENTER_TRIGGER.setOptionListValue(
                        SnapAlignTrigger.fromStringStatic(root.get("masaextraSnapAlignTrigger").getAsString()));
            }
            readKeybind(root, "masaextraSnapAlignHotkey", SNAP_TO_BLOCK_CENTER_HOTKEY.getKeybind());
        read(root, "masaextraSuppressEasyPlaceOnAutoCollect", SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT);
            read(root, "masaextraAutoToggleEasyPlace", AUTO_TOGGLE_EASY_PLACE);
            readKeybind(root, "masaextraAutoToggleEasyPlaceHotkey", AUTO_TOGGLE_EASY_PLACE.getKeybind());
            if (root.has("masaextraAutoToggleEasyPlaceRange")) {
                AUTO_TOGGLE_EASY_PLACE_RANGE.setDoubleValue(root.get("masaextraAutoToggleEasyPlaceRange").getAsDouble());
            }
            read(root, "masaextraDedupeTillSound", DEDUPE_TILL_SOUND);
            read(root, "masaextraBilingualSearch", BILINGUAL_SEARCH);
            read(root, "masaextraSyncmaticaResync", SYNCMATICA_RESYNC);
            read(root, "masaextraRestorePlacementSelection", RESTORE_PLACEMENT_SELECTION);
            read(root, "masaextraRefreshMaterialListOnCollect", REFRESH_MATERIAL_LIST_ON_COLLECT);
            read(root, "masaextraRefreshGammaOnJoin", REFRESH_GAMMA_ON_JOIN);
            read(root, "masaextraOmmcHighlightStrictMatch", OMMC_HIGHLIGHT_STRICT_MATCH);
            read(root, "masaextraChatDebug", CHAT_DEBUG);
            read(root, "easyPlaceV2ProtocolExtra", EASY_PLACE_V2_PROTOCOL_EXTRA);
            if (root.has("masaextraCooldownMs")) {
                EASY_PLACE_COOLDOWN_MS.setIntegerValue(root.get("masaextraCooldownMs").getAsInt());
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void save() {
        try {
            JsonObject root = new JsonObject();
            write(root, "masaextraEnabled", ENABLED);
            write(root, "masaextraUseServerExact", USE_SERVER_EXACT);
            write(root, "masaextraDebugMode", DEBUG_MODE);
            write(root, "masaextraSuppressMissingSupport", SUPPRESS_MISSING_SUPPORT);
            write(root, "masaextraSnapAlignEnabled", SNAP_TO_BLOCK_CENTER_ENABLED);
            root.addProperty("masaextraSnapAlignTrigger", SNAP_TO_BLOCK_CENTER_TRIGGER.getOptionListValue().getStringValue());
            writeKeybind(root, "masaextraSnapAlignHotkey", SNAP_TO_BLOCK_CENTER_HOTKEY.getKeybind());
        write(root, "masaextraSuppressEasyPlaceOnAutoCollect", SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT);
            write(root, "masaextraAutoToggleEasyPlace", AUTO_TOGGLE_EASY_PLACE);
            writeKeybind(root, "masaextraAutoToggleEasyPlaceHotkey", AUTO_TOGGLE_EASY_PLACE.getKeybind());
            root.addProperty("masaextraAutoToggleEasyPlaceRange", AUTO_TOGGLE_EASY_PLACE_RANGE.getDoubleValue());
            write(root, "masaextraDedupeTillSound", DEDUPE_TILL_SOUND);
            write(root, "masaextraBilingualSearch", BILINGUAL_SEARCH);
            write(root, "masaextraSyncmaticaResync", SYNCMATICA_RESYNC);
            write(root, "masaextraRestorePlacementSelection", RESTORE_PLACEMENT_SELECTION);
            write(root, "masaextraRefreshMaterialListOnCollect", REFRESH_MATERIAL_LIST_ON_COLLECT);
            write(root, "masaextraRefreshGammaOnJoin", REFRESH_GAMMA_ON_JOIN);
            write(root, "masaextraOmmcHighlightStrictMatch", OMMC_HIGHLIGHT_STRICT_MATCH);
            write(root, "masaextraChatDebug", CHAT_DEBUG);
            write(root, "easyPlaceV2ProtocolExtra", EASY_PLACE_V2_PROTOCOL_EXTRA);
            root.addProperty("masaextraCooldownMs", EASY_PLACE_COOLDOWN_MS.getIntegerValue());

            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {
        }
    }

    /**
     * Keybinds are stored as raw GLFW key codes.
     *
     * <p>{@code IKeybind.getStringValue()} and {@code setValueFromString} are not
     * symmetric in this malilib build - the former produces a display name like
     * "G" while the latter resolves through {@code KeyCodes.getKeyCodeFromName},
     * which does not accept that form. Round-tripping through the string left the
     * keybind empty, so the stored hotkey silently stopped working after a
     * restart. Storing the codes avoids the conversion entirely.
     */
    private static void writeKeybind(JsonObject root, String key, IKeybind keybind) {
        JsonArray array = new JsonArray();
        for (int code : keybind.getKeys()) {
            array.add(code);
        }
        root.add(key, array);
    }

    private static void readKeybind(JsonObject root, String key, IKeybind keybind) {
        if (!root.has(key)) {
            return;
        }
        JsonElement element = root.get(key);
        keybind.clearKeys();
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                keybind.addKey(entry.getAsInt());
            }
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            // Legacy string form written by earlier builds.
            keybind.setValueFromString(element.getAsString());
        }
    }

    private static void read(JsonObject root, String key, ConfigBoolean option) {
        if (root.has(key)) {
            option.setBooleanValue(root.get(key).getAsBoolean());
        }
    }

    private static void write(JsonObject root, String key, ConfigBoolean option) {
        root.addProperty(key, option.getBooleanValue());
    }
}
