package org.semakol.hudglassescc.hud;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.semakol.hudglassescc.Config;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.block.entity.HudModemBlockEntity;
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

/**
 * Item-driven HUD delivery. Binding lives on the glasses stack
 * ({@link BoundModemData}); whoever wears glasses bound to a modem receives that
 * modem's buffer, so transferring the glasses transfers the HUD. Each tick we
 * scan online players, resolve their worn-glasses binding to a modem, and push
 * the buffer when its {@linkplain HudBuffer#getVersion() version} changed since
 * the last frame that player received.
 */
public final class HudManager {
    private static final Set<HudModemBlockEntity> ACTIVE = ConcurrentHashMap.newKeySet();
    private static volatile boolean resizePending = false;

    /** What we last delivered to each player, so we only resend on change. Main thread. */
    private record Delivery(int modemId, long version, long settingsVersion) {}
    private static final Map<UUID, Delivery> LAST = new HashMap<>();

    private HudManager() {}

    public static void register(HudModemBlockEntity be) {
        ACTIVE.add(be);
    }

    public static void unregister(HudModemBlockEntity be) {
        ACTIVE.remove(be);
    }

    /** Drop all tracked state — called on server stop so nothing leaks across worlds. */
    public static void clearAll() {
        ACTIVE.clear();
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
        if (resizePending) {
            resizePending = false;
            applyResize();
        }

        // Persist deferred Lua resizes + index modems by id.
        Map<Integer, HudModemBlockEntity> byId = new HashMap<>();
        for (HudModemBlockEntity be : ACTIVE) {
            be.flushPersistIfNeeded();
            byId.put(be.getModemId(), be);
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
            HudModemBlockEntity be = byId.get(binding.modemId());
            if (be == null) {
                // Bound modem isn't loaded right now. Keep the last frame frozen;
                // don't clear (it may come back). But it's not an active viewer.
                continue;
            }
            Level level = be.getLevel();
            if (level == null) continue;

            if (maxRangeSq >= 0) {
                // Sable-aware distance: handles modems inside Create Aeronautics
                // sub-levels; falls back to plain Euclidean without Sable installed.
                BlockPos pos = be.getBlockPos();
                double dSq = SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                        level,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        player.getX(), player.getY(), player.getZ());
                if (dSq > maxRangeSq) continue; // out of range — frozen, not a viewer
            }

            // Active viewer.
            viewersByModem.computeIfAbsent(binding.modemId(), k -> new ArrayList<>())
                    .add(player.getName().getString());

            HudBuffer buf = be.getBuffer();
            long ver = buf.getVersion();
            long sver = be.getSettingsVersion();
            Delivery prev = LAST.get(uuid);
            boolean modemChanged = prev == null || prev.modemId() != binding.modemId();
            boolean bufChanged = modemChanged || prev.version() != ver;
            boolean settingsChanged = modemChanged || prev.settingsVersion() != sver;
            if (bufChanged) {
                PacketDistributor.sendToPlayer(player, buf.toPayload());
            }
            if (settingsChanged) {
                PacketDistributor.sendToPlayer(player, be.buildSettingsPayload());
            }
            if (bufChanged || settingsChanged) {
                LAST.put(uuid, new Delivery(binding.modemId(), ver, sver));
            }
        }

        // Forget players who logged off.
        LAST.keySet().retainAll(seen);

        // Publish viewer lists so getOwners() reflects current wearers.
        for (HudModemBlockEntity be : ACTIVE) {
            List<String> v = viewersByModem.get(be.getModemId());
            be.setCurrentViewers(v == null ? List.of() : v);
        }
    }

    private static void applyResize() {
        int w = Config.hudWidth;
        int h = Config.hudHeight;
        for (HudModemBlockEntity be : ACTIVE) {
            if (be.hasCustomSize()) continue; // Lua override wins
            HudBuffer existing = be.getBuffer();
            if (existing.width == w && existing.height == h) continue;
            be.replaceBuffer(new HudBuffer(w, h));
        }
    }

    public static void onPlayerLeave(Player player) {
        if (player instanceof ServerPlayer sp) {
            LAST.remove(sp.getUUID());
        }
    }
}
