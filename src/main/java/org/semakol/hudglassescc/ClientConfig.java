package org.semakol.hudglassescc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.semakol.hudglassescc.client.ClientHudState;
import org.semakol.hudglassescc.hud.HudDisplay;

/**
 * Client-only persisted state — the player's default HUD preferences (on/off
 * toggle, text shadow style/layer, screen fit). A modem can override the display
 * settings per-viewer; these values are the fallback when its override is AUTO.
 *
 * <p>The display enums live in {@link HudDisplay} so they are shared with the
 * server-side override path.
 *
 * <p>Lives at {@code .minecraft/config/hudglassescc-client.toml}.
 */
@EventBusSubscriber(modid = Hudglassescc.MODID, value = Dist.CLIENT)
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue HUD_ENABLED = BUILDER
            .comment("Whether the HUD overlay is rendered. Toggle with the configured key.")
            .translation("hudglassescc.config.hudEnabled")
            .define("hudEnabled", true);

    public static final ModConfigSpec.EnumValue<HudDisplay.TextShadowStyle> TEXT_SHADOW = BUILDER
            .comment(
                    "Drop shadow / outline behind HUD text. [EXPERIMENTAL]",
                    " NONE    — flat text (default)",
                    " SHADOW  — vanilla 1px drop shadow (cheap)",
                    " OUTLINE — black 4-direction outline (most readable, ~5x glyph draws)"
            )
            .translation("hudglassescc.config.textShadow")
            .defineEnum("textShadow", HudDisplay.TextShadowStyle.NONE);

    public static final ModConfigSpec.EnumValue<HudDisplay.ShadowLayer> TEXT_SHADOW_LAYER = BUILDER
            .comment(
                    "Where the shadow/outline sits relative to the cell backgrounds. [EXPERIMENTAL]",
                    "Only matters when textShadow is SHADOW or OUTLINE.",
                    " OVER_BACKGROUND  - background -> shadow -> text (shadow drawn over backgrounds, default)",
                    " UNDER_BACKGROUND - shadow -> background -> text (shadow drawn under backgrounds; only shows past opaque cells)"
            )
            .translation("hudglassescc.config.textShadowLayer")
            .defineEnum("textShadowLayer", HudDisplay.ShadowLayer.OVER_BACKGROUND);

    public static final ModConfigSpec.EnumValue<HudDisplay.HudFit> HUD_FIT = BUILDER
            .comment(
                    "How the HUD is scaled to the screen.",
                    " FIT     — keep aspect ratio, centered, letterboxed (default)",
                    " STRETCH — fill the whole screen, distorts if the ratio differs",
                    " COVER   — keep aspect ratio, scaled up to cover the screen, crops overflow"
            )
            .translation("hudglassescc.config.hudFit")
            .defineEnum("hudFit", HudDisplay.HudFit.FIT);

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        ClientHudState.setEnabled(HUD_ENABLED.get());
    }
}
