package com.example.masaextra.network;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * Port of chenjunfu2/EarlyCompat {@code EarlyCompatS2ClientHandler}'s server-support flag.
 *
 * <p>Set once the server answers the HI handshake with HI_ACK. Every protocol
 * rewrite on the client is gated on this: sending the extra protocol value to a
 * server that does not decode it would place the wrong block state, and can
 * also trip the server's own placement checks.
 *
 * <p>Also remembers the server's MasaExtra version so the config GUI can show
 * whether the other side is present and which build it runs.
 */
public final class ServerSupport {
    private static volatile boolean available;
    private static volatile String serverVersion = "";

    private ServerSupport() {
    }

    /** True once the connected server has confirmed it runs the protocol. */
    public static boolean isSupportsExtraProtocol() {
        return available;
    }

    public static void setAvailable(boolean value) {
        available = value;
        if (!value) {
            serverVersion = "";
        }
    }

    /** Records the answer to the handshake. */
    public static void setServerInfo(int protocolVersion, String version) {
        if (protocolVersion >= 1) {
            available = true;
            serverVersion = version == null ? "" : version;
        }
    }

    /** One-line status for the config GUI. */
    public static String getServerInfoText() {
        if (!available) {
            return StringUtils.translate("masaextra.info.server_missing");
        }
        if (serverVersion.isEmpty()) {
            return StringUtils.translate("masaextra.info.server_present_unknown");
        }
        return StringUtils.translate("masaextra.info.server_present", serverVersion);
    }
}
