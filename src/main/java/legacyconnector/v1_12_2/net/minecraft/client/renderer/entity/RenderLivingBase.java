package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import java.nio.FloatBuffer;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GLAllocation;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.OpenGlHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.DynamicTexture;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122;

/**
 * 1.12.2 living-entity renderer: sets up the model space (position, body rotation and death tilt, the -1 flip and the 1.501 model
 * offset), feeds the model the interpolated animation values and draws it, then its layers. The red hurt tint 1.12 made with a texture
 * combiner ({@link #setBrightness}) is the modern overlay texture here, set by the modern renderer around the legacy one.
 */
public abstract class RenderLivingBase<T extends EntityLivingBase> extends Render<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DynamicTexture TEXTURE_BRIGHTNESS = null;
    protected ModelBase mainModel;
    protected FloatBuffer brightnessBuffer = GLAllocation.createDirectFloatBuffer(4);
    protected List<LayerRenderer<T>> layerRenderers = Lists.newArrayList();
    protected boolean renderMarker;

    public RenderLivingBase(RenderManager renderManagerIn, ModelBase modelBaseIn, float shadowSizeIn) {
        super(renderManagerIn);
        this.mainModel = modelBaseIn;
        this.shadowSize = shadowSizeIn;
    }

    public <V extends EntityLivingBase, U extends LayerRenderer<V>> boolean addLayer(U layer) {
        @SuppressWarnings("unchecked")
        LayerRenderer<T> cast = (LayerRenderer<T>) layer;
        return layerRenderers.add(cast);
    }

    public ModelBase getMainModel() {
        return mainModel;
    }

    /** The shortest turn from one angle to the next, interpolated. */
    protected float interpolateRotation(float prevYawOffset, float yawOffset, float partialTicks) {
        float f = yawOffset - prevYawOffset;
        while (f < -180.0F) f += 360.0F;
        while (f >= 180.0F) f -= 360.0F;
        return prevYawOffset + partialTicks * f;
    }

    public void transformHeldFull3DItemLayer() {
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (mainModel == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        mainModel.swingProgress = getSwingProgress(entity, partialTicks);
        Entity ridden = entity.getRidingEntity();
        boolean shouldSit = entity.isRiding() && ridden != null && ridden.shouldRiderSit();
        mainModel.isRiding = shouldSit;
        mainModel.isChild = entity.isChild();
        try {
            float bodyYaw = interpolateRotation(EntityLivingBase.mcc$get$prevRenderYawOffset(entity), EntityLivingBase.mcc$get$renderYawOffset(entity), partialTicks);
            float headYaw = interpolateRotation(EntityLivingBase.mcc$get$prevRotationYawHead(entity), EntityLivingBase.mcc$get$rotationYawHead(entity), partialTicks);
            float netHeadYaw = headYaw - bodyYaw;
            if (shouldSit && ridden instanceof EntityLivingBase mount) {
                bodyYaw = interpolateRotation(EntityLivingBase.mcc$get$prevRenderYawOffset(mount), EntityLivingBase.mcc$get$renderYawOffset(mount), partialTicks);
                netHeadYaw = headYaw - bodyYaw;
                float wrapped = MathHelper.wrapDegrees(netHeadYaw);
                if (wrapped < -85.0F) wrapped = -85.0F;
                if (wrapped >= 85.0F) wrapped = 85.0F;
                bodyYaw = headYaw - wrapped;
                if (wrapped * wrapped > 2500.0F) bodyYaw += wrapped * 0.2F;
                netHeadYaw = headYaw - bodyYaw;
            }
            float pitch = Entity.mcc$get$prevRotationPitch(entity) + (Entity.mcc$get$rotationPitch(entity) - Entity.mcc$get$prevRotationPitch(entity)) * partialTicks;
            renderLivingAt(entity, x, y, z);
            float age = handleRotationFloat(entity, partialTicks);
            applyRotations(entity, age, bodyYaw, partialTicks);
            float scale = prepareScale(entity, partialTicks);
            float swingAmount = 0.0F;
            float swing = 0.0F;
            if (!entity.isRiding()) {
                float prev = EntityLivingBase.mcc$get$prevLimbSwingAmount(entity);
                float amount = EntityLivingBase.mcc$get$limbSwingAmount(entity);
                swingAmount = prev + (amount - prev) * partialTicks;
                swing = EntityLivingBase.mcc$get$limbSwing(entity) - amount * (1.0F - partialTicks);
                if (entity.isChild()) swing *= 3.0F;
                if (swingAmount > 1.0F) swingAmount = 1.0F;
            }
            GlStateManager.enableAlpha();
            mainModel.setLivingAnimations(entity, swing, swingAmount, partialTicks);
            mainModel.setRotationAngles(swing, swingAmount, age, netHeadYaw, pitch, scale, entity);
            if (renderOutlines) {
                boolean team = setScoreTeamColor(entity);
                GlStateManager.enableColorMaterial();
                GlStateManager.enableOutlineMode(getTeamColor(entity));
                if (!renderMarker) renderModel(entity, swing, swingAmount, age, netHeadYaw, pitch, scale);
                if (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).isSpectator()) renderLayers(entity, swing, swingAmount, partialTicks, age, netHeadYaw, pitch, scale);
                GlStateManager.disableOutlineMode();
                GlStateManager.disableColorMaterial();
                if (team) unsetScoreTeamColor();
            } else {
                boolean bright = setDoRenderBrightness(entity, partialTicks);
                renderModel(entity, swing, swingAmount, age, netHeadYaw, pitch, scale);
                if (bright) unsetBrightness();
                GlStateManager.depthMask(true);
                if (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).isSpectator()) renderLayers(entity, swing, swingAmount, partialTicks, age, netHeadYaw, pitch, scale);
            }
            GlStateManager.disableRescaleNormal();
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("rendering the living entity " + entity.getClass().getName() + " with " + getClass().getName(), e);
        }
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /** Model space: the -1 flip, the renderer's own scaling, then up by the 24 model pixels (+0.001) the feet stand below the origin. */
    public float prepareScale(T entitylivingbaseIn, float partialTicks) {
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        preRenderCallback(entitylivingbaseIn, partialTicks);
        GlStateManager.translate(0.0F, -1.501F, 0.0F);
        return 0.0625F;
    }

    protected boolean setScoreTeamColor(T entityLivingBaseIn) {
        int color = 16777215;
        GlStateManager.disableLighting();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.color((float) (color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F, (float) (color & 255) / 255.0F, 1.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        return true;
    }

    protected void unsetScoreTeamColor() {
        GlStateManager.enableLighting();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    protected void renderModel(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
        boolean visible = isVisible(entitylivingbaseIn);
        var viewer = Minecraft.mcc$get$player(Minecraft.getMinecraft());
        boolean translucent = !visible && viewer != null && !entitylivingbaseIn.isInvisibleToPlayer(viewer);
        if (!visible && !translucent) return;
        if (!bindEntityTexture(entitylivingbaseIn)) return;
        if (translucent) GlStateManager.enableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
        mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        if (translucent) GlStateManager.disableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
    }

    protected boolean isVisible(T entitylivingbaseIn) {
        return !entitylivingbaseIn.isInvisible() || renderOutlines;
    }

    protected boolean setDoRenderBrightness(T entityLivingBaseIn, float partialTicks) {
        return setBrightness(entityLivingBaseIn, partialTicks, true);
    }

    /**
     * 1.12 tinted the model red while hurt and by {@link #getColorMultiplier} with a texture combiner. The hurt tint is the modern
     * overlay texture (set for the whole legacy draw); a colour multiplier is applied as the current colour.
     */
    protected boolean setBrightness(T entitylivingbaseIn, float partialTicks, boolean combineTextures) {
        float brightness = entitylivingbaseIn.getBrightness();
        int color = getColorMultiplier(entitylivingbaseIn, brightness, partialTicks);
        int alpha = color >> 24 & 255;
        if (alpha <= 0) return false;
        float a = alpha / 255.0F;
        // blend toward the multiplier's colour, as the combiner's interpolation did
        float r = 1.0F - a + a * (color >> 16 & 255) / 255.0F;
        float g = 1.0F - a + a * (color >> 8 & 255) / 255.0F;
        float b = 1.0F - a + a * (color & 255) / 255.0F;
        GlStateManager.color(r, g, b, 1.0F);
        return true;
    }

    protected void unsetBrightness() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected void renderLivingAt(T entityLivingBaseIn, double x, double y, double z) {
        GlStateManager.translate((float) x, (float) y, (float) z);
    }

    protected void applyRotations(T entityLiving, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
        int deathTime = EntityLivingBase.mcc$get$deathTime(entityLiving);
        if (deathTime > 0) {
            float f = ((float) deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) f = 1.0F;
            GlStateManager.rotate(f * getDeathMaxRotation(entityLiving), 0.0F, 0.0F, 1.0F);
        } else {
            String name = TextFormatting.getTextWithoutFormattingCodes(entityLiving.getName());
            if (name != null && ("Dinnerbone".equals(name) || "Grumm".equals(name))) {
                GlStateManager.translate(0.0F, Entity.mcc$get$height(entityLiving) + 0.1F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }

    protected float getSwingProgress(T livingBase, float partialTickTime) {
        return livingBase.getSwingProgress(partialTickTime);
    }

    protected float handleRotationFloat(T livingBase, float partialTicks) {
        return (float) Entity.mcc$get$ticksExisted(livingBase) + partialTicks;
    }

    protected void renderLayers(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitchIn, float scaleIn) {
        for (LayerRenderer<T> layer : layerRenderers) {
            boolean bright = setBrightness(entitylivingbaseIn, partialTicks, layer.shouldCombineTextures());
            try {
                layer.doRenderLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitchIn, scaleIn);
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("rendering the layer " + layer.getClass().getName(), e);
            }
            if (bright) unsetBrightness();
        }
    }

    protected float getDeathMaxRotation(T entityLivingBaseIn) {
        return 90.0F;
    }

    protected int getColorMultiplier(T entitylivingbaseIn, float lightBrightness, float partialTickTime) {
        return 0;
    }

    protected void preRenderCallback(T entitylivingbaseIn, float partialTickTime) {
    }

    @Override
    public void renderName(T entity, double x, double y, double z) {
        if (!canRenderName(entity)) return;
        double distance = renderManager.renderViewEntity == null ? 0.0D : entity.getDistanceSq(renderManager.renderViewEntity);
        float range = entity.isSneaking() ? 32.0F : 64.0F;
        if (distance >= (double) (range * range)) return;
        String name = entity.getDisplayName().getFormattedText();
        GlStateManager.alphaFunc(516, 0.1F);
        renderEntityName(entity, x, y, z, name, distance);
    }

    @Override
    protected boolean canRenderName(T entity) {
        var viewer = Minecraft.mcc$get$player(Minecraft.getMinecraft());
        if (viewer != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity living
                && viewer.mcc$entity instanceof net.minecraft.world.entity.player.Player player) {
            var team = living.getTeam();
            var own = player.getTeam();
            if (team != null) {
                switch (team.getNameTagVisibility()) {
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        if (own != null && !team.isAlliedTo(own)) return false;
                        break;
                    case HIDE_FOR_OWN_TEAM:
                        if (own != null && team.isAlliedTo(own)) return false;
                        break;
                    default:
                        break;
                }
            }
        }
        return Minecraft.isGuiEnabled() && entity != renderManager.renderViewEntity && (viewer == null || !entity.isInvisibleToPlayer(viewer))
                && !entity.isBeingRidden();
    }
}
