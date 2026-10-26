package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The crack particle of a broken item or thrown snowball: a piece of the item's icon. */
public class EntityBreakingFX extends EntityFX {
    public EntityBreakingFX(World world, double x, double y, double z, Item item) {
        this(world, x, y, z, item, 0);
    }

    public EntityBreakingFX(World world, double x, double y, double z, Item item, int damage) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        try {
            this.particleIcon = item == null ? null : item.getIconFromDamage(damage);
        } catch (RuntimeException | LinkageError e) {
            this.particleIcon = null;
        }
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
        this.particleGravity = 1.0F;
        this.particleScale /= 2.0F;
    }

    public EntityBreakingFX(World world, double x, double y, double z, double vx, double vy, double vz, Item item, int damage) {
        this(world, x, y, z, item, damage);
        mcc$scaleMotion(0.1D, 0.1D, 0.1D);
        setVelocity(mcc$get$motionX(this) + vx, mcc$get$motionY(this) + vy, mcc$get$motionZ(this) + vz);
    }

    @Override
    public int getFXLayer() {
        return 2;
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        mcc$renderIconPiece(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }
}
