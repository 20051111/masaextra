package com.example.masaextra.protocol;

/**
 * Mirror of the client-only options that mixins registered for both sides need.
 *
 * <p>Every mixin listed in the shared {@code mixins} array is loaded on a dedicated
 * server too, and that server has no malilib, so those classes must not reference
 * {@code ExactPlaceConfig} at all - not even from inside a client-only branch.
 * A class reference sitting in the constant pool is a landmine: the moment any
 * code path reaches it the JVM throws {@code NoClassDefFoundError}, and the
 * server dies. This holder carries plain booleans instead, kept in sync by the
 * client entrypoint, so a server simply reads the defaults (everything off).
 */
public final class ClientOptions {
    public static volatile boolean enabled = false;
    public static volatile boolean useServerExact = false;
    public static volatile boolean v2ProtocolExtra = false;
    public static volatile boolean suppressMissingSupport = true;

    private ClientOptions() {
    }

    /** Called by the client entrypoint whenever the config values may have changed. */
    public static void sync(boolean enabled, boolean useServerExact,
                            boolean v2ProtocolExtra, boolean suppressMissingSupport) {
        ClientOptions.enabled = enabled;
        ClientOptions.useServerExact = useServerExact;
        ClientOptions.v2ProtocolExtra = v2ProtocolExtra;
        ClientOptions.suppressMissingSupport = suppressMissingSupport;
    }
}
