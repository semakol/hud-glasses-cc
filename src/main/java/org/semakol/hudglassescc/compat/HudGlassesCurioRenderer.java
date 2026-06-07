package org.semakol.hudglassescc.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.semakol.hudglassescc.Hudglassescc;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Draws the HUD Glasses overlay on the player's head when they're worn in
 * a Curios head slot. Mirrors what the vanilla {@code HumanoidArmorLayer}
 * would have done for a real helmet — reuses the same armor texture and the
 * outer-armor head ModelPart.
 *
 * <p>Loaded only when Curios is installed (Curios + client classes are
 * required). Registration happens in {@link CuriosCompat#onClientSetup}.
 */
public class HudGlassesCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Hudglassescc.MODID, "textures/models/armor/hud_glasses_layer_1.png");

    private final HumanoidModel<LivingEntity> armorModel;

    public HudGlassesCurioRenderer() {
        this.armorModel = new HumanoidModel<>(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack matrixStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {

        // The parent model is whatever the entity uses (PlayerModel, ZombieModel, etc.).
        // For our glasses we only care about its head — copy its pose so the glyph
        // follows head rotation correctly.
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> parent)) return;
        armorModel.head.copyFrom(parent.head);

        VertexConsumer vc = buffers.getBuffer(RenderType.armorCutoutNoCull(TEXTURE));
        armorModel.head.render(matrixStack, vc, light, OverlayTexture.NO_OVERLAY);
    }
}
