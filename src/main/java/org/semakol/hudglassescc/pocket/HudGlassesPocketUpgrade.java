package org.semakol.hudglassescc.pocket;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;
import net.minecraft.world.item.ItemStack;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.peripheral.HudPeripheral;

/**
 * Gives a pocket computer the same {@code hud_glasses} peripheral the HUD Modem
 * block has — a portable HUD source you carry instead of mounting on a wall.
 *
 * <p>Display only. It occupies the pocket computer's single upgrade slot, so this
 * variant leaves no room for a modem; see {@link HudEnderPocketUpgrade} for the
 * combined HUD + ender modem.
 */
public class HudGlassesPocketUpgrade extends AbstractHudPocketUpgrade {

    public HudGlassesPocketUpgrade() {
        super(Hudglassescc.id("hud_glasses"));
    }

    @Override
    public UpgradeType<? extends IPocketUpgrade> getType() {
        return Hudglassescc.HUD_POCKET_UPGRADE_TYPE.get();
    }

    @Override
    public ItemStack getCraftingItem() {
        // Resolved lazily: "put a HUD Modem into your pocket computer".
        return new ItemStack(Hudglassescc.HUD_MODEM_ITEM.get());
    }

    @Override
    public IPeripheral createPeripheral(IPocketAccess access) {
        // The source (and its buffer) lives as long as this peripheral does, i.e.
        // one pocket-computer session. Only the id is persisted, on the item.
        return new HudPeripheral(newSource(access));
    }
}
