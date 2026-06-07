package org.semakol.hudglassescc.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.semakol.hudglassescc.Hudglassescc;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Actual Curios API call site. This class is only ever referenced from
 * {@link CuriosCompat} under a {@code CuriosCompat.LOADED} guard, so the JVM
 * won't try to resolve the {@code top.theillusivec4.*} symbols when Curios
 * isn't installed.
 *
 * <p>Note: Curios 9.x for NeoForge removed the {@code CuriosCapability.ITEM}
 * stack-capability path. The replacement is {@link CuriosApi#registerCurio}
 * binding an {@link ICurioItem} to an Item, called once during common setup.
 */
final class CuriosHandler {
    private static final ICurioItem HUD_GLASSES_CURIO = new ICurioItem() {
        /**
         * HUD Glasses is registered as an {@link net.minecraft.world.item.ArmorItem}
         * with {@link net.minecraft.world.entity.EquipmentSlot#HEAD}. Curios'
         * default {@code canEquip} refuses to put real armor into a Curios slot
         * to avoid double-equipping. For us that's the wrong call — we want the
         * player to be able to wear HUD glasses either in the vanilla helmet
         * slot OR in the Curios head slot, not both at once. Override to always
         * allow Curios placement; the wear-state check inside the HUD pipeline
         * (which finds whichever slot has them) handles the "not both" rule
         * naturally.
         */
        @Override
        public boolean canEquip(SlotContext slotContext, ItemStack stack) {
            return true;
        }
    };

    private CuriosHandler() {}

    static ItemStack findHudGlasses(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.findFirstCurio(Hudglassescc.HUD_GLASSES.get()))
                .map(slot -> slot.stack())
                .orElse(ItemStack.EMPTY);
    }

    static void registerCurio() {
        CuriosApi.registerCurio(Hudglassescc.HUD_GLASSES.get(), HUD_GLASSES_CURIO);
    }

    /**
     * Client-only: register the renderer that draws the glasses on the player's
     * head when worn in a Curios slot. Must be called from FMLClientSetupEvent.
     */
    static void registerRenderer() {
        CuriosRendererRegistry.register(Hudglassescc.HUD_GLASSES.get(), HudGlassesCurioRenderer::new);
    }
}
