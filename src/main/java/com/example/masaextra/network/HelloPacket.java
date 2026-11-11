package com.example.masaextra.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record HelloPacket(int protocolVersion) implements FabricPacket {
    public static final PacketType<HelloPacket> TYPE = PacketType.create(
            new ResourceLocation("masaextra", "hello"), HelloPacket::new);

    public HelloPacket(FriendlyByteBuf buf) {
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
