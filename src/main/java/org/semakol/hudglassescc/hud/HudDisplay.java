package org.semakol.hudglassescc.hud;

/**
 * Display settings shared between the server side (per-modem overrides stored on
 * the block entity + network payload) and the client side ({@code ClientConfig}
 * defaults + {@code HudOverlay} rendering).
 *
 * <p>Kept dist-neutral — no client-only or CC imports — so it loads on both the
 * dedicated server and the client. A modem may override any of these per-viewer
 * via the peripheral; the override travels in {@code HudSettingsPayload} and the
 * client resolves an absent override ("AUTO") to the player's own config value.
 */
public final class HudDisplay {
    private HudDisplay() {}

    public enum TextShadowStyle {
        /** Flat text — fastest, can be hard to read on bright backgrounds. */
        NONE,
        /** Vanilla Minecraft 1-pixel bottom-right drop shadow. Cheap, usually enough. */
        SHADOW,
        /** Black 4-direction outline around each glyph. Most readable, ~5x glyph draws. */
        OUTLINE
    }

    public enum HudFit {
        /** Keep aspect ratio, centered. Letterbox bars where it doesn't match. */
        FIT,
        /** Stretch to fill the whole screen; X and Y scale independently (distorts). */
        STRETCH,
        /** Keep aspect ratio, scaled up to cover the screen; overflow is cropped. */
        COVER
    }

    public enum ShadowLayer {
        /** background &rarr; shadow &rarr; text: the shadow/outline is drawn over the cell backgrounds (default). */
        OVER_BACKGROUND,
        /** shadow &rarr; background &rarr; text: the shadow/outline is drawn under the cell backgrounds, so it only shows where it bleeds past an opaque cell. */
        UNDER_BACKGROUND
    }
}
