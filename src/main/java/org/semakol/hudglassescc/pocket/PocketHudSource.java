package org.semakol.hudglassescc.pocket;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.semakol.hudglassescc.Config;
import org.semakol.hudglassescc.hud.HudBuffer;
import org.semakol.hudglassescc.hud.HudSource;
import org.semakol.hudglassescc.network.HudSettingsPayload;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link HudSource} backed by a pocket computer's HUD upgrade — the portable
 * counterpart of the HUD Modem block entity.
 *
 * <p>Lives for as long as the peripheral does (one pocket-computer session), so
 * unlike the block there is nothing to persist: the buffer and its size reset when
 * the computer reboots, and the script redraws. Only the source **id** is
 * persistent, and that lives on the item (see {@code PocketHudData}), not here.
 *
 * <p>Threading mirrors the block entity: Lua writes arrive on a CC computer thread
 * while location/viewers are published from the server main thread, so shared state
 * is volatile / immutable snapshots.
 */
public class PocketHudSource implements HudSource {
    private volatile HudBuffer buffer = new HudBuffer(Config.hudWidth, Config.hudHeight);
    private volatile boolean customSize = false;
    private volatile int sourceId = -1;
    private volatile List<String> currentViewers = List.of();

    private volatile byte ovTextShadow = HudSettingsPayload.AUTO;
    private volatile byte ovHudFit = HudSettingsPayload.AUTO;
    private volatile byte ovShadowLayer = HudSettingsPayload.AUTO;
    private final AtomicLong settingsVersion = new AtomicLong(1);

    /** Refreshed from the pocket computer's access each server tick. */
    @Nullable
    private volatile Level level;
    private volatile Vec3 position = Vec3.ZERO;

    // ---- main thread: refreshed each tick by the upgrade ----

    void updateLocation(@Nullable Level level, Vec3 position) {
        this.level = level;
        this.position = position;
    }

    void setSourceId(int id) {
        this.sourceId = id;
    }

    // ---- identity / location ----

    @Override
    public int getSourceId() {
        return sourceId;
    }

    @Override
    @Nullable
    public Level getSourceLevel() {
        return level;
    }

    @Override
    public Vec3 getSourcePosition() {
        return position;
    }

    // ---- buffer ----

    @Override
    public HudBuffer getBuffer() {
        return buffer;
    }

    @Override
    public void replaceBuffer(HudBuffer newBuffer) {
        this.buffer = newBuffer;
    }

    @Override
    public boolean hasCustomSize() {
        return customSize;
    }

    @Override
    public void setBufferSize(int w, int h) {
        w = Math.max(HudBuffer.MIN_W, Math.min(HudBuffer.MAX_W, w));
        h = Math.max(HudBuffer.MIN_H, Math.min(HudBuffer.MAX_H, h));
        HudBuffer cur = buffer;
        if (customSize && cur.width == w && cur.height == h) return;
        buffer = new HudBuffer(w, h);
        customSize = true;
    }

    @Override
    public void resetBufferSize() {
        int w = Config.hudWidth;
        int h = Config.hudHeight;
        HudBuffer cur = buffer;
        if (!customSize && cur.width == w && cur.height == h) return;
        buffer = new HudBuffer(w, h);
        customSize = false;
    }

    // ---- viewers ----

    @Override
    public List<String> getCurrentViewers() {
        return currentViewers;
    }

    @Override
    public void setCurrentViewers(List<String> viewers) {
        this.currentViewers = viewers.isEmpty() ? List.of() : List.copyOf(viewers);
    }

    // ---- display overrides ----

    @Override public byte getOverrideTextShadow() { return ovTextShadow; }
    @Override public byte getOverrideHudFit() { return ovHudFit; }
    @Override public byte getOverrideShadowLayer() { return ovShadowLayer; }

    @Override
    public void setOverrideTextShadow(int ordinal) {
        byte v = (byte) ordinal;
        if (ovTextShadow != v) { ovTextShadow = v; settingsVersion.incrementAndGet(); }
    }

    @Override
    public void setOverrideHudFit(int ordinal) {
        byte v = (byte) ordinal;
        if (ovHudFit != v) { ovHudFit = v; settingsVersion.incrementAndGet(); }
    }

    @Override
    public void setOverrideShadowLayer(int ordinal) {
        byte v = (byte) ordinal;
        if (ovShadowLayer != v) { ovShadowLayer = v; settingsVersion.incrementAndGet(); }
    }

    @Override
    public long getSettingsVersion() {
        return settingsVersion.get();
    }

    @Override
    public HudSettingsPayload buildSettingsPayload() {
        return new HudSettingsPayload(ovTextShadow, ovHudFit, ovShadowLayer);
    }
}
