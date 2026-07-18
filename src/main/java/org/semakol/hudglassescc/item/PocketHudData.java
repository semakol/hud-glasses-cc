package org.semakol.hudglassescc.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The stable HUD source id of a pocket computer's HUD upgrade.
 *
 * <p>Stored in the upgrade's {@code DataComponentPatch} (see
 * {@code IPocketAccess#setUpgradeData}) — in 1.21.1 pocket upgrades persist data
 * as data components, not NBT. Ids come from the same
 * {@code HudModemIdRegistry} as block modems, so a glasses binding resolves to
 * exactly one source.
 */
public record PocketHudData(int sourceId) {

    public static final Codec<PocketHudData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("source_id").forGetter(PocketHudData::sourceId)
    ).apply(inst, PocketHudData::new));

    public static final StreamCodec<ByteBuf, PocketHudData> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(PocketHudData::new, PocketHudData::sourceId);
}
