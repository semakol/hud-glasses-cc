package org.semakol.hudglassescc.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.item.BoundModemData;

/**
 * Binding lives on the glasses item: a {@link BoundModemData} holding the id of a
 * {@link HudSource}. Shared by every way of binding — right-clicking a HUD Modem
 * block, or sneak-right-clicking a pocket computer with the HUD upgrade.
 */
public final class HudBinding {
    private HudBinding() {}

    /**
     * Toggles {@code sourceId} on the given glasses stack: binds if not already
     * bound to it, unbinds if it is. Server side only.
     */
    public static void toggle(Player player, ItemStack glasses, int sourceId) {
        BoundModemData existing = glasses.get(Hudglassescc.BOUND_MODEM.get());
        if (existing != null && existing.modemId() == sourceId) {
            glasses.remove(Hudglassescc.BOUND_MODEM.get());
            player.displayClientMessage(Component.translatable("hudglassescc.modem.unbound"), true);
        } else {
            glasses.set(Hudglassescc.BOUND_MODEM.get(), new BoundModemData(sourceId));
            player.displayClientMessage(Component.translatable("hudglassescc.modem.bound", sourceId), true);
        }
    }
}
