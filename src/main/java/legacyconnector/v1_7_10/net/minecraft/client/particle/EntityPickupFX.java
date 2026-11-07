package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The item pickup effect (an item flying to the collector); drawn by the modern pickup animation, so this particle only lives briefly. */
public class EntityPickupFX extends EntityFX {
    public EntityPickupFX(World world, Entity item, Entity collector, float yOffset) {
        super(world, item == null ? 0.0D : Entity.mcc$get$posX(item), item == null ? 0.0D : Entity.mcc$get$posY(item), item == null ? 0.0D : Entity.mcc$get$posZ(item), 0.0D, 0.0D, 0.0D);
        this.particleMaxAge = 3;
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
    }
}
