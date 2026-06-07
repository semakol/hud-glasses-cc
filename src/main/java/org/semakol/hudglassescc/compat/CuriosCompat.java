package org.semakol.hudglassescc.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.semakol.hudglassescc.Hudglassescc;

/**
 * Soft-dependency facade for the Curios API. All Curios-aware calls go through
 * here so the actual Curios classes ({@link CuriosHandler}) are only loaded
 * when the mod is installed.
 *
 * <p>Curios 9.x for NeoForge removed the stack-capability registration path.
 * The new flow is {@code CuriosApi.registerCurio(item, ICurioItem)} once, in
 * common setup — see {@link #onCommonSetup(FMLCommonSetupEvent)}.
 */
public final class CuriosCompat {
    public static final boolean LOADED = ModList.get().isLoaded("curios");

    private CuriosCompat() {}

    /**
     * Returns the HUD glasses {@link ItemStack} the player is currently wearing,
     * checking both the vanilla helmet slot and (if Curios is loaded) the Curios
     * "head" slot. Returns {@link ItemStack#EMPTY} if neither holds glasses.
     */
    public static ItemStack getWornHudStack(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(Hudglassescc.HUD_GLASSES.get())) return head;
        if (LOADED) {
            ItemStack curio = CuriosHandler.findHudGlasses(player);
            if (curio.is(Hudglassescc.HUD_GLASSES.get())) return curio;
        }
        return ItemStack.EMPTY;
    }

    /** Binds HUD Glasses as a Curio (head slot) if Curios is loaded. */
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        Hudglassescc.LOGGER.info("[curios] onCommonSetup, LOADED={}", LOADED);
        if (!LOADED) return;
        try {
            // Direct call (no enqueueWork). CuriosApi.registerCurio uses a
            // ConcurrentHashMap internally so it's safe from the parallel setup
            // thread, and registering here (instead of post-parallel) keeps the
            // binding visible during Curios' own setup polling.
            CuriosHandler.registerCurio();
            Hudglassescc.LOGGER.info("[curios] registered HUD Glasses as ICurioItem OK");
        } catch (Throwable t) {
            Hudglassescc.LOGGER.error("[curios] registerCurio FAILED", t);
        }
    }

    /**
     * Client-only: registers the renderer that draws the glasses on the player's
     * head model when worn in a Curios slot. Without this, the slot accepts the
     * item but the player looks bare-headed because vanilla HumanoidArmorLayer
     * only renders items in EquipmentSlot.HEAD.
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        Hudglassescc.LOGGER.info("[curios] onClientSetup, LOADED={}", LOADED);
        if (!LOADED) return;
        try {
            CuriosHandler.registerRenderer();
            Hudglassescc.LOGGER.info("[curios] registered HUD Glasses ICurioRenderer OK");
        } catch (Throwable t) {
            Hudglassescc.LOGGER.error("[curios] registerRenderer FAILED", t);
        }
    }
}
