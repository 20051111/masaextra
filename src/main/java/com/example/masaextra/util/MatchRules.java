package com.example.masaextra.util;

import java.util.Locale;

/**
 * The pure matching rule behind every search box this mod touches: literal,
 * pinyin, and English, with a single on/off switch passed in.
 *
 * <p>Split out from {@link SearchMatcher} so the rule can be exercised without
 * malilib or the game on the classpath - the caller there only reads the config
 * and delegates here.
 *
 * <p>Order matters for cost: the literal check is a plain {@code contains}, the
 * pinyin check converts hanzi, and the English check may load a language table.
 * A query that already matches literally never reaches the expensive stages.
 */
public final class MatchRules {
    private MatchRules() {
    }

    /**
     * @param displayText what the user sees in the current language
     * @param englishText the English label, or null when same or unknown
     * @param query       what was typed
     * @param enabled     whether pinyin and English matching are active
     */
    public static boolean matches(String displayText, String englishText, String query,
                                  boolean enabled) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String text = displayText == null ? "" : displayText;
        String q = query.toLowerCase(Locale.ROOT);

        // 1. Literal. Always on, and the only thing a vanilla search does.
        if (text.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (!enabled) {
            return false;
        }

        // 2. Pinyin of the visible text.
        if (PinyinSearch.matches(text, query)) {
            return true;
        }

        // 3. English label. Skipped when it is the same string, which is the case
        //    for literal component names and any key the table does not know.
        if (englishText != null && !englishText.isEmpty() && !englishText.equals(text)) {
            if (englishText.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
            if (PinyinSearch.matches(englishText, query)) {
                return true;
            }
        }

        return false;
    }
}
