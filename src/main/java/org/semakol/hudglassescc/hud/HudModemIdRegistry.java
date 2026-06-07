package org.semakol.hudglassescc.hud;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent counter for HUD-modem IDs. Lives on the overworld dimension storage.
 */
public class HudModemIdRegistry extends SavedData {
    public static final String NAME = "hudglassescc_modem_ids";

    private int nextId = 1;

    public synchronized int allocateId() {
        int id = nextId++;
        setDirty();
        return id;
    }

    public static HudModemIdRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(HudModemIdRegistry::new, HudModemIdRegistry::load, null),
                NAME
        );
    }

    public static HudModemIdRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        HudModemIdRegistry r = new HudModemIdRegistry();
        r.nextId = Math.max(1, tag.getInt("NextId"));
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("NextId", nextId);
        return tag;
    }
}
