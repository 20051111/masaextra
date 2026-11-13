package com.example.masaextra.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S "HI" handshake packet, ported from EarlyCompat.
 * The client sends this on game join; the server replies with {@link HelloPacket}.
 *
 * <p>1.21 removed Fabric API's {@code FabricPacket}/{@code PacketType} wrappers;
 * payloads implement {@link CustomPacketPayload} and carry a {@link StreamCodec}
 * registered through {@code PayloadTypeRegistry}.
 */
public record HiPacket(int protocolVersion) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HiPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("masaextra", "hi"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HiPacket> CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeVarInt(packet.protocolVersion()),
                    buf -> new HiPacket(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
