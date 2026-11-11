package com.example.masaextra;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.IConfigHandler;
import com.example.masaextra.config.SnapAlignTrigger;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
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
            if (root.has("masaextraSnapAlignHotkey")) {
                SNAP_TO_BLOCK_CENTER_HOTKEY.setValueFromString(root.get("masaextraSnapAlignHotkey").getAsString());
            }
        read(root, "masaextraSuppressEasyPlaceOnAutoCollect", SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT);
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
            root.addProperty("masaextraSnapAlignHotkey", SNAP_TO_BLOCK_CENTER_HOTKEY.getStringValue());
        write(root, "masaextraSuppressEasyPlaceOnAutoCollect", SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT);
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

    private static void read(JsonObject root, String key, ConfigBoolean option) {
        if (root.has(key)) {
            option.setBooleanValue(root.get(key).getAsBoolean());
        }
    }

    private static void write(JsonObject root, String key, ConfigBoolean option) {
        root.addProperty(key, option.getBooleanValue());
    }
}
