package com.example.masaextra.protocol;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Port of chenjunfu2/EarlyCompat {@code EasyPlaceExtraProtocolHelper}.
 *
 * <p>Wire format: an ordinary float coordinate is in {@code [0, 1)} inside the
 * block. Adding {@code (protocolValue << 1) + 2} pushes it outside that range,
 * and the receiving side decodes it again. Bit 0 of the shifted value is
 * deliberately left unused so that floating point error cannot flip it.
 *
 * <p>Mappings: {@code Vec3d -> Vec3}, {@code util.math.BlockPos -> core.BlockPos}.
 */
public final class EasyPlaceExtraProtocolHelper {
    private EasyPlaceExtraProtocolHelper() {
    }

    /** True when a relative coordinate actually carries a protocol value. */
    public static boolean isProtocol(double relativeHitDim) {
        return relativeHitDim >= 2.0D;
    }

    /** Bit 3 marks the "extra" protocol used by the custom block adapters. */
    public static boolean isExtraProtocol(int protocolValue) {
        return (protocolValue & 0b0000_1000) == 0b0000_1000;
    }

    public static double getRelativeHitX(Vec3 hitPos, BlockPos blockPos) {
        return hitPos.x - (double) blockPos.getX();
    }

    public static double getRelativeHitZ(Vec3 hitPos, BlockPos blockPos) {
        return hitPos.z - (double) blockPos.getZ();
    }

    public static int decodeProtocolValueFromHitDim(double relativeHitDim) {
        return ((int) relativeHitDim - 2) >>> 1;
    }

    public static double encodeProtocolValueToHitDim(double relativeHitDim, int protocolValue) {
        return relativeHitDim + (double) ((protocolValue << 1) + 2);
    }

    /** Strip the marker bit and repack, leaving 6 usable bits. */
    public static int extraProtocolValueToRawProtocolValue(int protocolValue) {
        return ((protocolValue & 0b1111_0000) >>> 1) | (protocolValue & 0b0000_0111);
    }

    /** Split the value around bit 3 and set the marker bit. */
    public static int rawProtocolValueToExtraProtocolValue(int protocolValue) {
        return ((protocolValue & 0b0111_1000) << 1) | (protocolValue & 0b0000_0111) | 0b0000_1000;
    }

    public static int addExtraProtocolBit(int protocolValue) {
        return protocolValue | 0b0000_1000;
    }

    public static int removeExtraProtocolBit(int protocolValue) {
        return protocolValue & ~0b0000_1000;
    }

    public static Vec3 encodeProtocolValueToHitVecX(int protocolValue, Vec3 hitVec) {
        return new Vec3(encodeProtocolValueToHitDim(hitVec.x, protocolValue), hitVec.y, hitVec.z);
    }

    public static Vec3 encodeProtocolValueToHitVecZ(int protocolAdditionValue, Vec3 hitVec) {
        return new Vec3(hitVec.x, hitVec.y, encodeProtocolValueToHitDim(hitVec.z, protocolAdditionValue));
    }

    public static Vec3 encodeExtraProtocolValueToHitVecX(int protocolValue, Vec3 hitVec) {
        int extraProtocolValue = rawProtocolValueToExtraProtocolValue(protocolValue);
        return encodeProtocolValueToHitVecX(extraProtocolValue, hitVec);
    }
}
