package org.semakol.hudglassescc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.semakol.hudglassescc.client.ClientHudState;

/**
 * Client-only persisted state. HUD on/off toggle + text shadow style.
 *
 * Lives at {@code .minecraft/config/hudglassescc-client.toml}.
 */
@EventBusSubscriber(modid = Hudglassescc.MODID, value = Dist.CLIENT)
public class ClientConfig {
    public enum TextShadowStyle {
        /** Flat text — fastest, can be hard to read on bright backgrounds. */
        NONE,
        /** Vanilla Minecraft 1-pixel bottom-right drop shadow. Cheap, usually enough. */
        SHADOW,
        /** Black 4-direction outline around each glyph. Most readable, ~5x glyph draws. */
        OUTLINE
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue HUD_ENABLED = BUILDER
            .comment("Whether the HUD overlay is rendered. Toggle with the configured key.")
            .define("hudEnabled", true);

    public static final ModConfigSpec.EnumValue<TextShadowStyle> TEXT_SHADOW = BUILDER
            .comment(
                    "Drop shadow / outline behind HUD text. [EXPERIMENTAL]",
                    " NONE    — flat text (default)",
                    " SHADOW  — vanilla 1px drop shadow (cheap)",
                    " OUTLINE — black 4-direction outline (most readable, ~5x glyph draws)"
            )
            .defineEnum("textShadow", TextShadowStyle.NONE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        ClientHudState.setEnabled(HUD_ENABLED.get());
    }
}
