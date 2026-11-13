package com.example.masaextra.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server request to place a specific block state at a position.
 *
 * <p>Keeps the original {@code exactplace} channel name so a 1.20.4 client and a
 * 1.21.8 server cannot silently half-talk to each other - they simply will not
 * find each other's channels, which is the safe outcome.
 */
public record PlacePacket(BlockPos pos, int stateId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlacePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("exactplace", "place"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacePacket> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeBlockPos(packet.pos());
                        buf.writeVarInt(packet.stateId());
                    },
                    buf -> new PlacePacket(buf.readBlockPos(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
