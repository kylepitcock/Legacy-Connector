package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelSkeleton;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySkeleton;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** Skeleton renderer (also the base of mod skeleton variants): the wither skeleton type is drawn with its own texture, a fifth larger. */
public class RenderSkeleton extends RenderBiped {
    private static final ResourceLocation SKELETON = new ResourceLocation("textures/entity/skeleton/skeleton.png");
    private static final ResourceLocation WITHER = new ResourceLocation("textures/entity/skeleton/wither_skeleton.png");

    public RenderSkeleton() {
        super(new ModelSkeleton(), 0.5F);
    }

    @Override
    protected void func_82421_b() {
        field_82423_g = new ModelSkeleton(1.0F);
        field_82425_h = new ModelSkeleton(0.5F);
        modelArmourChestplate = field_82423_g;
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        if (entity instanceof EntitySkeleton skeleton && skeleton.getSkeletonType() == 1) RenderContext.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        return entity instanceof EntitySkeleton skeleton && skeleton.getSkeletonType() == 1 ? WITHER : SKELETON;
    }
}
