package com.example.masaextra.protocol;

/**
 * Port of chenjunfu2/EarlyCompat {@code PlaceStateAccessor}.
 *
 * <p>Implemented by (mixin-)patched {@code BlockItem}. It records whether the
 * placement currently in flight came from the exact-placement protocol, plus a
 * bit field of per-placement flags that blocks can consult later in the
 * pipeline (for example "do not re-shape this rail").
 *
 * <p>The flags are static on the patched BlockItem, which is safe because a
 * placement runs entirely inside one server thread and is cleared on return.
 */
public interface PlaceStateAccessor {
    boolean isEasyPlaceState();

    long placeProperty();

    void placeProperty(long value);

    default boolean hasPlaceFlag(long flag) {
        return (placeProperty() & flag) == flag;
    }

    default void setPlaceFlag(long flag) {
        placeProperty(placeProperty() | flag);
    }

    long EASY_PLACE_RAIL_BLOCK_NO_SHAPE_UPDATE = 1L;
}
