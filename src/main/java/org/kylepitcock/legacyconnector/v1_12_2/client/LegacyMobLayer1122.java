package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderLivingBase;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerRenderer;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaEntities1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Runs the layers 1.12.2 mods added to a vanilla renderer ({@code renderManager.entityRenderMap.get(EntityWolf.class).addLayer(...)}: Wolf
 * Armor and Storage's armour and backpack on wolves) on the modern renderer of that vanilla mob, in the same model space as a 1.12 layer
 * (after the body rotation, the -1 flip and the 1.501 offset). Mobs whose vanilla renderer got no layers cost one list check per frame.
 */
public class LegacyMobLayer1122<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final String legacyName;
    private RenderLivingBase<?> legacy;
    private boolean resolved;

    public LegacyMobLayer1122(RenderLayerParent<T, M> parent, String legacyName) {
        super(parent);
        this.legacyName = legacyName;
    }

    /** The port of the vanilla renderer mods add layers to (made with the manager), or null. */
    private RenderLivingBase<?> legacy() {
        if (!resolved) {
            resolved = true;
            Class<?> cls = VanillaEntities1122.mcc$legacyClass(legacyName);
            if (cls != null && RenderManager.mcc$instance().mcc$vanillaExact(cls) instanceof RenderLivingBase<?> r) legacy = r;
        }
        return legacy;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        RenderLivingBase renderer = legacy();
        if (renderer == null) return;
        var layers = renderer.mcc$addedLayers();
        if (layers.isEmpty()) return;
        if (!(EntityBridge1122.wrap(entity) instanceof EntityLivingBase view)) return;
        LegacyEntityRenderer1122.updateView(RenderManager.mcc$instance(), partialTicks);
        var model = renderer.getMainModel();
        if (model != null) {
            model.isChild = entity.isBaby();
            model.isRiding = entity.isPassenger();
            model.swingProgress = entity.getAttackAnim(partialTicks);
        }
        RenderContext.beginWorld(pose, buffers, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F));
        try {
            for (Object o : new java.util.ArrayList<>(layers)) {
                LayerRenderer layer = (LayerRenderer) o;
                try {
                    legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.mcc$reset();
                    layer.doRenderLayer(view, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, 0.0625F);
                } catch (RuntimeException | LinkageError e) {
                    RenderErrors1122.report("running the layer " + layer.getClass().getName() + " on " + legacyName, e);
                }
            }
        } finally {
            legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.mcc$reset();
            RenderContext.resetState();
            RenderContext.end();
        }
    }

    /** Adds the layer to the modern renderer of every vanilla mob that has a 1.12 class. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void addTo(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER) continue;
            String name = VanillaEntities1122.mcc$legacyNameOf(type);
            if (name == null) continue;
            try {
                var r = event.getRenderer((EntityType) type);
                if (r instanceof LivingEntityRenderer living) living.addLayer(new LegacyMobLayer1122(living, name));
            } catch (RuntimeException | LinkageError e) {
                // a renderer that is not a living one, or a type without a renderer
            }
        }
    }
}
