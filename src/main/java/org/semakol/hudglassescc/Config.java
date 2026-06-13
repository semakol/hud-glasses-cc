package org.semakol.hudglassescc;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.semakol.hudglassescc.hud.HudManager;

@EventBusSubscriber(modid = Hudglassescc.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue HUD_WIDTH = BUILDER
            .comment("HUD width in characters. Cells stretch to fill the screen, so fewer = bigger text.")
            .translation("hudglassescc.config.hudWidth")
            .defineInRange("hudWidth", 80, 4, 256);

    private static final ModConfigSpec.IntValue HUD_HEIGHT = BUILDER
            .comment("HUD height in characters.")
            .translation("hudglassescc.config.hudHeight")
            .defineInRange("hudHeight", 28, 1, 128);

    private static final ModConfigSpec.IntValue MAX_RANGE = BUILDER
            .comment("Maximum distance in blocks between modem and bound player for the HUD to update.",
                    "Set to -1 for unlimited (default). Player must also be in the same dimension.")
            .translation("hudglassescc.config.maxRangeBlocks")
            .defineInRange("maxRangeBlocks", -1, -1, 100000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int hudWidth = 80;
    public static int hudHeight = 28;
    public static int maxRangeBlocks = -1;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        hudWidth = HUD_WIDTH.get();
        hudHeight = HUD_HEIGHT.get();
        maxRangeBlocks = MAX_RANGE.get();
        HudManager.requestResize();
    }
}
