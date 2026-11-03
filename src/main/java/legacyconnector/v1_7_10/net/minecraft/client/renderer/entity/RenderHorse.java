package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.LayeredTexture;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityHorse;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's horse renderer (also donkey, mule, zombie and skeleton horse, and the base of mod horses): fixed skins by type, coat +
 * markings + armor layered into one texture (cached by the horse's texture key), donkeys and mules slightly smaller. The foal
 * scale is applied by {@link legacyconnector.v1_7_10.net.minecraft.client.model.ModelHorse}.
 */
public class RenderHorse extends RenderLiving {
    private static final Map field_110852_a = new HashMap();
    private static final ResourceLocation whiteHorseTextures = new ResourceLocation("textures/entity/horse/horse_white.png");
    private static final ResourceLocation muleTextures = new ResourceLocation("textures/entity/horse/mule.png");
    private static final ResourceLocation donkeyTextures = new ResourceLocation("textures/entity/horse/donkey.png");
    private static final ResourceLocation zombieHorseTextures = new ResourceLocation("textures/entity/horse/horse_zombie.png");
    private static final ResourceLocation skeletonHorseTextures = new ResourceLocation("textures/entity/horse/horse_skeleton.png");

    public RenderHorse(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    protected void preRenderCallback(EntityHorse horse, float partialTicks) {
        float scale = 1.0F;
        int type = horse.getHorseType();
        if (type == 1) scale *= 0.87F;
        else if (type == 2) scale *= 0.92F;
        GL11.glScalef(scale, scale, scale);
        super.preRenderCallback((EntityLivingBase) horse, partialTicks);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        if (entity instanceof EntityHorse horse) preRenderCallback(horse, partialTicks);
        else super.preRenderCallback(entity, partialTicks);
    }

    protected void renderModel(EntityHorse horse, float swing, float swingAmount, float age, float headYaw, float pitch, float scale) {
        if (horse.isInvisible()) {
            this.mainModel.setRotationAngles(swing, swingAmount, age, headYaw, pitch, scale, horse);
        } else {
            bindEntityTexture(horse);
            this.mainModel.render(horse, swing, swingAmount, age, headYaw, pitch, scale);
        }
    }

    @Override
    protected void renderModel(EntityLivingBase entity, float swing, float swingAmount, float age, float headYaw, float pitch, float scale) {
        if (entity instanceof EntityHorse horse) renderModel(horse, swing, swingAmount, age, headYaw, pitch, scale);
        else super.renderModel(entity, swing, swingAmount, age, headYaw, pitch, scale);
    }

    protected ResourceLocation getEntityTexture(EntityHorse horse) {
        if (!horse.func_110239_cn()) {
            return switch (horse.getHorseType()) {
                case 1 -> donkeyTextures;
                case 2 -> muleTextures;
                case 3 -> zombieHorseTextures;
                case 4 -> skeletonHorseTextures;
                default -> whiteHorseTextures;
            };
        }
        return func_110848_b(horse);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityHorse horse ? getEntityTexture(horse) : whiteHorseTextures;
    }

    /** The layered skin (coat, markings, armor) of the horse, built on first use and cached by its texture key. */
    private ResourceLocation func_110848_b(EntityHorse horse) {
        String key = horse.getHorseTexture();
        synchronized (field_110852_a) {
            ResourceLocation location = (ResourceLocation) field_110852_a.get(key);
            if (location == null) {
                location = new ResourceLocation(key);
                Minecraft.getMinecraft().getTextureManager().loadTexture(location, new LayeredTexture(horse.getVariantTexturePaths()));
                field_110852_a.put(key, location);
            }
            return location;
        }
    }

    public void doRender(EntityHorse horse, double x, double y, double z, float yaw, float partialTicks) {
        super.doRender((EntityLiving) horse, x, y, z, yaw, partialTicks);
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityHorse horse) doRender(horse, x, y, z, yaw, partialTicks);
        else super.doRender(entity, x, y, z, yaw, partialTicks);
    }
}
