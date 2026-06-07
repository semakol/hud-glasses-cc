package org.semakol.hudglassescc.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.semakol.hudglassescc.Hudglassescc;

import java.util.List;

public class HudGlassesItem extends ArmorItem {
    public HudGlassesItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BoundModemData bound = stack.get(Hudglassescc.BOUND_MODEM.get());
        if (bound != null) {
            tooltip.add(Component.translatable("hudglassescc.item.hud_glasses.bound", bound.modemId())
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("hudglassescc.item.hud_glasses.not_bound")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("hudglassescc.item.hud_glasses.tooltip")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
