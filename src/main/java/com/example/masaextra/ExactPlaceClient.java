package com.example.masaextra;

import com.example.masaextra.network.HelloPacket;
import com.example.masaextra.network.HiPacket;
import com.example.masaextra.network.ServerSupport;
import fi.dy.masa.malilib.config.ConfigManager;
import com.example.masaextra.compat.AutoEasyPlaceCompat;
import com.example.masaextra.compat.MasaExtraKeybindProvider;
import com.example.masaextra.compat.SyncmaticaResync;
import com.example.masaextra.compat.TweakermoreAutoCollectCompat;
import com.example.masaextra.protocol.ClientOptions;
import com.example.masaextra.compat.TweakerooSnapAimCompat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExactPlaceClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("masaextra");
    /**
     * How long to keep retrying the HI handshake after a join, in ticks.
     *
     * <p>ClientPlayConnectionEvents.JOIN also fires on respawn and dimension
     * change, and it can fire before the custom-payload channel is usable. The
     * old 40-tick window (2 s) meant a single missed HI_ACK left ServerSupport
     * false for the rest of the session, silently disabling every protocol
     * rewrite - the encoder logged "SKIP disabled server=false" 53 times in a row
     * and Litematica's own encoding took over, placing floor torches where the
     * schematic had wall torches. 600 ticks = 30 s, retried once a second.
     */
    private static final int MAX_HANDSHAKE_TICKS = 600;
    /** Retry once a second rather than 4x over 2 seconds. */
    private static final int RETRY_INTERVAL_TICKS = 20;

    private static int handshakeTicks;
    private static boolean warnedHandshakeTimeout;

    private static void debug(String message, Object... args) {
        if (ExactPlaceConfig.DEBUG_MODE.getBooleanValue()) {
            LOGGER.info("[masaextra] " + message, args);
        }
    }

    @Override
    public void onInitializeClient() {
        ExactPlaceConfig.INSTANCE.load();

        com.example.masaextra.protocol.BlockPlacer.DEBUG_ENABLED = ExactPlaceConfig.DEBUG_MODE.getBooleanValue();
        ConfigManager.getInstance().registerConfigHandler("masaextra", ExactPlaceConfig.INSTANCE);

        // Same pattern as Tweakeroo / Tweakermore: without a keybind provider the
        // hotkeys never reach malilib's key map and can never fire.
        MasaExtraKeybindProvider.register();

        PayloadTypeRegistry.playS2C().register(HelloPacket.TYPE, HelloPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(HiPacket.TYPE, HiPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(HelloPacket.TYPE, (payload, context) -> {
            if (payload.protocolVersion() >= 1) {
                ServerSupport.setServerInfo(payload.protocolVersion(), payload.modVersion());
                debug("received HI_ACK -> ServerSupport=true (server version={})", payload.modVersion());
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerSupport.setAvailable(false);
            handshakeTicks = 0;
            warnedHandshakeTimeout = false;
            if (masaextra$isActive()) {
                sendHi("JOIN");
            }
            GammaRefresher.onJoin();
            SyncmaticaResync.onJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerSupport.setAvailable(false);
            handshakeTicks = 0;
            warnedHandshakeTimeout = false;
            SyncmaticaResync.onDisconnect();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Mirror the client-only options into the plain-boolean holder the
            // shared mixins read, so those classes never touch malilib types.
            ClientOptions.sync(
                    ExactPlaceConfig.ENABLED.getBooleanValue(),
                    ExactPlaceConfig.USE_SERVER_EXACT.getBooleanValue(),
                    ExactPlaceConfig.EASY_PLACE_V2_PROTOCOL_EXTRA.getBooleanValue(),
                    ExactPlaceConfig.SUPPRESS_MISSING_SUPPORT.getBooleanValue());
            GammaRefresher.tick(client);
            SyncmaticaResync.onTick();
            // Keep retrying while connected but unconfirmed: the JOIN event can
            // fire before the channel exists, and a missed HI_ACK would otherwise
            // disable the whole protocol for the session.
            if (client.player == null || ServerSupport.isSupportsExtraProtocol()
                    || !masaextra$isActive()) {
                return;
            }
            if (handshakeTicks >= MAX_HANDSHAKE_TICKS) {
                if (!warnedHandshakeTimeout) {
                    warnedHandshakeTimeout = true;
                    // Distinguish the two causes: no channel at all means the server
                    // does not run masaextra (or has it disabled); a present channel
                    // that never answers means HI was dropped.
                    if (ClientPlayNetworking.canSend(HiPacket.TYPE)) {
                        LOGGER.warn("[masaextra] the server runs masaextra but declined HI after {} ticks. "
                                + "Its receiver only answers when ServerConfig.enabled is true, and that "
                                + "flag defaults to false: make sure the server has "
                                + "config/exactplace-server.json with \"enabled\": true, then restart it "
                                + "and reconnect. The server log will show either "
                                + "'[masaextra] server enabled=true ...' or "
                                + "'[masaextra] HI from ... ignored: enabled=false'. "
                                + "Until then exact placement falls back to Litematica's own encoding.",
                                MAX_HANDSHAKE_TICKS);
                    } else {
                        LOGGER.warn("[masaextra] server does not offer the masaextra channel "
                                + "(mod missing, disabled via exactplace-server.json, or mismatched); "
                                + "exact placement falls back to Litematica's own encoding");
                    }
                }
                return;
            }
            if (handshakeTicks % RETRY_INTERVAL_TICKS == 0) {
                sendHi("retry#" + handshakeTicks);
            }
            handshakeTicks++;
        });

        AutoEasyPlaceCompat.register();

        // Optional integration: the class references Tweakeroo types, so it is
        // only touched when the mod is actually present. The catch keeps a
        // mismatched Tweakeroo version from taking the whole client down.
        if (FabricLoader.getInstance().isModLoaded("tweakeroo")) {
            try {
                TweakerooSnapAimCompat.register();
            } catch (Throwable t) {
                LOGGER.warn("[masaextra] Tweakeroo snap aim alignment unavailable", t);
            }
        }
        if (FabricLoader.getInstance().isModLoaded("tweakermore")) {
            try {
                TweakermoreAutoCollectCompat.register();
        SyncmaticaResync.register();
            } catch (Throwable t) {
                LOGGER.warn("[masaextra] Tweakermore auto-collect integration unavailable", t);
            }
        }
    }

    /**
     * Master switch plus the "use the server protocol" option. Both default to
     * false, so a fresh install never sends the handshake until the user turns
     * the feature on, and no protocol rewriting happens either.
     */
    private static boolean masaextra$isActive() {
        return ExactPlaceConfig.ENABLED.getBooleanValue()
                && ExactPlaceConfig.USE_SERVER_EXACT.getBooleanValue();
    }

    /**
     * Send HI to the server.
     *
     * @return true when the packet was actually handed to the network layer
     */
    private static boolean sendHi(String reason) {
        if (!ClientPlayNetworking.canSend(HiPacket.TYPE)) {
            // The server only advertises the channel when masaextra is installed
            // and enabled there, so this is also our "is the server running us?"
            // signal.
            debug("{}: cannot send masaextra:hi (server does not offer the channel)", reason);
            return false;
        }
        try {
            ClientPlayNetworking.send(new HiPacket(1));
            debug("{}: masaextra:hi sent", reason);
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[masaextra] {}: failed to send HI", reason, t);
            return false;
        }
    }
}
