package org.semakol.hudglassescc.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.semakol.hudglassescc.ClientConfig;
import org.semakol.hudglassescc.Hudglassescc;

@EventBusSubscriber(modid = Hudglassescc.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.hudglassescc.toggle_hud",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.hudglassescc");

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Hudglassescc.id("hud_glasses"), HudOverlay::render);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (TOGGLE_HUD.consumeClick()) {
            boolean enabled = ClientHudState.toggleEnabled();
            // Persist across sessions — written to the client TOML and auto-saved.
            ClientConfig.HUD_ENABLED.set(enabled);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.translatable(enabled
                                ? "hudglassescc.hud.enabled"
                                : "hudglassescc.hud.disabled"),
                        true);
            }
        }
    }
}
