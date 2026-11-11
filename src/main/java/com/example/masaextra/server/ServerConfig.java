package com.example.masaextra.server;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class ServerConfig {
    public static boolean enabled = false;
    public static boolean requireItem = true;
    public static double maxDistance = 6.0D;
    public static int rateLimitPerSecond = 20;
    public static boolean allowBlockEntities = false;
    public static boolean debug = false;
    /**
     * Server-side counterpart of the client's easyPlaceV2ProtocolExtra. Mirrors the
     * client option so the server only decodes the extra protocol when the client
     * was told to send it.
     */
    public static boolean easyPlaceV2ProtocolExtra = true;
    /** Server-side counterpart of the client's keep-exact-rail-shape option. */
    public static boolean easyPlaceRailBlockNoShapeUpdate = true;
    public static final Set<String> blacklist = new HashSet<>(Set.of(
            "minecraft:bedrock",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:jigsaw",
            "minecraft:spawner",
            "minecraft:end_portal_frame"
    ));

    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("exactplace-server.json");

    private ServerConfig() {
    }

    public static void load() {
        try {
            if (!Files.exists(FILE)) {
                save();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (root.has("enabled")) enabled = root.get("enabled").getAsBoolean();
            if (root.has("requireItem")) requireItem = root.get("requireItem").getAsBoolean();
            if (root.has("maxDistance")) maxDistance = root.get("maxDistance").getAsDouble();
            if (root.has("rateLimitPerSecond")) rateLimitPerSecond = root.get("rateLimitPerSecond").getAsInt();
            if (root.has("allowBlockEntities")) allowBlockEntities = root.get("allowBlockEntities").getAsBoolean();
            if (root.has("debug")) debug = root.get("debug").getAsBoolean();
            if (root.has("easyPlaceV2ProtocolExtra")) easyPlaceV2ProtocolExtra = root.get("easyPlaceV2ProtocolExtra").getAsBoolean();
            if (root.has("easyPlaceRailBlockNoShapeUpdate")) easyPlaceRailBlockNoShapeUpdate = root.get("easyPlaceRailBlockNoShapeUpdate").getAsBoolean();
            if (root.has("blacklist") && root.get("blacklist").isJsonArray()) {
                blacklist.clear();
                JsonArray array = root.getAsJsonArray("blacklist");
                for (JsonElement element : array) {
                    blacklist.add(element.getAsString());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            root.addProperty("requireItem", requireItem);
            root.addProperty("maxDistance", maxDistance);
            root.addProperty("rateLimitPerSecond", rateLimitPerSecond);
            root.addProperty("allowBlockEntities", allowBlockEntities);
            root.addProperty("debug", debug);
            root.addProperty("easyPlaceV2ProtocolExtra", easyPlaceV2ProtocolExtra);
            root.addProperty("easyPlaceRailBlockNoShapeUpdate", easyPlaceRailBlockNoShapeUpdate);
            JsonArray array = new JsonArray();
            blacklist.forEach(array::add);
            root.add("blacklist", array);

            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {
        }
    }

    public static boolean isBlacklisted(Block block) {
        return blacklist.contains(BuiltInRegistries.BLOCK.getKey(block).toString());
    }
}
