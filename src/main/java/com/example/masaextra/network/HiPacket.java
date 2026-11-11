package com.example.masaextra.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S "HI" handshake packet, ported from EarlyCompat.
 * The client sends this on game join; the server replies with HelloPacket (HI_ACK).
 */
public record HiPacket(int protocolVersion) implements FabricPacket {
    public static final PacketType<HiPacket> TYPE = PacketType.create(
            new ResourceLocation("masaextra", "hi"), HiPacket::new);

    public HiPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(protocolVersion);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
