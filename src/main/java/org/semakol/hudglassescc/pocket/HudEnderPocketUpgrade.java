package org.semakol.hudglassescc.pocket;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;
import net.minecraft.world.item.ItemStack;
import org.semakol.hudglassescc.Hudglassescc;

/**
 * HUD + ender modem in the pocket computer's single upgrade slot — the portable
 * setup that can both draw to the glasses and talk on the network.
 *
 * <p>Fitted like any CC pocket upgrade: craft the <b>HUD Ender Modem</b> item
 * (HUD Modem + Ender Modem), then combine it with a pocket computer — CC's own
 * upgrade recipe does that and preserves the computer's id and files. Its JEI/REI
 * entry comes for free: CC's {@code UpgradeRecipeGenerator} walks the upgrade
 * registry and uses {@link #getCraftingItem()}.
 *
 * <p>The dedicated item also keeps things unambiguous: CC picks an upgrade by
 * {@code isItemSuitable}, so reusing the plain HUD Modem would clash with
 * {@link HudGlassesPocketUpgrade}, and reusing the Ender Modem would clash with CC's
 * own. It's likewise what you get back when the upgrade is taken off, so no
 * ingredient is silently lost.
 *
 * <p>Server admins can disable the item's recipe with {@code allowPocketHudEnderCraft}.
 */
public class HudEnderPocketUpgrade extends AbstractHudPocketUpgrade {

    public HudEnderPocketUpgrade() {
        super(Hudglassescc.id("hud_ender"));
    }

    @Override
    public UpgradeType<? extends IPocketUpgrade> getType() {
        return Hudglassescc.HUD_ENDER_UPGRADE_TYPE.get();
    }

    @Override
    public ItemStack getCraftingItem() {
        return new ItemStack(Hudglassescc.HUD_ENDER_MODEM_ITEM.get());
    }

    @Override
    public IPeripheral createPeripheral(IPocketAccess access) {
        return new HudEnderModemPeripheral(newSource(access), access.getLevel().getServer());
    }
}
