package com.example.masaextra.mixin.pinyin;

import com.example.masaextra.ExactPlaceConfig;
import com.example.masaextra.util.SearchMatcher;
import net.minecraft.client.searchtree.SuffixArray;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Extended matching for Minecraft's own search trees: the creative inventory and
 * the recipe book both feed a {@code SuffixArray}.
 *
 * <p>{@code SuffixArray} compiles its strings once and answers {@code search} by
 * literal prefix, which cannot express pinyin or English. This appends the entries
 * whose display name matches the query by those routes, keeping the original
 * results and their order, so a literal prefix hit still ranks as before.
 *
 * <p>Injected at RETURN and merged into the returned list rather than pushed into
 * the caller's menu: {@code search} runs again on every keystroke, and writing to
 * the menu would accumulate duplicates as the user types.
 *
 * <p>Not restricted to the creative inventory - the recipe book gets the same
 * benefit, and only {@code ItemStack} entries are inspected, so other uses of
 * {@code SuffixArray} are untouched.
 *
 * <p>Client only: the whole class lives in the client source set of Minecraft.
 */
@Mixin(SuffixArray.class)
public abstract class MixinSuffixArrayPinyin<T> {
    @Shadow
    protected List<T> list;

    @Inject(method = "search", at = @At("RETURN"), cancellable = true)
    private void masaextra$extendedSearch(String query, CallbackInfoReturnable<List<T>> cir) {
        if (!ExactPlaceConfig.BILINGUAL_SEARCH.getBooleanValue()) {
            return;
        }
        if (query == null || query.isEmpty()) {
            return;
        }

        List<T> original = cir.getReturnValue();
        List<T> merged = original == null ? new ArrayList<>() : new ArrayList<>(original);
        boolean added = false;

        for (T entry : this.list) {
            if (!(entry instanceof ItemStack stack) || merged.contains(entry)) {
                continue;
            }
            Component name = stack.getHoverName();
            if (SearchMatcher.matches(name.getString(), name, query)) {
                merged.add(entry);
                added = true;
            }
        }

        if (added) {
            cir.setReturnValue(merged);
        }
    }
}
