package org.semakol.hudglassescc.pocket;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.compat.CuriosCompat;
import org.semakol.hudglassescc.hud.HudBinding;
import org.semakol.hudglassescc.hud.HudManager;
import org.semakol.hudglassescc.hud.HudModemIdRegistry;
import org.semakol.hudglassescc.item.PocketHudData;
import org.semakol.hudglassescc.peripheral.HudPeripheral;

/**
 * Shared behaviour for every pocket upgrade that carries a HUD: keeping the
 * source alive for delivery, allocating/persisting its stable id, and binding the
 * holder's worn glasses.
 *
 * <p>Binding: a pocket computer is an item, so there's no block to right-click.
 * A plain right-click still opens the terminal; <b>sneak</b> + right-click binds
 * the holder's <b>worn</b> glasses (no need to take them off), mirroring the
 * block's empty-hand right-click.
 *
 * <p>Subclasses only differ in their type, crafting item and which peripheral they
 * expose (HUD only, or HUD + ender modem).
 */
public abstract class AbstractHudPocketUpgrade implements IPocketUpgrade {
    private final ResourceLocation id;

    protected AbstractHudPocketUpgrade(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public Component getAdjective() {
        return Component.translatable(UpgradeBase.getDefaultAdjective(id));
    }

    @Override
    public void update(IPocketAccess access, @Nullable IPeripheral peripheral) {
        PocketHudSource src = sourceOf(peripheral);
        if (src == null) return;

        // Main thread: safe to allocate the id and touch world state here.
        src.setSourceId(ensureId(access));
        src.updateLocation(access.getLevel(), access.getPosition());
        HudManager.tickPocketSource(src);
    }

    @Override
    public boolean onRightClick(Level world, IPocketAccess access, @Nullable IPeripheral peripheral) {
        if (world.isClientSide) return false;
        if (!(access.getEntity() instanceof Player player)) return false;
        // Plain right-click must keep opening the terminal — only sneak binds.
        if (!player.isCrouching()) return false;

        int sourceId = ensureId(access);
        ItemStack worn = CuriosCompat.getWornHudStack(player);
        if (worn.is(Hudglassescc.HUD_GLASSES.get())) {
            HudBinding.toggle(player, worn, sourceId);
        } else {
            player.displayClientMessage(Component.translatable("hudglassescc.pocket.no_glasses"), true);
        }
        return true; // consume, so the terminal doesn't open on sneak
    }

    @Nullable
    protected static PocketHudSource sourceOf(@Nullable IPeripheral peripheral) {
        return peripheral instanceof HudPeripheral hp && hp.source() instanceof PocketHudSource src ? src : null;
    }

    /**
     * Reads this pocket's stable source id, allocating and persisting one on first
     * use. Ids come from the same registry as block modems, so bindings resolve
     * uniquely. Main thread only.
     */
    protected static int ensureId(IPocketAccess access) {
        DataComponentPatch data = access.getUpgradeData();
        var existing = data.get(Hudglassescc.POCKET_HUD_ID.get());
        if (existing != null && existing.isPresent()) return existing.get().sourceId();

        int id = HudModemIdRegistry.get(access.getLevel().getServer()).allocateId();
        // Only this upgrade writes here, so a fresh patch is safe.
        access.setUpgradeData(DataComponentPatch.builder()
                .set(Hudglassescc.POCKET_HUD_ID.get(), new PocketHudData(id))
                .build());
        return id;
    }

    /** A source pre-seeded with the pocket's current location, so it's never null-located. */
    protected static PocketHudSource newSource(IPocketAccess access) {
        PocketHudSource src = new PocketHudSource();
        src.updateLocation(access.getLevel(), access.getPosition());
        return src;
    }
}
