package org.semakol.hudglassescc.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BoundModemData(int modemId) {

    public static final Codec<BoundModemData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("modem_id").forGetter(BoundModemData::modemId)
    ).apply(inst, BoundModemData::new));

    public static final StreamCodec<ByteBuf, BoundModemData> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(BoundModemData::new, BoundModemData::modemId);
}
