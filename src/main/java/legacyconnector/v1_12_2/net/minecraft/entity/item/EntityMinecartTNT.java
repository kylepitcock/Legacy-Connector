package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 TNT minecart (a view ignites and counts down as the modern one). */
public class EntityMinecartTNT extends EntityMinecart {
    public EntityMinecartTNT(World world) {
        super(world);
    }

    public EntityMinecartTNT(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityMinecartTNT(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    public Type getType() {
        return Type.TNT;
    }

    private int mcc$fuse = -1;

    public void ignite() {
        if (mcc$entity instanceof net.minecraft.world.entity.vehicle.MinecartTNT t && !mcc$hosted()) t.primeFuse();
        else mcc$fuse = 80;
    }

    public int getFuseTicks() {
        if (mcc$entity instanceof net.minecraft.world.entity.vehicle.MinecartTNT t && !mcc$hosted()) return t.getFuse();
        return mcc$fuse;
    }

    public boolean isIgnited() {
        if (mcc$entity instanceof net.minecraft.world.entity.vehicle.MinecartTNT t && !mcc$hosted()) return t.isPrimed();
        return mcc$fuse > -1;
    }
}
