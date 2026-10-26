package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "hugeexplosion": an invisible emitter that spawns six large explosions a tick for eight ticks. */
public class EntityHugeExplodeFX extends EntityFX {
    private int timeSinceStart;
    private int maximumTime = 8;

    public EntityHugeExplodeFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
    }

    @Override
    public void onUpdate() {
        World world = mcc$world();
        for (int i = 0; i < 6 && world != null; ++i) {
            double x = mcc$x() + (rand.nextDouble() - rand.nextDouble()) * 4.0D;
            double y = mcc$y() + (rand.nextDouble() - rand.nextDouble()) * 4.0D;
            double z = mcc$z() + (rand.nextDouble() - rand.nextDouble()) * 4.0D;
            world.spawnParticle("largeexplode", x, y, z, (float) timeSinceStart / (float) maximumTime, 0.0D, 0.0D);
        }
        ++timeSinceStart;
        if (timeSinceStart >= maximumTime) setDead();
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
