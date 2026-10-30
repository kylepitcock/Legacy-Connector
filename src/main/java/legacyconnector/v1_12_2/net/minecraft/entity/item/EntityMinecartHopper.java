package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 hopper minecart (5 slots). */
public class EntityMinecartHopper extends EntityMinecartContainer {
    public EntityMinecartHopper(World world) {
        super(world);
    }

    public EntityMinecartHopper(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityMinecartHopper(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    public Type getType() {
        return Type.HOPPER;
    }

    @Override
    public int getSizeInventory() {
        return 5;
    }

    public boolean getBlocked() {
        var m = mcc$entity;
        return m instanceof net.minecraft.world.entity.vehicle.MinecartHopper h && !h.isEnabled();
    }

    public void setBlocked(boolean blocked) {
        if (mcc$entity instanceof net.minecraft.world.entity.vehicle.MinecartHopper h && !mcc$hosted()) h.setEnabled(!blocked);
    }
}
