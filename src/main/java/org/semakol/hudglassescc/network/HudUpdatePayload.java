package org.semakol.hudglassescc.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.semakol.hudglassescc.Hudglassescc;

public record HudUpdatePayload(
        int width,
        int height,
        String[] lines,
        String[] fgColors,
        String[] bgColors,
        int[] palette
) implements CustomPacketPayload {

    public static final Type<HudUpdatePayload> TYPE = new Type<>(Hudglassescc.id("hud_update"));

    // Hard caps that mirror the server-side buffer limits. A malformed or hostile
    // packet can't make the client allocate giant arrays.
    private static final int MAX_W = 256;
    private static final int MAX_H = 128;

    public static final StreamCodec<FriendlyByteBuf, HudUpdatePayload> STREAM_CODEC =
            new StreamCodec<FriendlyByteBuf, HudUpdatePayload>() {
                @Override
                public HudUpdatePayload decode(FriendlyByteBuf buf) {
                    int w = buf.readVarInt();
                    int h = buf.readVarInt();
                    if (w < 1 || w > MAX_W || h < 1 || h > MAX_H) {
                        throw new DecoderException("HUD payload size out of bounds: " + w + "x" + h);
                    }
                    String[] l = new String[h];
                    String[] fg = new String[h];
                    String[] bg = new String[h];
                    for (int i = 0; i < h; i++) {
                        l[i] = buf.readUtf(w * 4);
                        fg[i] = buf.readUtf(w * 4);
                        bg[i] = buf.readUtf(w * 4);
                    }
                    int[] palette = new int[16];
                    for (int i = 0; i < 16; i++) palette[i] = buf.readInt();
                    return new HudUpdatePayload(w, h, l, fg, bg, palette);
                }

                @Override
                public void encode(FriendlyByteBuf buf, HudUpdatePayload v) {
                    buf.writeVarInt(v.width);
                    buf.writeVarInt(v.height);
                    for (int i = 0; i < v.height; i++) {
                        buf.writeUtf(v.lines[i], v.width * 4);
                        buf.writeUtf(v.fgColors[i], v.width * 4);
                        buf.writeUtf(v.bgColors[i], v.width * 4);
                    }
                    for (int i = 0; i < 16; i++) buf.writeInt(v.palette[i]);
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
