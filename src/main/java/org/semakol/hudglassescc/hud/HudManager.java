package org.semakol.hudglassescc.hud;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.semakol.hudglassescc.Config;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.compat.CuriosCompat;
import org.semakol.hudglassescc.item.BoundModemData;
import org.semakol.hudglassescc.network.HudClearPayload;
import org.semakol.hudglassescc.network.HudUpdatePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Item-driven HUD delivery. Binding lives on the glasses stack
 * ({@link BoundModemData}); whoever wears glasses bound to a modem receives that
 * modem's buffer, so transferring the glasses transfers the HUD. Each tick we
 * scan online players, resolve their worn-glasses binding to a modem, and push
 * the buffer when its {@linkplain HudBuffer#getVersion() version} changed since
 * the last frame that player received.
 */
public final class HudManager {
    /** Block-modem sources: registered/unregistered explicitly on load/unload. */
    private static final Set<HudSource> ACTIVE = ConcurrentHashMap.newKeySet();

    /**
     * Pocket sources, refreshed every tick by the HUD pocket upgrade. A pocket
     * computer has no "unloaded" callback — it just stops ticking when it's turned
     * off or leaves a player's inventory — so entries carry the tick they were last
     * seen and are pruned once stale.
     */
    private static final Map<HudSource, Long> POCKET = new ConcurrentHashMap<>();
    private static final long POCKET_STALE_TICKS = 5;

    /** Bumped once per server tick; pocket sources are stamped with it. */
    private static final AtomicLong GAME_TICK = new AtomicLong();

    private static volatile boolean resizePending = false;

    /** What we last delivered to each player, so we only resend on change. Main thread. */
    private record Delivery(int modemId, long version, long settingsVersion) {}
    private static final Map<UUID, Delivery> LAST = new HashMap<>();

    private HudManager() {}

    public static void register(HudSource source) {
        ACTIVE.add(source);
    }

    public static void unregister(HudSource source) {
        ACTIVE.remove(source);
    }

    /**
     * Keep a pocket source alive for this tick. Called from the HUD pocket
     * upgrade's per-tick update; stops being called when the pocket computer
     * turns off, and the entry is then pruned in {@link #tick}.
     */
    public static void tickPocketSource(HudSource source) {
        POCKET.put(source, GAME_TICK.get());
    }

    /** Drop all tracked state — called on server stop so nothing leaks across worlds. */
    public static void clearAll() {
        ACTIVE.clear();
        POCKET.clear();
        LAST.clear();
        resizePending = false;
    }

    public static void requestResize() {
        resizePending = true;
    }

    public static void clearFor(ServerPlayer player) {
        LAST.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new HudClearPayload());
    }

    public static void tick(MinecraftServer server) {
        long now = GAME_TICK.incrementAndGet();

        if (resizePending) {
            resizePending = false;
            applyResize();
        }

        // Persist deferred Lua resizes + index sources by id.
        Map<Integer, HudSource> byId = new HashMap<>();
        for (HudSource src : ACTIVE) {
            src.flushPersistIfNeeded();
            byId.put(src.getSourceId(), src);
        }
        // Live pocket sources: drop any that stopped ticking (computer off / put away).
        POCKET.entrySet().removeIf(e -> now - e.getValue() > POCKET_STALE_TICKS);
        for (HudSource src : POCKET.keySet()) {
            int id = src.getSourceId();
            if (id > 0) byId.put(id, src);
        }

        int maxRange = Config.maxRangeBlocks;
        double maxRangeSq = maxRange < 0 ? -1.0 : (double) maxRange * maxRange;

        // Viewer names per modem, rebuilt each tick from who's actually watching.
        Map<Integer, List<String>> viewersByModem = new HashMap<>();
        Set<UUID> seen = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            seen.add(uuid);

            ItemStack worn = CuriosCompat.getWornHudStack(player);
            if (!worn.is(Hudglassescc.HUD_GLASSES.get())) {
                if (LAST.remove(uuid) != null) {
                    PacketDistributor.sendToPlayer(player, new HudClearPayload());
                }
                continue;
            }
            BoundModemData binding = worn.get(Hudglassescc.BOUND_MODEM.get());
            if (binding == null) {
                if (LAST.remove(uuid) != null) {
                    PacketDistributor.sendToPlayer(player, new HudClearPayload());
                }
                continue;
            }
            HudSource src = byId.get(binding.modemId());
            if (src == null) {
                // Bound source isn't loaded right now. Keep the last frame frozen;
                // don't clear (it may come back). But it's not an active viewer.
                continue;
            }
            Level level = src.getSourceLevel();
            if (level == null) continue;

            if (maxRangeSq >= 0) {
                // Sable-aware distance: handles sources inside Create Aeronautics
                // sub-levels; falls back to plain Euclidean without Sable installed.
                Vec3 pos = src.getSourcePosition();
                double dSq = SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                        level,
                        pos.x, pos.y, pos.z,
                        player.getX(), player.getY(), player.getZ());
                if (dSq > maxRangeSq) continue; // out of range — frozen, not a viewer
            }

            // Active viewer.
            viewersByModem.computeIfAbsent(binding.modemId(), k -> new ArrayList<>())
                    .add(player.getName().getString());

            HudBuffer buf = src.getBuffer();
            long ver = buf.getVersion();
            long sver = src.getSettingsVersion();
            Delivery prev = LAST.get(uuid);
            boolean modemChanged = prev == null || prev.modemId() != binding.modemId();
            boolean bufChanged = modemChanged || prev.version() != ver;
            boolean settingsChanged = modemChanged || prev.settingsVersion() != sver;
            if (bufChanged) {
                PacketDistributor.sendToPlayer(player, buf.toPayload());
            }
            if (settingsChanged) {
                PacketDistributor.sendToPlayer(player, src.buildSettingsPayload());
            }
            if (bufChanged || settingsChanged) {
                LAST.put(uuid, new Delivery(binding.modemId(), ver, sver));
            }
        }

        // Forget players who logged off.
        LAST.keySet().retainAll(seen);

        // Publish viewer lists so getOwners() reflects current wearers.
        for (HudSource src : ACTIVE) publishViewers(src, viewersByModem);
        for (HudSource src : POCKET.keySet()) publishViewers(src, viewersByModem);
    }

    private static void publishViewers(HudSource src, Map<Integer, List<String>> viewersByModem) {
        List<String> v = viewersByModem.get(src.getSourceId());
        src.setCurrentViewers(v == null ? List.of() : v);
    }

    private static void applyResize() {
        int w = Config.hudWidth;
        int h = Config.hudHeight;
        for (HudSource src : ACTIVE) {
            if (src.hasCustomSize()) continue; // Lua override wins
            HudBuffer existing = src.getBuffer();
            if (existing.width == w && existing.height == h) continue;
            src.replaceBuffer(new HudBuffer(w, h));
        }
    }

    public static void onPlayerLeave(Player player) {
        if (player instanceof ServerPlayer sp) {
            LAST.remove(sp.getUUID());
        }
    }
}
