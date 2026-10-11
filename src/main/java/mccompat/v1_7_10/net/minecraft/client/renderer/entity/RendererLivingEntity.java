package mccompat.v1_7_10.net.minecraft.client.renderer.entity;

import mccompat.v1_7_10.net.minecraft.client.model.ModelBase;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.mccompat.v1_7_10.client.RenderContext;

/**
 * Legacy living-entity renderer: sets up the 1.7.10 model space (body rotation, the -1 scale flip, the 1.5 block
 * model offset), feeds the model the interpolated animation values and renders it plus its render passes.
 */
public abstract class RendererLivingEntity extends Render {
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
