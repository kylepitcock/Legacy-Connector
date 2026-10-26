package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_7_10.net.minecraft.item.ItemArmor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyItemAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.slf4j.LoggerFactory;

/**
 * Draws legacy armor: 1.7.10 armor is a texture on a ModelBiped (the item's own {@code getArmorModel} when it has one), which modern
 * armor rendering knows nothing about because legacy armor items are not modern armor items.
 */
public class LegacyArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] PREFIX = {"leather", "chainmail", "iron", "diamond", "gold"};
    private static ModelBiped outer;
    private static ModelBiped inner;
    private static int failures;

    public LegacyArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    /**
     * 1.7.10 skins had no outer layer, so legacy armor was made to cover only the body: where a player wears it, the skin's
     * outer layer (hat, jacket and sleeves, trousers) is hidden instead of poking through it (Fisk's Heroes' suits).
     */
    public static void registerOverlayHiding() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre e) -> {
            var player = e.getEntity();
            var model = e.getRenderer().getModel();
            if (legacyArmor(player, EquipmentSlot.HEAD)) model.hat.visible = false;
            if (legacyArmor(player, EquipmentSlot.CHEST)) {
                model.jacket.visible = false;
                model.leftSleeve.visible = false;
                model.rightSleeve.visible = false;
            }
            if (legacyArmor(player, EquipmentSlot.LEGS)) {
                model.leftPants.visible = false;
                model.rightPants.visible = false;
            }
        });
    }

    private static boolean legacyArmor(LivingEntity entity, EquipmentSlot slot) {
        var stack = entity.getItemBySlot(slot);
        return !stack.isEmpty() && stack.getItem() instanceof LegacyItemAdapter adapter && adapter.legacyItem() instanceof ItemArmor;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (failures > 20) return;
        for (int slot = 0; slot < 4; slot++) {
            var stack = entity.getItemBySlot(SLOTS[slot]);
            if (stack.isEmpty() || !(stack.getItem() instanceof LegacyItemAdapter adapter) || !(adapter.legacyItem() instanceof ItemArmor armor) || armor.armorType != slot) continue;
            if (!ClientEvents1710.armorPiece(entity, slot, stack, partialTick)) continue;
            RenderContext.beginWorld(pose, buffers, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F));
            try {
                var legacyEntity = EntityBridge.wrap(entity);
                var legacyStack = legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack);
                ModelBiped model = armor.getArmorModel((legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase) legacyEntity, legacyStack, slot);
                if (model == null) {
                    if (outer == null) {
                        outer = new ModelBiped(1.0F);
                        inner = new ModelBiped(0.5F);
                    }
                    model = slot == 2 ? inner : outer;
                }
                String path = armor.getArmorTexture(legacyStack, legacyEntity, slot, null);
                ResourceLocation texture = path != null ? textureOf(path) : defaultTexture(armor, slot);
                model.bipedHead.showModel = slot == 0;
                model.bipedHeadwear.showModel = slot == 0;
                model.bipedBody.showModel = slot == 1 || slot == 2;
                model.bipedRightArm.showModel = slot == 1;
                model.bipedLeftArm.showModel = slot == 1;
                model.bipedRightLeg.showModel = slot == 2 || slot == 3;
                model.bipedLeftLeg.showModel = slot == 2 || slot == 3;
                model.isSneak = entity.isCrouching();
                model.isRiding = entity.isPassenger();
                model.isChild = entity.isBaby();
                model.onGround = entity.getAttackAnim(partialTick);
                model.heldItemRight = entity.getMainHandItem().isEmpty() ? 0 : 1;
                RenderContext.bindTexture(texture);
                RenderContext.setArmorLayering(true);
                model.render(legacyEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F);
            } catch (RuntimeException | LinkageError e) {
                if (failures++ < 3) LoggerFactory.getLogger("LegacyConnector/Render").error("Legacy armor {} failed to render", armor.getClass().getName(), e);
            } finally {
                RenderContext.setArmorLayering(false);
                RenderContext.end();
            }
        }
    }

    /**
     * The vanilla head layer draws any non-armor item in the head slot as a floating 3D item; legacy armor is drawn by this layer instead,
     * so the head layer is replaced by one that skips it.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void hideFromHeadLayer(LivingEntityRenderer<?, ?> renderer) {
        try {
            var field = LivingEntityRenderer.class.getDeclaredField("layers");
            field.setAccessible(true);
            java.util.List<RenderLayer<?, ?>> layers = (java.util.List<RenderLayer<?, ?>>) field.get(renderer);
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i) instanceof net.minecraft.client.renderer.entity.layers.CustomHeadLayer head) {
                    layers.set(i, new SkippingHeadLayer(head, (RenderLayerParent) renderer));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Render").warn("Could not adjust the head layer of {}: {}", renderer.getClass().getName(), e.toString());
        }
    }

    private static final class SkippingHeadLayer<T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> extends RenderLayer<T, M> {
        private final RenderLayer<T, M> inner;

        SkippingHeadLayer(RenderLayer<T, M> inner, RenderLayerParent<T, M> parent) {
            super(parent);
            this.inner = inner;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                float netHeadYaw, float headPitch) {
            if (entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof LegacyItemAdapter a && a.legacyItem() instanceof ItemArmor) return;
            inner.render(pose, buffers, light, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        }
    }

    private static ResourceLocation defaultTexture(ItemArmor armor, int slot) {
        String prefix = armor.renderIndex >= 0 && armor.renderIndex < PREFIX.length ? PREFIX[armor.renderIndex] : "iron";
        return ResourceLocation.withDefaultNamespace("textures/models/armor/" + prefix + "_layer_" + (slot == 2 ? 2 : 1) + ".png");
    }

    private static ResourceLocation textureOf(String path) {
        int colon = path.indexOf(':');
        return colon < 0 ? TextureLocations.modern("minecraft", path) : TextureLocations.modern(path.substring(0, colon), path.substring(colon + 1));
    }
}
