package com.example.masaextra.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PlacePacket(BlockPos pos, int stateId) implements FabricPacket {
    public static final PacketType<PlacePacket> TYPE = PacketType.create(
            new ResourceLocation("exactplace", "place"), PlacePacket::new);

    public PlacePacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(stateId);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
