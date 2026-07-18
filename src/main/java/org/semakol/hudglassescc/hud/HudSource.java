package org.semakol.hudglassescc.hud;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.semakol.hudglassescc.network.HudSettingsPayload;

import java.util.List;

/**
 * Something that owns a {@link HudBuffer} and can be bound to by glasses.
 *
 * <p>Two implementations: the HUD Modem block entity (stationary) and the pocket
 * computer's HUD upgrade (portable). Both draw their id from the same
 * {@link HudModemIdRegistry}, so a glasses binding resolves to exactly one source.
 * {@code HudPeripheral} uses the peripheral-facing half; {@link HudManager} uses
 * the delivery-facing half.
 *
 * <p>Threading: peripheral methods run on a CC computer thread while delivery runs
 * on the server main thread, so implementations must publish shared state safely
 * (volatile buffer, immutable viewer snapshots, atomic versions).
 */
public interface HudSource {

    // ---- peripheral-facing (CC computer thread) ----

    HudBuffer getBuffer();

    void replaceBuffer(HudBuffer buffer);

    /** True when Lua explicitly resized this source — config reloads leave it alone. */
    boolean hasCustomSize();

    void setBufferSize(int width, int height);

    /** Drops the Lua size override, reverting to the config defaults. */
    void resetBufferSize();

    /** Stable id that glasses bind to. */
    int getSourceId();

    /** Immutable snapshot — safe to read from the CC thread. */
    List<String> getCurrentViewers();

    byte getOverrideTextShadow();

    byte getOverrideHudFit();

    byte getOverrideShadowLayer();

    void setOverrideTextShadow(int ordinal);

    void setOverrideHudFit(int ordinal);

    void setOverrideShadowLayer(int ordinal);

    // ---- delivery-facing (server main thread) ----

    /** Publish who is currently viewing this source. */
    void setCurrentViewers(List<String> viewers);

    long getSettingsVersion();

    HudSettingsPayload buildSettingsPayload();

    /** Level the source currently lives in, or {@code null} if it isn't in one. */
    @Nullable
    Level getSourceLevel();

    /** Where the source is right now — used for the range check. */
    Vec3 getSourcePosition();

    /** Persist deferred changes. No-op for sources that have nothing to persist. */
    default void flushPersistIfNeeded() {}
}
