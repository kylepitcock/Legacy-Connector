package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBoat;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityBoat;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's boat renderer (mods extend it for their own boats): the boat model, rocking after a hit. The vanilla 1.7.10
 * boat texture is gone (modern boats have another model and layout), so only a mod's own texture draws correctly.
 */
public class RenderBoat extends Render {
    private static final ResourceLocation boatTextures = new ResourceLocation("textures/entity/boat.png");
    protected ModelBase modelBoat;

    public RenderBoat() {
        this.shadowSize = 0.5F;
        this.modelBoat = new ModelBoat();
    }

    public void doRender(EntityBoat boat, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        float hit = boat.getTimeSinceHit() - partialTicks;
        float damage = Math.max(0.0F, boat.getDamageTaken() - partialTicks);
        if (hit > 0.0F) GL11.glRotatef((float) Math.sin(hit) * hit * damage / 10.0F * boat.getForwardDirection(), 1.0F, 0.0F, 0.0F);
        bindEntityTexture(boat);
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        modelBoat.render(boat, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GL11.glPopMatrix();
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityBoat boat) doRender(boat, x, y, z, yaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntityBoat boat) {
        return boatTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityBoat boat ? getEntityTexture(boat) : boatTextures;
    }
}
