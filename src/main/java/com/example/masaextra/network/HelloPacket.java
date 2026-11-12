package com.example.masaextra.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
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
public record HelloPacket(int protocolVersion, String modVersion) implements FabricPacket {
    public static final PacketType<HelloPacket> TYPE = PacketType.create(
            new ResourceLocation("masaextra", "hello"), HelloPacket::new);

    public HelloPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readableBytes() > 0 ? buf.readUtf(64) : "");
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(protocolVersion);
        buf.writeUtf(modVersion == null ? "" : modVersion, 64);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
