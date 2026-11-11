package com.example.masaextra.network;

/**
 * Port of chenjunfu2/EarlyCompat {@code EarlyCompatS2ClientHandler}'s server-support flag.
 *
 * <p>Set once the server answers the HI handshake with HI_ACK. Every protocol
 * rewrite on the client is gated on this: sending the extra protocol value to a
 * server that does not decode it would place the wrong block state, and can
 * also trip the server's own placement checks.
 */
public final class ServerSupport {
    private static volatile boolean available;

    private ServerSupport() {
    }

    /** True once the connected server has confirmed it runs the protocol. */
    public static boolean isSupportsExtraProtocol() {
        return available;
    }

    public static void setAvailable(boolean value) {
        available = value;
    }
}
