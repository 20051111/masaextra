package com.example.masaextra.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * English display names, independent of the active language.
 *
 * <p>A Chinese client only ever sees Chinese strings, so matching an English query
 * against {@code getString()} can never work. The English labels do exist though -
 * every mod ships an {@code en_us.json} whose keys are the very translation keys
 * the Chinese names come from. This loads those tables once and answers by key.
 *
 * <p>All namespaces are scanned, so items from other mods resolve too. First key
 * wins: the built-in resources are listed first, which keeps Minecraft's own
 * labels stable when a mod overrides one.
 */
public final class EnglishLang {
    private static final Map<String, String> TABLE = new ConcurrentHashMap<>();
    /** Read once; a failure is not retried, an empty table just disables matching. */
    private static volatile boolean attempted;

    private EnglishLang() {
    }

    /** English text for a translation key, or the key itself when unknown. */
    public static String get(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        ensureLoaded();
        String english = TABLE.get(key);
        return english != null ? english : key;
    }

    public static boolean isLoaded() {
        return attempted;
    }

    public static int size() {
        return TABLE.size();
    }

    private static void ensureLoaded() {
        if (attempted) {
            return;
        }
        synchronized (EnglishLang.class) {
            if (attempted) {
                return;
            }
            attempted = true;
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc == null) {
                    return;
                }
                ResourceManager manager = mc.getResourceManager();
                Map<ResourceLocation, Resource> found =
                        manager.listResources("lang", id -> id.getPath().endsWith("en_us.json"));
                for (Resource resource : found.values()) {
                    loadOne(resource);
                }
            } catch (Exception ignored) {
                // Leave the table empty; callers fall back to the key.
            }
        }
    }

    private static void loadOne(Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                try {
                    TABLE.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
                } catch (Exception ignored) {
                    // Non-string values are not display names; skip them.
                }
            }
        } catch (Exception ignored) {
            // Unreadable pack: skip it and keep whatever we already have.
        }
    }
}
