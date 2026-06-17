package org.semakol.hudglassescc.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.semakol.hudglassescc.client.ClientPayloadHandler;

public final class HudPayloads {
    private HudPayloads() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                HudUpdatePayload.TYPE,
                HudUpdatePayload.STREAM_CODEC,
                ClientPayloadHandler::handleHudUpdate
        );
        registrar.playToClient(
                HudClearPayload.TYPE,
                HudClearPayload.STREAM_CODEC,
                ClientPayloadHandler::handleHudClear
        );
        registrar.playToClient(
                HudSettingsPayload.TYPE,
                HudSettingsPayload.STREAM_CODEC,
                ClientPayloadHandler::handleHudSettings
        );
    }
}
