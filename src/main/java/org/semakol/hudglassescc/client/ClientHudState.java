package org.semakol.hudglassescc.client;

import org.semakol.hudglassescc.hud.HudDisplay;
import org.semakol.hudglassescc.network.HudUpdatePayload;

public final class ClientHudState {
    private static volatile HudUpdatePayload current;
    private static volatile boolean enabled = true;

    // Per-modem display overrides pushed by the bound modem. null = AUTO: fall
    // back to the player's own ClientConfig value. These are transient client
    // state and are never written to the config file.
    private static volatile HudDisplay.TextShadowStyle ovShadow;
    private static volatile HudDisplay.HudFit ovFit;
    private static volatile HudDisplay.ShadowLayer ovLayer;

    private ClientHudState() {}

    public static void apply(HudUpdatePayload payload) {
        current = payload;
    }

    public static void clear() {
        current = null;
        // Unbinding / switching modems must not leave a stale forced setting behind.
        ovShadow = null;
        ovFit = null;
        ovLayer = null;
    }

    public static void applySettings(byte textShadow, byte hudFit, byte shadowLayer) {
        ovShadow = decode(HudDisplay.TextShadowStyle.values(), textShadow);
        ovFit = decode(HudDisplay.HudFit.values(), hudFit);
        ovLayer = decode(HudDisplay.ShadowLayer.values(), shadowLayer);
    }

    /** Byte (enum ordinal, or -1/out-of-range = AUTO) → enum value or null. */
    private static <E extends Enum<E>> E decode(E[] values, byte b) {
        return (b < 0 || b >= values.length) ? null : values[b];
    }

    /** Modem-forced text shadow style, or {@code null} to use the player's config. */
    public static HudDisplay.TextShadowStyle shadowOverride() { return ovShadow; }

    /** Modem-forced screen fit, or {@code null} to use the player's config. */
    public static HudDisplay.HudFit fitOverride() { return ovFit; }

    /** Modem-forced shadow layer, or {@code null} to use the player's config. */
    public static HudDisplay.ShadowLayer layerOverride() { return ovLayer; }

    public static HudUpdatePayload get() {
        return current;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean v) {
        enabled = v;
    }

    public static boolean toggleEnabled() {
        enabled = !enabled;
        return enabled;
    }
}
