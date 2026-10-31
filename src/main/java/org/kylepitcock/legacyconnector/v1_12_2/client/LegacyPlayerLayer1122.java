package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelPlayer;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderPlayer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Runs the layers 1.12.2 mods added to the player renderers ({@code RenderManager.getSkinMap()}) on the modern player renderer. A
 * modern layer is drawn in the same model space as a 1.12 layer (after the body rotation, the -1 flip and the 1.501 offset); the legacy
 * player model is posed like the modern one first, so layers that attach to its parts ({@code bipedBody.postRender}) follow the body.
 */
public class LegacyPlayerLayer1122 extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final boolean slim;

    public LegacyPlayerLayer1122(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        RenderPlayer renderer = RenderManager.mcc$instance().getSkinMap().get(slim ? "slim" : "default");
        if (renderer == null) return;
        var layers = new java.util.ArrayList<>(renderer.mcc$modLayers());
        var armor = renderer.mcc$armorLayer();
        if (armor != null && hasLegacyArmor(player)) layers.add(0, armor);
        if (layers.isEmpty()) return;
        if (!(EntityBridge1122.wrap(player) instanceof legacyconnector.v1_12_2.net.minecraft.client.entity.AbstractClientPlayer legacy)) return;
        LegacyEntityRenderer1122.updateView(RenderManager.mcc$instance(), partialTicks);
        pose(renderer.getMainModel(), getParentModel(), player);
        RenderContext.beginWorld(pose, buffers, light, LivingEntityRenderer.getOverlayCoords(player, 0.0F));
        try {
            for (LayerRenderer<legacyconnector.v1_12_2.net.minecraft.client.entity.AbstractClientPlayer> layer : layers) {
                try {
                    legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.mcc$reset();
                    layer.doRenderLayer(legacy, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, 0.0625F);
                } catch (RuntimeException | LinkageError e) {
                    RenderErrors1122.report("running the player layer " + layer.getClass().getName(), e);
                }
            }
        } finally {
            legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.mcc$reset();
            RenderContext.resetState();
            RenderContext.end();
        }
    }

    /** Whether the player wears an armour piece of a legacy mod (vanilla pieces are drawn by the modern renderer). */
    private static boolean hasLegacyArmor(AbstractClientPlayer player) {
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() != net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            var stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && !(stack.getItem() instanceof net.minecraft.world.item.ArmorItem)) return true;
        }
        return false;
    }

    /** Copies the modern model's part placement and flags to the legacy one. */
    private static void pose(ModelPlayer legacy, PlayerModel<AbstractClientPlayer> modern, AbstractClientPlayer player) {
        copy(modern.head, legacy.bipedHead);
        copy(modern.hat, legacy.bipedHeadwear);
        copy(modern.body, legacy.bipedBody);
        copy(modern.rightArm, legacy.bipedRightArm);
        copy(modern.leftArm, legacy.bipedLeftArm);
        copy(modern.rightLeg, legacy.bipedRightLeg);
        copy(modern.leftLeg, legacy.bipedLeftLeg);
        copy(modern.jacket, legacy.bipedBodyWear);
        copy(modern.rightSleeve, legacy.bipedRightArmwear);
        copy(modern.leftSleeve, legacy.bipedLeftArmwear);
        copy(modern.rightPants, legacy.bipedRightLegwear);
        copy(modern.leftPants, legacy.bipedLeftLegwear);
        legacy.isSneak = modern.crouching;
        legacy.isChild = modern.young;
        legacy.isRiding = modern.riding;
        legacy.swingProgress = modern.attackTime;
    }

    private static void copy(ModelPart from, ModelRenderer to) {
        if (from == null || to == null) return;
        to.rotationPointX = from.x;
        to.rotationPointY = from.y;
        to.rotationPointZ = from.z;
        to.rotateAngleX = from.xRot;
        to.rotateAngleY = from.yRot;
        to.rotateAngleZ = from.zRot;
    }

    /** Adds the layer to both modern player renderers. */
    @SuppressWarnings("unchecked")
    static void addTo(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var r = event.getSkin(skin);
            if (r instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer player) {
                player.addLayer(new LegacyPlayerLayer1122(player, skin == net.minecraft.client.resources.PlayerSkin.Model.SLIM));
            }
        }
    }
}
