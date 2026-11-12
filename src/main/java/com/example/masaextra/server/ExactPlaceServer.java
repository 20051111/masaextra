package com.example.masaextra.server;

import com.example.masaextra.network.HelloPacket;
import com.example.masaextra.network.HiPacket;
import com.example.masaextra.network.PlacePacket;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server side of the exact placement protocol.
 *
 * HI / HI_ACK handshake (EarlyCompat style) tells the client the server
 * supports it. Placement itself uses masaextra:place: the client sends the
 * exact schematic state id, the server validates it and applies it with
 * setBlock, so the resulting state is always exactly the schematic one.
 */
public class ExactPlaceServer implements DedicatedServerModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("masaextra");
    private static final Set<UUID> extraProtocolPlayers = new HashSet<>();
    private static final Map<UUID, Deque<Long>> RATE = new HashMap<>();

    /** This build's version string, reported back so the client can display it. */
    private static String masaextra$modVersion() {
        return FabricLoader.getInstance().getModContainer("masaextra")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    @Override
    public void onInitializeServer() {
        ServerConfig.load();
        com.example.masaextra.protocol.BlockPlacer.DEBUG_ENABLED = ServerConfig.debug;

        ServerPlayNetworking.registerGlobalReceiver(HiPacket.TYPE, (packet, player, sender) -> {
            if (!ServerConfig.enabled) {
                LOGGER.info("[masaextra] HI from {} ignored: enabled=false", name(player));
                return;
            }
            extraProtocolPlayers.add(player.getUUID());
            ServerPlayNetworking.send(player, new HelloPacket(1, masaextra$modVersion()));
            LOGGER.info("[masaextra] HI from {} -> HI_ACK", name(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(PlacePacket.TYPE, (packet, player, sender) -> {
            if (player.getServer() != null) {
                player.getServer().execute(() -> place(player, packet));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            extraProtocolPlayers.remove(handler.player.getUUID());
            RATE.remove(handler.player.getUUID());
        });

        if (ServerConfig.debug) {
            LOGGER.info("[masaextra] server enabled={} requireItem={} maxDistance={}",
                    ServerConfig.enabled, ServerConfig.requireItem, ServerConfig.maxDistance);
        }
    }

    public static boolean isExtraProtocolPlayer(ServerPlayer player) {
        return extraProtocolPlayers.contains(player.getUUID());
    }

    private static void place(ServerPlayer player, PlacePacket packet) {
        if (!ServerConfig.enabled || !extraProtocolPlayers.contains(player.getUUID())) {
            return;
        }

        BlockPos pos = packet.pos();
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos)) {
            return;
        }
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos))
                > ServerConfig.maxDistance * ServerConfig.maxDistance) {
            debug("reject reach {} {}", name(player), pos);
            return;
        }

        BlockState state = Block.stateById(packet.stateId());
        if (state.isAir() || !state.canSurvive(level, pos)) {
            debug("reject survive {} {} {}", name(player), pos, state);
            return;
        }
        if (ServerConfig.isBlacklisted(state.getBlock())) {
            debug("reject blacklist {}", state);
            return;
        }
        if (!ServerConfig.allowBlockEntities && state.hasBlockEntity()) {
            debug("reject block entity {}", state);
            return;
        }
        if (ServerConfig.requireItem && !player.isCreative()
                && !player.getMainHandItem().is(state.getBlock().asItem())) {
            debug("reject item {} needs {}", name(player), state.getBlock().asItem());
            return;
        }
        if (!allowRate(player)) {
            debug("reject rate {}", name(player));
            return;
        }

        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return;
        }

        if (!player.isCreative() && ServerConfig.requireItem) {
            player.getMainHandItem().shrink(1);
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        debug("placed {} at {} for {}", state, pos, name(player));

        // The block can be removed again by the neighbour updates that follow
        // setBlock (a wall torch without support drops immediately). Verify on
        // the next tick so a silent vanish is visible in the log.
        if (ServerConfig.debug) {
            player.getServer().execute(() -> {
                BlockState after = level.getBlockState(pos);
                if (after.getBlock() == state.getBlock()) {
                    debug("confirmed {} at {}", after, pos);
                } else {
                    debug("VANISHED {} at {} -> now {}", state, pos, after);
                }
            });
        }
    }

    private static void debug(String format, Object... args) {
        if (ServerConfig.debug) {
            LOGGER.info("[masaextra] " + format, args);
        }
    }

    private static boolean allowRate(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Deque<Long> times = RATE.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() > 1000L) {
            times.pollFirst();
        }
        if (times.size() >= Math.max(1, ServerConfig.rateLimitPerSecond)) {
            return false;
        }
        times.addLast(now);
        return true;
    }

    private static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }
}