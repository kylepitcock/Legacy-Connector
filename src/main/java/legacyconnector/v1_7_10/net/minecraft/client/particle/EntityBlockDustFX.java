package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "blockdust_": a digging particle that keeps exactly the given velocity (falling sand dust, landing). */
public class EntityBlockDustFX extends EntityDiggingFX {
    public EntityBlockDustFX(World world, double x, double y, double z, double vx, double vy, double vz, Block block, int meta) {
        super(world, x, y, z, vx, vy, vz, block, meta);
        setVelocity(vx, vy, vz);
    }
}
