package com.example.masaextra.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pinyin matching for search boxes, so a Chinese item or option can be found by
 * typing its full pinyin ("shuizhi") or just the initials ("sz").
 *
 * <p>Ported from WuHuClient's {@code PinYinSearch}, with three changes that
 * matter for the way it is used here:
 *
 * <ul>
 *   <li><b>Cached.</b> The original regenerates every pinyin combination on each
 *       call. Search runs over the whole item registry and re-runs on every
 *       keystroke, so the same few thousand names were being converted over and
 *       over. Results are memoised per text; the inputs are a fixed set of names,
 *       so the hit rate is near total.</li>
 *   <li><b>Bounded.</b> A word with many multi-reading characters explodes
 *       combinatorially ({@code 2^n}). Combinations are capped, and the cap is
 *       applied breadth-first so the plain readings are kept and only exotic
 *       variants are dropped.</li>
 *   <li><b>Short-circuited.</b> Pure-ASCII input compares directly, and the raw
 *       text is checked before any pinyin work is done.</li>
 * </ul>
 *
 * <p>Multi-reading characters still produce several candidates within the cap,
 * and a match on any one of them counts, so searching by a non-default reading
 * keeps working.
 */
public final class PinyinSearch {
    /** Upper bound on stored combinations per text, to stop 2^n growth. */
    private static final int MAX_COMBINATIONS = 16;
    /** Upper bound on cached texts, roughly 2000 names * a few strings. */
    private static final int CACHE_LIMIT = 4096;

    private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();
    private static final HanyuPinyinOutputFormat FORMAT = createFormat();

    private PinyinSearch() {
    }

    private static HanyuPinyinOutputFormat createFormat() {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        return format;
    }

    /**
     * Whether {@code text} matches {@code query}, by literal containment or by any
     * of its pinyin forms.
     */
    public static boolean matches(String text, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        if (text == null || text.isEmpty()) {
            return false;
        }

        String q = query.toLowerCase(Locale.ROOT);
        String lower = text.toLowerCase(Locale.ROOT);

        // Literal hit first: no pinyin work at all for the common case.
        if (lower.contains(q)) {
            return true;
        }
        // Both sides ASCII means pinyin cannot add anything.
        if (isAscii(text)) {
            return false;
        }

        for (String candidate : pinyinOf(text)) {
            if (candidate.contains(q)) {
                return true;
            }
        }
        return false;
    }

    /** Cached pinyin candidates for a text. */
    private static List<String> pinyinOf(String text) {
        List<String> cached = CACHE.get(text);
        if (cached != null) {
            return cached;
        }
        List<String> built = buildPinyin(text);
        if (CACHE.size() < CACHE_LIMIT) {
            CACHE.put(text, built);
        }
        return built;
    }

    /**
     * Builds the candidate list: the joined full pinyin and the joined initials,
     * breadth-first so ordinary readings come before multi-reading variants.
     */
    private static List<String> buildPinyin(String text) {
        char[] chars = text.toCharArray();
        List<String[]> readingsPerChar = new ArrayList<>(chars.length);

        for (char c : chars) {
            if (c <= 128) {
                readingsPerChar.add(new String[] { String.valueOf(Character.toLowerCase(c)) });
                continue;
            }
            String[] readings = null;
            try {
                readings = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
            } catch (Exception ignored) {
                // Unmappable character: fall through and keep it literally.
            }
            if (readings == null || readings.length == 0) {
                readingsPerChar.add(new String[] { String.valueOf(c) });
            } else {
                String[] lowered = new String[readings.length];
                for (int i = 0; i < readings.length; i++) {
                    lowered[i] = readings[i] == null ? null : readings[i].toLowerCase(Locale.ROOT);
                }
                readingsPerChar.add(lowered);
            }
        }

        List<String> fullForms = new ArrayList<>();
        List<String> initialForms = new ArrayList<>();
        fullForms.add("");
        initialForms.add("");

        for (String[] readings : readingsPerChar) {
            List<String> nextFull = new ArrayList<>();
            List<String> nextInitial = new ArrayList<>();
            outer:
            for (String reading : readings) {
                if (reading == null || reading.isEmpty()) {
                    continue;
                }
                char head = Character.toLowerCase(reading.charAt(0));
                for (int i = 0; i < fullForms.size(); i++) {
                    if (nextFull.size() >= MAX_COMBINATIONS) {
                        break outer;
                    }
                    nextFull.add(fullForms.get(i) + reading);
                    nextInitial.add(initialForms.get(i) + head);
                }
            }
            if (nextFull.isEmpty()) {
                // Every reading was unusable; keep the literal character.
                for (int i = 0; i < fullForms.size(); i++) {
                    nextFull.add(fullForms.get(i) + chars[0]);
                    nextInitial.add(initialForms.get(i));
                }
            }
            fullForms = nextFull;
            initialForms = nextInitial;
        }

        LinkedHashSet<String> merged = new LinkedHashSet<>(fullForms);
        merged.addAll(initialForms);
        return new ArrayList<>(merged);
    }

    private static boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 128) {
                return false;
            }
        }
        return true;
    }

    /** Test hook: the candidates generated for a text. */
    public static List<String> debugForms(String text) {
        return new ArrayList<>(pinyinOf(text));
    }

    /** Test hook: drop the memoised results. */
    public static void clearCache() {
        CACHE.clear();
    }

    /** Test hook: current cache size. */
    public static int cacheSize() {
        return CACHE.size();
    }
}
