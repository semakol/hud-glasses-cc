package org.semakol.hudglassescc;

import com.mojang.logging.LogUtils;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.semakol.hudglassescc.block.HudModemBlock;
import org.semakol.hudglassescc.block.entity.HudModemBlockEntity;
import org.semakol.hudglassescc.compat.CuriosCompat;
import org.semakol.hudglassescc.hud.HudManager;
import org.semakol.hudglassescc.item.BoundModemData;
import org.semakol.hudglassescc.item.HudGlassesItem;
import org.semakol.hudglassescc.network.HudPayloads;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.List;

@Mod(Hudglassescc.MODID)
public class Hudglassescc {
    public static final String MODID = "hudglassescc";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** CC: Tweaked's creative tab — we add our items to it instead of making our own. */
    public static final ResourceKey<CreativeModeTab> CC_CREATIVE_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("computercraft", "tab"));

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoundModemData>> BOUND_MODEM =
            DATA_COMPONENTS.registerComponentType("bound_modem", builder -> builder
                    .persistent(BoundModemData.CODEC)
                    .networkSynchronized(BoundModemData.STREAM_CODEC));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HUD_GLASSES_MATERIAL =
            ARMOR_MATERIALS.register("hud_glasses", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                        for (ArmorItem.Type t : ArmorItem.Type.values()) m.put(t, 0);
                    }),
                    0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(id("hud_glasses"))),
                    0f,
                    0f));

    public static final DeferredItem<HudGlassesItem> HUD_GLASSES = ITEMS.register("hud_glasses",
            () -> new HudGlassesItem(HUD_GLASSES_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1)));

    public static final DeferredBlock<HudModemBlock> HUD_MODEM_BLOCK = BLOCKS.register("hud_modem",
            () -> new HudModemBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5f)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> HUD_MODEM_ITEM = ITEMS.registerSimpleBlockItem(HUD_MODEM_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HudModemBlockEntity>> HUD_MODEM_BE =
            BLOCK_ENTITIES.register("hud_modem", () -> BlockEntityType.Builder.of(
                    HudModemBlockEntity::new, HUD_MODEM_BLOCK.get()).build(null));

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public Hudglassescc(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);

        modEventBus.addListener(HudPayloads::register);
        modEventBus.addListener(Hudglassescc::onRegisterCapabilities);
        modEventBus.addListener(Hudglassescc::onBuildCreativeTabContents);
        modEventBus.addListener(CuriosCompat::onCommonSetup);
        modEventBus.addListener(CuriosCompat::onClientSetup);

        NeoForge.EVENT_BUS.addListener(Hudglassescc::onServerTick);
        NeoForge.EVENT_BUS.addListener(Hudglassescc::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(Hudglassescc::onServerStopped);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // Add the "Config" button to the mod list entry (client only).
        if (FMLEnvironment.dist == Dist.CLIENT) {
            org.semakol.hudglassescc.client.ConfigScreenSetup.register(modContainer);
        }
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                PeripheralCapability.get(),
                HUD_MODEM_BE.get(),
                (be, side) -> be.getPeripheral()
        );
        // Curios in 9.x for NeoForge no longer uses item capabilities — see
        // CuriosCompat.onCommonSetup for the new CuriosApi.registerCurio path.
    }

    private static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CC_CREATIVE_TAB)) {
            event.accept(HUD_GLASSES);
            event.accept(HUD_MODEM_ITEM);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        HudManager.tick(event.getServer());
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HudManager.onPlayerLeave(event.getEntity());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        HudManager.clearAll();
    }
}
