package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelWither;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.boss.BossStatus;
import legacyconnector.v1_7_10.net.minecraft.entity.boss.EntityWither;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's wither renderer (mods extend it for their own withers): twice the model size, growing while invulnerable, the
 * blinking invulnerable texture, the boss bar, and the glowing armour layer below half health. The armour layer's
 * scrolling texture matrix has no core-profile equivalent; it is drawn unscrolled.
 */
public class RenderWither extends RenderLiving {
    private static final ResourceLocation field_110913_a = new ResourceLocation("textures/entity/wither/wither_invulnerable.png");
    private static final ResourceLocation field_110911_f = new ResourceLocation("textures/entity/wither/wither.png");
    private static final ResourceLocation field_110912_g = new ResourceLocation("textures/entity/wither/wither_armor.png");
    private int field_82419_a;

    public RenderWither() {
        super(new ModelWither(), 1.0F);
        this.field_82419_a = ((ModelWither) this.mainModel).func_82903_a();
    }

    public void doRender(EntityWither wither, double x, double y, double z, float yaw, float partialTicks) {
        BossStatus.setBossStatus(wither, true);
        int size = mainModel instanceof ModelWither m ? m.func_82903_a() : field_82419_a;
        if (size != field_82419_a) {
            field_82419_a = size;
            mainModel = new ModelWither();
        }
        super.doRender((EntityLiving) wither, x, y, z, yaw, partialTicks);
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityWither wither) doRender(wither, x, y, z, yaw, partialTicks);
        else super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntityWither wither) {
        int invul = wither.getInvulTime();
        return invul > 0 && (invul > 80 || invul / 5 % 2 != 1) ? field_110913_a : field_110911_f;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityWither wither ? getEntityTexture(wither) : field_110911_f;
    }

    protected void preRenderCallback(EntityWither wither, float partialTicks) {
        int invul = wither.getInvulTime();
        if (invul > 0) {
            float scale = 2.0F - (invul - partialTicks) / 220.0F * 0.5F;
            GL11.glScalef(scale, scale, scale);
        } else {
            GL11.glScalef(2.0F, 2.0F, 2.0F);
        }
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        if (entity instanceof EntityWither wither) preRenderCallback(wither, partialTicks);
    }

    protected int shouldRenderPass(EntityWither wither, int pass, float partialTicks) {
        if (wither.isArmored()) {
            if (pass == 1) {
                GL11.glDepthMask(!wither.isInvisible());
                bindTexture(field_110912_g);
                setRenderPassModel(mainModel);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                GL11.glTranslatef(0.0F, -0.01F, 0.0F);
                GL11.glScalef(1.1F, 1.1F, 1.1F);
                return 1;
            }
            if (pass == 2) {
                GL11.glDepthMask(true);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        return -1;
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return entity instanceof EntityWither wither ? shouldRenderPass(wither, pass, partialTicks) : -1;
    }

    protected int inheritRenderPass(EntityWither wither, int pass, float partialTicks) {
        return -1;
    }

    @Override
    protected int inheritRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return -1;
    }
}
