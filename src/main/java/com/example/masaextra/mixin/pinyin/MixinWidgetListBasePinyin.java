package com.example.masaextra.mixin.pinyin;

import com.example.masaextra.ExactPlaceConfig;
import com.example.masaextra.util.SearchMatcher;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extended matching for malilib's list filter: literal, pinyin, and English.
 *
 * <p>The list stores translation keys rather than display text, so both the
 * current-language label and the {@code en_us} label are resolved from the same
 * key before matching.
 *
 * <p>Client only: malilib does not exist on a dedicated server.
 */
@Mixin(WidgetListBase.class)
public class MixinWidgetListBasePinyin {
    @Inject(
            method = "matchesFilter(Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void masaextra$extendedMatches(String text, String filter,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!ExactPlaceConfig.BILINGUAL_SEARCH.getBooleanValue()) {
            return;
        }
        if (text == null || filter == null || filter.isEmpty()) {
            return;
        }

        String translated = StringUtils.translate(text);
        String english = SearchMatcher.englishOfKey(text);

        // Try the resolved label first, then the raw key: some entries are already
        // display strings, some are keys, and this covers both without branching.
        if (SearchMatcher.matches(translated, english, filter)
                || SearchMatcher.matches(text, english, filter)) {
            cir.setReturnValue(true);
        }
    }
}
