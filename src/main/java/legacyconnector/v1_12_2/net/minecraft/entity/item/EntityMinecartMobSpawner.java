package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 spawner minecart. */
public class EntityMinecartMobSpawner extends EntityMinecart {
    public EntityMinecartMobSpawner(World world) {
        super(world);
    }

    public EntityMinecartMobSpawner(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityMinecartMobSpawner(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    public Type getType() {
        return Type.SPAWNER;
    }
}
