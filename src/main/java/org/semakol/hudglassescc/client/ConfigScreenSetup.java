package org.semakol.hudglassescc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.semakol.hudglassescc.Hudglassescc;

/**
 * Wires the NeoForge built-in config UI to the "Config" button in the mod list,
 * and lets a keybind open the client section directly in-game. Client-only:
 * referenced only behind a {@code Dist.CLIENT} guard so the dedicated server
 * never loads these client classes.
 */
public final class ConfigScreenSetup {
    private ConfigScreenSetup() {}

    public static void register(ModContainer container) {
        // Mod-list "Config" button → the full screen (lets you pick Client / Common).
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * Opens the CLIENT config section directly (used by the keybind), skipping the
     * type-picker. Falls back to the full screen if the client config isn't found.
     */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        ModConfig clientConfig = findOurConfig(ModConfig.Type.CLIENT);
        if (clientConfig != null) {
            mc.setScreen(new ConfigurationScreen.ConfigurationSectionScreen(
                    mc.screen, ModConfig.Type.CLIENT, clientConfig,
                    Component.translatable("hudglassescc.config.client.title")));
            return;
        }
        ModList.get().getModContainerById(Hudglassescc.MODID).ifPresent(
                c -> mc.setScreen(new ConfigurationScreen(c, mc.screen)));
    }

    private static ModConfig findOurConfig(ModConfig.Type type) {
        for (ModConfig cfg : ModConfigs.getConfigSet(type)) {
            if (cfg.getModId().equals(Hudglassescc.MODID)) return cfg;
        }
        return null;
    }
}
