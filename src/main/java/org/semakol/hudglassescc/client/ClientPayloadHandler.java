package org.semakol.hudglassescc.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.semakol.hudglassescc.network.HudClearPayload;
import org.semakol.hudglassescc.network.HudUpdatePayload;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleHudUpdate(HudUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHudState.apply(payload));
    }

    public static void handleHudClear(HudClearPayload payload, IPayloadContext context) {
        context.enqueueWork(ClientHudState::clear);
    }
}
