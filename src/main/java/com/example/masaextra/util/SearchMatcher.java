package com.example.masaextra.util;

import com.example.masaextra.ExactPlaceConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Bridges the config switch to {@link MatchRules}, and resolves the English label
 * for the various entry types a search box deals with.
 *
 * <p>One switch, three ways to match: Chinese (always), pinyin, and English. The
 * user asked for a single bilingual option rather than separate toggles, so the
 * rule is all-or-nothing.
 */
public final class SearchMatcher {
    private SearchMatcher() {
    }

    /** Reads the live config switch. */
    public static boolean matches(String displayText, String englishText, String query) {
        return MatchRules.matches(displayText, englishText, query,
                ExactPlaceConfig.BILINGUAL_SEARCH.getBooleanValue());
    }

    /** Convenience for a {@link Component} whose English label comes from its key. */
    public static boolean matches(String displayText, Component source, String query) {
        return matches(displayText, englishOf(source), query);
    }

    /** English label for a component, resolved through its translation key. */
    public static String englishOf(Component source) {
        if (source != null && source.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if (key != null && !key.isEmpty()) {
                return EnglishLang.get(key);
            }
        }
        return null;
    }

    /** English label for a raw translation key, as the config list stores. */
    public static String englishOfKey(String translationKey) {
        return translationKey == null || translationKey.isEmpty()
                ? null
                : EnglishLang.get(translationKey);
    }
}
