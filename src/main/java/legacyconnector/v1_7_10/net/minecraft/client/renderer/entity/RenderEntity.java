package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** 1.7.10's fallback renderer: the entity's bounding box as a white box. Mods extend it for entities they draw themselves. */
public class RenderEntity extends Render {
    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity == null) return;
        GL11.glPushMatrix();
        // the box is in world coordinates: offset it by the render position minus the entity's last position
        renderOffsetAABB(Entity.mcc$get$boundingBox(entity), x - Entity.mcc$get$lastTickPosX(entity), y - Entity.mcc$get$lastTickPosY(entity),
                z - Entity.mcc$get$lastTickPosZ(entity));
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
