package com.example.masaextra.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client answer to {@link HiPacket}.
 *
 * @param protocolVersion protocol revision, checked by the client
 * @param modVersion      the server's MasaExtra version, for display only
 *
 * <p>The version field is read only when bytes remain, and writing it appends at
 * the tail, so client and server on different builds still talk to each other:
 * an old server yields an empty version, and an old client ignores the trailing
 * bytes.
 */
public record HelloPacket(int protocolVersion, String modVersion) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HelloPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("masaextra", "hello"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HelloPacket> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.protocolVersion());
                        buf.writeUtf(packet.modVersion() == null ? "" : packet.modVersion(), 64);
                    },
                    buf -> new HelloPacket(
                            buf.readVarInt(),
                            buf.readableBytes() > 0 ? buf.readUtf(64) : ""));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
