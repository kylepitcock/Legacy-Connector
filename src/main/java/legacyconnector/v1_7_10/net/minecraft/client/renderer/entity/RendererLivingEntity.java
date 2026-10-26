package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Legacy living-entity renderer: sets up the 1.7.10 model space (body rotation, the -1 scale flip, the 1.5 block
 * model offset), feeds the model the interpolated animation values and renders it plus its render passes.
 */
public abstract class RendererLivingEntity extends Render {
    /** Forge 1.7.10: how far name tags show (mods hide or extend them). */
    public static float NAME_TAG_RANGE = 64.0F;
    public static float NAME_TAG_RANGE_SNEAK = 32.0F;
    private static final java.util.Set<String> WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    protected ModelBase mainModel;
    protected ModelBase renderPassModel;

    public RendererLivingEntity(ModelBase model, float shadowSize) {
        this.mainModel = model;
        this.shadowSize = shadowSize;
    }

    public void setRenderPassModel(ModelBase model) {
        this.renderPassModel = model;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        doRender((EntityLivingBase) entity, x, y, z, yaw, partialTicks);
    }

    public void doRender(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTicks) {
        LivingEntity m = entity.mcc$entity instanceof LivingEntity l ? l : null;
        if (m == null) return;
        if (mainModel == null) {
            // A vanilla renderer the shim has no model for (a mod extends it): nothing can be drawn, but the game goes on.
            if (WARNED.add(getClass().getName())) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Render").warn("The entity renderer {} has no model (its vanilla base class is not ported)", getClass().getName());
            return;
        }
        RenderContext.push();
        try {
            mainModel.onGround = renderSwingProgress(entity, partialTicks);
            if (renderPassModel != null) renderPassModel.onGround = mainModel.onGround;
            mainModel.isRiding = m.isPassenger();
            mainModel.isChild = m.isBaby();
            if (renderPassModel != null) {
                renderPassModel.isRiding = mainModel.isRiding;
                renderPassModel.isChild = mainModel.isChild;
            }
            float bodyYaw = Mth.rotLerp(partialTicks, m.yBodyRotO, m.yBodyRot);
            float headYaw = Mth.rotLerp(partialTicks, m.yHeadRotO, m.yHeadRot);
            float pitch = Mth.lerp(partialTicks, m.xRotO, m.getXRot());
            renderLivingAt(entity, x, y, z);
            float age = handleRotationFloat(entity, partialTicks);
            rotateCorpse(entity, age, bodyYaw, partialTicks);
            float scale = 0.0625F;
            RenderContext.scale(-1.0F, -1.0F, 1.0F);
            preRenderCallback(entity, partialTicks);
            RenderContext.translate(0.0F, -24.0F * scale - 0.0078125F, 0.0F);
            float swingAmount = m.walkAnimation.speed(partialTicks);
            float swing = m.walkAnimation.position(partialTicks);
            if (m.isBaby()) swing *= 3.0F;
            if (swingAmount > 1.0F) swingAmount = 1.0F;
            mainModel.setLivingAnimations(entity, swing, swingAmount, partialTicks);
            renderModel(entity, swing, swingAmount, age, headYaw - bodyYaw, pitch, scale);
            for (int pass = 0; pass < 4; pass++) {
                int result = shouldRenderPass(entity, pass, partialTicks);
                if (result > 0 && renderPassModel != null) {
                    renderPassModel.setLivingAnimations(entity, swing, swingAmount, partialTicks);
                    renderPassModel.render(entity, swing, swingAmount, age, headYaw - bodyYaw, pitch, scale);
                }
            }
            renderEquippedItems(entity, partialTicks);
        } finally {
            RenderContext.pop();
        }
        passSpecialRender(entity, x, y, z);
    }

    protected void renderModel(EntityLivingBase entity, float swing, float swingAmount, float age, float headYaw, float pitch, float scale) {
        bindEntityTexture(entity);
        if (!entity.isInvisible()) mainModel.render(entity, swing, swingAmount, age, headYaw, pitch, scale);
    }

    protected void renderLivingAt(EntityLivingBase entity, double x, double y, double z) {
        RenderContext.translate(x, y, z);
    }

    /** Draws a name tag above the entity, offset (scoreboard lines under player names). */
    protected void renderOffsetLivingLabel(EntityLivingBase entity, double x, double y, double z, String text, float scale, double distance) {
        renderLivingLabel(entity, text, x, y, z, 64);
    }

    protected void rotateCorpse(EntityLivingBase entity, float age, float bodyYaw, float partialTicks) {
        RenderContext.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
        int deathTime = entity.mcc$entity instanceof LivingEntity l ? l.deathTime : 0;
        if (deathTime > 0) {
            float f = (deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = Math.min(1.0F, (float) Math.sqrt(f));
            RenderContext.rotate(f * getDeathMaxRotation(entity), 0.0F, 0.0F, 1.0F);
        }
    }

    protected float renderSwingProgress(EntityLivingBase entity, float partialTicks) {
        return entity.mcc$entity instanceof LivingEntity l ? l.getAttackAnim(partialTicks) : 0.0F;
    }

    protected float handleRotationFloat(EntityLivingBase entity, float partialTicks) {
        return (entity.mcc$entity == null ? 0 : entity.mcc$entity.tickCount) + partialTicks;
    }

    protected void renderEquippedItems(EntityLivingBase entity, float partialTicks) {
    }

    /**
     * Draws the arrows stuck in the entity (players call it from their equipped items): each at a random point of a random
     * box of the main model, pointing out of it (the same points every frame, seeded by the entity id).
     */
    protected void renderArrowsStuckInEntity(EntityLivingBase entity, float partialTicks) {
        int count = entity.getArrowCountInEntity();
        if (count <= 0 || mainModel == null || mainModel.boxList.isEmpty()) return;
        java.util.Random random = new java.util.Random(entity.getEntityId());
        bindTexture(RenderArrow.arrowTextures);
        for (int i = 0; i < count; ++i) {
            RenderContext.push();
            try {
                legacyconnector.v1_7_10.net.minecraft.client.model.ModelRenderer part = mainModel.getRandomModelBox(random);
                if (part.cubeList.isEmpty()) continue;
                var box = (legacyconnector.v1_7_10.net.minecraft.client.model.ModelBox) part.cubeList.get(random.nextInt(part.cubeList.size()));
                part.postRender(0.0625F);
                float fx = random.nextFloat(), fy = random.nextFloat(), fz = random.nextFloat();
                RenderContext.translate((box.posX1 + (box.posX2 - box.posX1) * fx) / 16.0F, (box.posY1 + (box.posY2 - box.posY1) * fy) / 16.0F,
                        (box.posZ1 + (box.posZ2 - box.posZ1) * fz) / 16.0F);
                fx = -(fx * 2.0F - 1.0F);
                fy = -(fy * 2.0F - 1.0F);
                fz = -(fz * 2.0F - 1.0F);
                float horizontal = (float) Math.sqrt(fx * fx + fz * fz);
                float yaw = (float) (Math.atan2(fx, fz) * 180.0D / Math.PI);
                float pitch = (float) (Math.atan2(fy, horizontal) * 180.0D / Math.PI);
                // as RenderArrow places an arrow with these angles at the box point
                RenderContext.rotate(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
                RenderContext.rotate(pitch, 0.0F, 0.0F, 1.0F);
                RenderArrow.mcc$drawArrow();
            } finally {
                RenderContext.pop();
            }
        }
    }

    protected int inheritRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return shouldRenderPass(entity, pass, partialTicks);
    }

    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return -1;
    }

    protected void func_82408_c(EntityLivingBase entity, int pass, float partialTicks) {
    }

    protected float getDeathMaxRotation(EntityLivingBase entity) {
        return 90.0F;
    }

    protected int getColorMultiplier(EntityLivingBase entity, float brightness, float partialTicks) {
        return 0;
    }

    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
    }

    /** Name tags are drawn by the modern renderer. */
    protected void passSpecialRender(EntityLivingBase entity, double x, double y, double z) {
    }
}
