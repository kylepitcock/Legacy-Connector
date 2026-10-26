package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntitySheep;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityWolf;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's wolf renderer (also the base of mod wolves): wild, tamed and angry textures, the tail raised by health, the
 * wet shading while shaking and the dyed collar of a tamed wolf as render passes.
 */
public class RenderWolf extends RenderLiving {
    private static final ResourceLocation wolfTextures = new ResourceLocation("textures/entity/wolf/wolf.png");
    private static final ResourceLocation tamedWolfTextures = new ResourceLocation("textures/entity/wolf/wolf_tame.png");
    private static final ResourceLocation anrgyWolfTextures = new ResourceLocation("textures/entity/wolf/wolf_angry.png");
    private static final ResourceLocation wolfCollarTextures = new ResourceLocation("textures/entity/wolf/wolf_collar.png");

    public RenderWolf(ModelBase model, ModelBase passModel, float shadowSize) {
        super(model, shadowSize);
        setRenderPassModel(passModel);
    }

    /** The tail angle: up when angry, by health when tamed. */
    protected float handleRotationFloat(EntityWolf wolf, float partialTicks) {
        if (wolf.mcc$entity instanceof net.minecraft.world.entity.animal.Wolf modern) return modern.getTailAngle();
        if (wolf.isAngry()) return 1.5393804F;
        if (wolf.isTamed()) return (0.55F - (20.0F - wolf.getHealth()) * 0.02F) * (float) Math.PI;
        return (float) Math.PI / 5.0F;
    }

    @Override
    protected float handleRotationFloat(EntityLivingBase entity, float partialTicks) {
        return entity instanceof EntityWolf wolf ? handleRotationFloat(wolf, partialTicks) : super.handleRotationFloat(entity, partialTicks);
    }

    protected int shouldRenderPass(EntityWolf wolf, int pass, float partialTicks) {
        if (pass == 0 && wolf.mcc$entity instanceof net.minecraft.world.entity.animal.Wolf modern && modern.isWet()) {
            float shade = wolf.getBrightness(partialTicks) * modern.getWetShade(partialTicks);
            bindTexture(wolfTextures);
            GL11.glColor3f(shade, shade, shade);
            return 1;
        }
        if (pass == 1 && wolf.isTamed()) {
            bindTexture(wolfCollarTextures);
            int color = wolf.getCollarColor();
            if (color < 0 || color >= EntitySheep.fleeceColorTable.length) color = 14;
            GL11.glColor3f(EntitySheep.fleeceColorTable[color][0], EntitySheep.fleeceColorTable[color][1], EntitySheep.fleeceColorTable[color][2]);
            return 1;
        }
        if (pass == 2) GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        return -1;
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return entity instanceof EntityWolf wolf ? shouldRenderPass(wolf, pass, partialTicks) : -1;
    }

    protected ResourceLocation getEntityTexture(EntityWolf wolf) {
        return wolf.isTamed() ? tamedWolfTextures : wolf.isAngry() ? anrgyWolfTextures : wolfTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityWolf wolf ? getEntityTexture(wolf) : wolfTextures;
    }
}
