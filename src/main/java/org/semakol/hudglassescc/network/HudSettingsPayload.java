package org.semakol.hudglassescc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.hud.HudDisplay;

/**
 * Per-modem display overrides pushed to a viewer. Each field is an enum ordinal,
 * or {@code -1} meaning AUTO (the client falls back to the player's own config).
 *
 * <p>Sent independently of {@link HudUpdatePayload} — only when the modem's
 * settings actually change — so a settings tweak never re-ships the whole buffer.
 */
public record HudSettingsPayload(byte textShadow, byte hudFit, byte shadowLayer)
        implements CustomPacketPayload {

    /** AUTO: no override, defer to the viewer's client config. */
    public static final byte AUTO = -1;

    public static final Type<HudSettingsPayload> TYPE = new Type<>(Hudglassescc.id("hud_settings"));

    public static final StreamCodec<FriendlyByteBuf, HudSettingsPayload> STREAM_CODEC =
            new StreamCodec<FriendlyByteBuf, HudSettingsPayload>() {
                @Override
                public HudSettingsPayload decode(FriendlyByteBuf buf) {
                    byte s = clamp(buf.readByte(), HudDisplay.TextShadowStyle.values().length);
                    byte f = clamp(buf.readByte(), HudDisplay.HudFit.values().length);
                    byte l = clamp(buf.readByte(), HudDisplay.ShadowLayer.values().length);
                    return new HudSettingsPayload(s, f, l);
                }

                @Override
                public void encode(FriendlyByteBuf buf, HudSettingsPayload v) {
                    buf.writeByte(v.textShadow);
                    buf.writeByte(v.hudFit);
                    buf.writeByte(v.shadowLayer);
                }
            };

    /** A malformed ordinal (negative besides AUTO, or past the enum) becomes AUTO. */
    private static byte clamp(byte v, int len) {
        return (v >= 0 && v < len) ? v : AUTO;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
