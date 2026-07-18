package org.semakol.hudglassescc.condition;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.semakol.hudglassescc.Config;

/**
 * Recipe condition tied to {@link Config#allowPocketHudEnderCraft}.
 *
 * <p>Attached to the HUD Ender Modem recipe, so turning the config off makes the
 * recipe vanish outright — it disappears from JEI/REI rather than sitting there
 * looking craftable. Since the item then has no recipe (and is hidden from the
 * creative tab), it effectively doesn't exist on that server.
 *
 * <p>Conditions are evaluated when recipes load, so changing the config needs a
 * {@code /reload} or restart to take effect.
 */
public final class HudEnderCraftCondition implements ICondition {
    public static final HudEnderCraftCondition INSTANCE = new HudEnderCraftCondition();
    public static final MapCodec<HudEnderCraftCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private HudEnderCraftCondition() {}

    @Override
    public boolean test(IContext context) {
        return Config.allowPocketHudEnderCraft;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "allowPocketHudEnderCraft";
    }
}
