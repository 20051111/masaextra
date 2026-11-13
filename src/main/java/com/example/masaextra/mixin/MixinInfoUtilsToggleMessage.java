package com.example.masaextra.mixin;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes the action bar text shown when a hotkeyed option is toggled.
 *
 * <p>malilib builds the state word with
 * {@code StringUtils.translate("malilib.message." + (on ? "on" : "off"))},
 * but the keys that actually exist in its language files are
 * {@code malilib.message.value.on} and {@code malilib.message.value.off}.
 * {@code StringUtils.translate} falls through to {@code I18n.get}, which returns
 * the key itself when nothing matches, so the action bar reads
 * {@code 功能 自动开关轻松放置 malilib.message.on} instead of
 * {@code 功能 自动开关轻松放置 开启}.
 *
 * <p>The same wrong key is used for every {@code ConfigBooleanHotkeyed} in every
 * mod, so correcting it here fixes Tweakeroo and Tweakermore toggles as well.
 *
 * <p>Client only: the action bar belongs to the client, and this class references
 * malilib which does not exist on a dedicated server.
 */
@Mixin(InfoUtils.class)
public class MixinInfoUtilsToggleMessage {
    @Inject(method = "printBooleanConfigToggleMessage", at = @At("HEAD"), cancellable = true)
    private static void masaextra$correctToggleMessage(String configName, boolean value, CallbackInfo ci) {
        String color = value ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        // The keys that really exist, with the .value. segment malilib omits.
        String state = StringUtils.translate("malilib.message.value." + (value ? "on" : "off"));
        InfoUtils.printActionbarMessage(StringUtils.translate(
                "malilib.message.toggled", configName, color + state + GuiBase.TXT_RST));
        ci.cancel();
    }
}
