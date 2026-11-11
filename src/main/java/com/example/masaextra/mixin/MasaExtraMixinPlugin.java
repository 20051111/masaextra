package com.example.masaextra.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Port of the optional-dependency gating from chenjunfu2/EarlyCompat's
 * {@code MixinPlugin}.
 *
 * <p>Only the carpet-extra group needs gating here: it targets a class that ships
 * with another mod, so without that mod present the mixin would fail to find its
 * target and crash the game during startup. Everything else targets Minecraft or
 * Litematica, which load earlier and are checked in the normal way.
 */
public class MasaExtraMixinPlugin implements IMixinConfigPlugin {
    private static final String CARPET_EXTRA_ID = "carpet-extra";

    private boolean carpetExtraAvailable;

    @Override
    public void onLoad(String mixinPackage) {
        carpetExtraAvailable = FabricLoader.getInstance().isModLoaded(CARPET_EXTRA_ID);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.example.masaextra.mixin.carpetextra.")) {
            return carpetExtraAvailable;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
