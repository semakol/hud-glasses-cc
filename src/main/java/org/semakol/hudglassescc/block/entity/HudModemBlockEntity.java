package org.semakol.hudglassescc.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.semakol.hudglassescc.Config;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.hud.HudBuffer;
import org.semakol.hudglassescc.hud.HudManager;
import org.semakol.hudglassescc.hud.HudModemIdRegistry;
import org.semakol.hudglassescc.network.HudSettingsPayload;
import org.semakol.hudglassescc.peripheral.HudPeripheral;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Modem owns the terminal {@link HudBuffer}. Binding lives entirely on the
 * glasses item (a {@code BoundModemData} data component holding this modem's
 * {@link #getModemId() id}); the modem does not track players. Whoever wears
 * glasses bound to this id receives the buffer — so transferring the glasses
 * transfers the HUD. {@link HudManager} drives delivery and publishes the
 * current viewer names back here for the {@code getOwners()} Lua call.
 *
 * <p>Threading: CC:Tweaked runs {@code @LuaFunction} peripheral methods on a
 * computer thread, not the server main thread. {@code buffer}/{@code customSize}
 * are {@code volatile} (clean resize swap), {@code currentViewers} is published
 * as an immutable snapshot from the main thread, and {@code setChanged} for a
 * Lua resize is deferred to the main thread via {@link #flushPersistIfNeeded}.
 */
public class HudModemBlockEntity extends BlockEntity {
    public static final int MIN_W = 4, MAX_W = 256;
    public static final int MIN_H = 1, MAX_H = 128;

    private HudPeripheral peripheral;
    private volatile HudBuffer buffer = new HudBuffer(Config.hudWidth, Config.hudHeight);
    private int modemId = -1;
    /** True when Lua explicitly resized this modem — config reloads then leave it alone. */
    private volatile boolean customSize = false;
    /** Set on the peripheral thread when a resize needs persisting; consumed on the main thread. */
    private volatile boolean persistPending = false;
    /** Names of players currently viewing this modem. Published from the main thread tick. */
    private volatile List<String> currentViewers = List.of();

    /**
     * Per-modem display overrides: enum ordinal, or {@link HudSettingsPayload#AUTO}
     * ({@code -1}) meaning "defer to each viewer's client config". Written from the
     * peripheral (computer) thread, read on the main thread for delivery.
     */
    private volatile byte ovTextShadow = HudSettingsPayload.AUTO;
    private volatile byte ovHudFit = HudSettingsPayload.AUTO;
    private volatile byte ovShadowLayer = HudSettingsPayload.AUTO;
    /** Bumped on any override change so {@link HudManager} only re-sends on change. */
    private final AtomicLong settingsVersion = new AtomicLong(1);

    public HudModemBlockEntity(BlockPos pos, BlockState state) {
        super(Hudglassescc.HUD_MODEM_BE.get(), pos, state);
    }

    /**
     * World-stable identifier for this modem. Allocated lazily on the server main
     * thread and persisted via NBT — so the binding survives block moves into Sable
     * sub-levels (Create Aeronautics contraptions) where the BlockPos changes.
     * Eagerly allocated in {@link #onLoad}; the lazy branch only runs on the main
     * thread so a peripheral-thread {@code getId()} never mutates world state.
     */
    public int getModemId() {
        if (modemId <= 0 && level instanceof ServerLevel sl && sl.getServer().isSameThread()) {
            modemId = HudModemIdRegistry.get(sl.getServer()).allocateId();
            setChanged();
        }
        return modemId;
    }

    public HudBuffer getBuffer() {
        return buffer;
    }

    public void replaceBuffer(HudBuffer newBuffer) {
        this.buffer = newBuffer;
    }

    public boolean hasCustomSize() {
        return customSize;
    }

    /**
     * Lua-driven resize (peripheral thread). Swaps the volatile buffer so writes
     * issued right after {@code setSize} land on the new buffer. The new buffer is
     * born dirty, so the main-thread tick re-publishes it; persistence is deferred
     * to {@link #flushPersistIfNeeded}.
     */
    public void setBufferSize(int w, int h) {
        w = Math.max(MIN_W, Math.min(MAX_W, w));
        h = Math.max(MIN_H, Math.min(MAX_H, h));
        HudBuffer cur = buffer;
        if (customSize && cur.width == w && cur.height == h) return;
        buffer = new HudBuffer(w, h);
        customSize = true;
        persistPending = true;
    }

    /** Drops the Lua override and reverts to current config defaults (peripheral thread). */
    public void resetBufferSize() {
        int w = Config.hudWidth;
        int h = Config.hudHeight;
        HudBuffer cur = buffer;
        if (!customSize && cur.width == w && cur.height == h) return;
        buffer = new HudBuffer(w, h);
        customSize = false;
        persistPending = true;
    }

    /** Main-thread only: persist a deferred resize/override change. Called from {@link HudManager#tick}. */
    public void flushPersistIfNeeded() {
        if (persistPending) {
            persistPending = false;
            setChanged();
        }
    }

    // ---- Display overrides (peripheral thread writes; main thread reads for delivery) ----

    public byte getOverrideTextShadow() { return ovTextShadow; }
    public byte getOverrideHudFit() { return ovHudFit; }
    public byte getOverrideShadowLayer() { return ovShadowLayer; }

    /** @param ordinal {@code HudDisplay.TextShadowStyle} ordinal, or {@code -1} for AUTO. */
    public void setOverrideTextShadow(int ordinal) {
        byte v = (byte) ordinal;
        if (ovTextShadow != v) { ovTextShadow = v; onSettingsChanged(); }
    }

    /** @param ordinal {@code HudDisplay.HudFit} ordinal, or {@code -1} for AUTO. */
    public void setOverrideHudFit(int ordinal) {
        byte v = (byte) ordinal;
        if (ovHudFit != v) { ovHudFit = v; onSettingsChanged(); }
    }

    /** @param ordinal {@code HudDisplay.ShadowLayer} ordinal, or {@code -1} for AUTO. */
    public void setOverrideShadowLayer(int ordinal) {
        byte v = (byte) ordinal;
        if (ovShadowLayer != v) { ovShadowLayer = v; onSettingsChanged(); }
    }

    private void onSettingsChanged() {
        settingsVersion.incrementAndGet();
        persistPending = true;
    }

    /** Current override version — changes whenever any override is set. */
    public long getSettingsVersion() { return settingsVersion.get(); }

    /** Snapshot of the three overrides for delivery (main thread). */
    public HudSettingsPayload buildSettingsPayload() {
        return new HudSettingsPayload(ovTextShadow, ovHudFit, ovShadowLayer);
    }

    public HudPeripheral getPeripheral() {
        if (peripheral == null) peripheral = new HudPeripheral(this);
        return peripheral;
    }

    /** Main thread: record who is currently viewing (immutable snapshot publish). */
    public void setCurrentViewers(List<String> viewers) {
        this.currentViewers = viewers.isEmpty() ? List.of() : List.copyOf(viewers);
    }

    /** Safe to read from the peripheral thread — an immutable snapshot. */
    public List<String> getCurrentViewers() {
        return currentViewers;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            HudManager.register(this);
            // Allocate the ID eagerly on the main thread so peripheral methods
            // (called from the CC thread) never race a lazy allocation.
            getModemId();
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            HudManager.unregister(this);
        }
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(tag, lookupProvider);
        modemId = tag.contains("ModemId") ? tag.getInt("ModemId") : -1;
        customSize = tag.getBoolean("CustomSize");
        if (customSize && tag.contains("BufferW") && tag.contains("BufferH")) {
            int w = Math.max(MIN_W, Math.min(MAX_W, tag.getInt("BufferW")));
            int h = Math.max(MIN_H, Math.min(MAX_H, tag.getInt("BufferH")));
            if (buffer.width != w || buffer.height != h) {
                buffer = new HudBuffer(w, h);
            }
        }
        ovTextShadow = tag.contains("OvTextShadow") ? tag.getByte("OvTextShadow") : HudSettingsPayload.AUTO;
        ovHudFit = tag.contains("OvHudFit") ? tag.getByte("OvHudFit") : HudSettingsPayload.AUTO;
        ovShadowLayer = tag.contains("OvShadowLayer") ? tag.getByte("OvShadowLayer") : HudSettingsPayload.AUTO;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.saveAdditional(tag, lookupProvider);
        if (modemId > 0) tag.putInt("ModemId", modemId);
        if (customSize) {
            tag.putBoolean("CustomSize", true);
            HudBuffer b = buffer;
            tag.putInt("BufferW", b.width);
            tag.putInt("BufferH", b.height);
        }
        if (ovTextShadow != HudSettingsPayload.AUTO) tag.putByte("OvTextShadow", ovTextShadow);
        if (ovHudFit != HudSettingsPayload.AUTO) tag.putByte("OvHudFit", ovHudFit);
        if (ovShadowLayer != HudSettingsPayload.AUTO) tag.putByte("OvShadowLayer", ovShadowLayer);
    }
}
