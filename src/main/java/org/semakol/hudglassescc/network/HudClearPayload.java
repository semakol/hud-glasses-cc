package org.semakol.hudglassescc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.semakol.hudglassescc.Hudglassescc;

public record HudClearPayload() implements CustomPacketPayload {
    public static final Type<HudClearPayload> TYPE = new Type<>(Hudglassescc.id("hud_clear"));

    public static final StreamCodec<FriendlyByteBuf, HudClearPayload> STREAM_CODEC =
            StreamCodec.unit(new HudClearPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
