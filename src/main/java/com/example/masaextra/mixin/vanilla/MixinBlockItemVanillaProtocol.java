package com.example.masaextra.mixin.vanilla;

import com.example.masaextra.protocol.PlaceStateAccessor;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.getRelativeHitX;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.getRelativeHitZ;
import static com.example.masaextra.protocol.EasyPlaceExtraProtocolHelper.isProtocol;

/**
 * Port of chenjunfu2/EarlyCompat {@code BlockItemMixin_VanillaProtocolCompat},
 * reduced to the part that still applies on 1.20.4.
 *
 * <p>EarlyCompat's other job - rebuilding the item stack from the Z-channel
 * protocol value - only ever served {@code ItemStackProtocolDataAdapter}
 * implementations, which in EarlyCompat are decorated pots and crafters. Neither
 * block exists in 1.20.4, so no adapter implements the interface and the
 * {@code @WrapOperation} on {@code updateCustomBlockEntityTag} could never fire.
 * It is dropped rather than kept as dead weight: it was also the injection that
 * failed here, because {@code useOn} keeps its context only in the parameter
 * slot and the shipped jars have no LocalVariableTable to capture from.
 *
 * <p>What remains is the flag that tells blocks whether the placement in flight
 * came from the exact-placement protocol, which the rail and lever adapters read.
 */
@Mixin(BlockItem.class)
public abstract class MixinBlockItemVanillaProtocol implements PlaceStateAccessor {
    @Unique
    private static boolean masaextra$isEasyPlaceState = false;

    @Unique
    private static long masaextra$placeProperty = 0L;

    @Override
    public boolean isEasyPlaceState() {
        return masaextra$isEasyPlaceState;
    }

    @Override
    public long placeProperty() {
        return masaextra$placeProperty;
    }

    @Override
    public void placeProperty(long value) {
        masaextra$placeProperty = value;
    }

    /**
     * A hit vector outside the unit cube means a protocol value is present.
     * X carries the block-state protocol, Z the item-stack data.
     */
    @Unique
    private static boolean masaextra$hitCarriesProtocol(UseOnContext context) {
        Vec3 hit = context.getClickLocation();
        BlockPos pos = context.getClickedPos();
        return isProtocol(getRelativeHitX(hit, pos)) || isProtocol(getRelativeHitZ(hit, pos));
    }

    @Inject(method = "useOn", at = @At(value = "HEAD"))
    private void masaextra$setPlaceState(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (masaextra$hitCarriesProtocol(context)) {
            masaextra$isEasyPlaceState = true;
        }
    }

    @Inject(method = "useOn", at = @At(value = "RETURN"))
    private void masaextra$clearPlaceState(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        masaextra$isEasyPlaceState = false;
        masaextra$placeProperty = 0L;
    }
}
