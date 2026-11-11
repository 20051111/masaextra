package com.example.masaextra.mixin;

import com.example.masaextra.ExactPlaceConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * OMMC's highlight-entity whitelist matches entity ids/names with
 * String.contains(entry): "minecraft:item" also highlights
 * minecraft:item_frame (substring). We replace OMMC's decision entirely.
 *
 * Injected at HEAD and cancelled when enabled: the method returns immediately,
 * so OMMC's own @Inject at RETURN never runs. This removes any dependency on
 * mixin apply order / priority / cancellable chaining, which broke earlier
 * attempts (priority semantics, getReturnValue(), etc.).
 *
 * OMMC access is fully reflective; harmless without OMMC.
 */
@Mixin(Entity.class)
public class MixinOmmcHighlightEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger("masaextra");
    private static long lastDebugLogTime;
    private static boolean loggedInit;

    @Inject(
            method = "isCurrentlyGlowing()Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    @SuppressWarnings("unchecked")
    private void masaextra$ommcStrictMatch(CallbackInfoReturnable<Boolean> cir) {
        if (!ExactPlaceConfig.ENABLED.getBooleanValue()
                || !ExactPlaceConfig.OMMC_HIGHLIGHT_STRICT_MATCH.getBooleanValue()) {
            return;
        }
        Entity self = (Entity) (Object) this;

        // Genuine glowing (e.g. spectral arrow / /effect) always wins.
        if (self.hasGlowingTag()) {
            cir.setReturnValue(true);
            return;
        }

        try {
            Class<?> configs = Class.forName("com.plusls.ommc.game.Configs");
            Object listType = configs.getField("highlightEntityListType").get(null);
            Class<?> listTypeEnum =
                    Class.forName("fi.dy.masa.malilib.util.restrictions.UsageRestriction$ListType");
            if (listType != listTypeEnum.getField("WHITELIST").get(null)) {
                return; // blacklist / none: let OMMC's own RETURN inject decide
            }

            Object whiteList = configs.getField("highlightEntityWhiteList").get(null);
            List<String> entries = (List<String>) whiteList.getClass()
                    .getMethod("getStrings").invoke(whiteList);

            String id = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).toString();
            String name = self.getType().getDescription().getString();

            boolean strictMatch = false;
            for (String entry : entries) {
                if (id.equals(entry) || id.equals("minecraft:" + entry) || name.equals(entry)) {
                    strictMatch = true;
                    break;
                }
            }

            cir.setReturnValue(strictMatch);

            if (!loggedInit) {
                loggedInit = true;
                LOGGER.info("[masaextra] OMMC strict-match active, whitelist={}", entries);
            }
            // per-entity debug logging disabled: it floods the log every tick
        } catch (Throwable t) {
            if (!loggedInit) {
                loggedInit = true;
                LOGGER.info("[masaextra] OMMC strict-match reflection failed: {}", t.toString());
            }
        }
    }
}
