package org.semakol.hudglassescc.client;

import org.semakol.hudglassescc.network.HudUpdatePayload;

public final class ClientHudState {
    private static volatile HudUpdatePayload current;
    private static volatile boolean enabled = true;

    private ClientHudState() {}

    public static void apply(HudUpdatePayload payload) {
        current = payload;
    }

    public static void clear() {
        current = null;
    }

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
