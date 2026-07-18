package org.semakol.hudglassescc.pocket;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.network.PacketReceiver;
import dan200.computercraft.api.network.PacketSender;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.semakol.hudglassescc.peripheral.HudPeripheral;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HUD **and** ender modem on a single pocket upgrade.
 *
 * <p>A pocket computer has only one upgrade slot, so a HUD-only upgrade would leave
 * it with no networking — which is most of the point of a portable computer. This
 * peripheral is both at once:
 *
 * <ul>
 *   <li>{@link #getType()} is {@code "modem"} because {@code rednet} checks the
 *       <em>primary</em> type exactly ({@code peripheral.getType(m) ~= "modem"}), so
 *       {@code rednet.open("back")} works.</li>
 *   <li>{@code "hud_glasses"} is an <em>additional</em> type, and
 *       {@code peripheral.find} matches via {@code hasType} — so
 *       {@code peripheral.find("hud_glasses")} finds this same object.</li>
 * </ul>
 *
 * <p>The HUD half is inherited wholesale from {@link HudPeripheral} (CC picks up
 * {@code @LuaFunction} methods declared on a superclass — its own
 * {@code MonitorPeripheral extends TermMethods} relies on exactly this).
 *
 * <p>The modem half is an <b>ender</b> modem: interdimensional, so there's no range
 * math (CC computes wireless range from its own config, which isn't exposed to us).
 */
public class HudEnderModemPeripheral extends HudPeripheral implements PacketSender, PacketReceiver {
    private static final int MIN_CHANNEL = 0;
    private static final int MAX_CHANNEL = 65535;
    /** Mirrors CC's own per-modem limit. */
    private static final int MAX_OPEN_CHANNELS = 128;

    private final PocketHudSource source;
    private final MinecraftServer server;

    private final Set<Integer> open = ConcurrentHashMap.newKeySet();
    private final Set<IComputerAccess> computers = ConcurrentHashMap.newKeySet();
    private volatile boolean registered = false;

    public HudEnderModemPeripheral(PocketHudSource source, MinecraftServer server) {
        super(source);
        this.source = source;
        this.server = server;
    }

    @Override
    public String getType() {
        return "modem"; // rednet compares this exactly
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of("hud_glasses"); // peripheral.find() matches through hasType
    }

    private PacketNetwork network() {
        return ComputerCraftAPI.getWirelessNetwork(server);
    }

    // ---- network membership follows attach/detach, like CC's own modems ----

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
        if (!registered) {
            registered = true;
            network().addReceiver(this);
        }
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
        if (computers.isEmpty() && registered) {
            registered = false;
            network().removeReceiver(this);
        }
    }

    // ---- modem Lua surface ----

    private static int checkChannel(int channel) throws LuaException {
        if (channel < MIN_CHANNEL || channel > MAX_CHANNEL) {
            throw new LuaException("Expected number in range " + MIN_CHANNEL + "-" + MAX_CHANNEL);
        }
        return channel;
    }

    @LuaFunction
    public final void open(int channel) throws LuaException {
        int c = checkChannel(channel);
        if (!open.contains(c) && open.size() >= MAX_OPEN_CHANNELS) {
            throw new LuaException("Too many open channels");
        }
        open.add(c);
    }

    @LuaFunction
    public final boolean isOpen(int channel) throws LuaException {
        return open.contains(checkChannel(channel));
    }

    @LuaFunction
    public final void close(int channel) throws LuaException {
        open.remove(checkChannel(channel));
    }

    @LuaFunction
    public final void closeAll() {
        open.clear();
    }

    @LuaFunction
    public final boolean isWireless() {
        return true;
    }

    /** Main thread: transmitting touches the world's wireless network. */
    @LuaFunction(mainThread = true)
    public final void transmit(int channel, int replyChannel, Object payload) throws LuaException {
        checkChannel(channel);
        checkChannel(replyChannel);
        if (source.getSourceLevel() == null) return; // not placed in a level yet
        network().transmitInterdimensional(new Packet(channel, replyChannel, payload, this));
    }

    // ---- PacketSender / PacketReceiver ----

    @Override
    public Level getLevel() {
        Level level = source.getSourceLevel();
        // Seeded at creation and refreshed every tick, so this should never be null;
        // fall back rather than NPE inside the network.
        return level != null ? level : server.overworld();
    }

    @Override
    public Vec3 getPosition() {
        return source.getSourcePosition();
    }

    @Override
    public String getSenderID() {
        return "pocket_computer_" + source.getSourceId();
    }

    @Override
    public double getRange() {
        return Double.MAX_VALUE; // ignored while interdimensional
    }

    @Override
    public boolean isInterdimensional() {
        return true; // ender modem
    }

    @Override
    public void receiveSameDimension(Packet packet, double distance) {
        queue(packet, distance);
    }

    @Override
    public void receiveDifferentDimension(Packet packet) {
        queue(packet, null);
    }

    /** Fires {@code modem_message} on every attached computer that has the channel open. */
    private void queue(Packet packet, @Nullable Double distance) {
        if (!open.contains(packet.channel())) return;
        for (IComputerAccess computer : computers) {
            computer.queueEvent("modem_message",
                    computer.getAttachmentName(),
                    packet.channel(),
                    packet.replyChannel(),
                    packet.payload(),
                    distance);
        }
    }
}
