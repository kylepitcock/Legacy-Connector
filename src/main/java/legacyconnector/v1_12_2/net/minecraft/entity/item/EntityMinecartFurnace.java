package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 furnace minecart (pushes itself while it has fuel). */
public class EntityMinecartFurnace extends EntityMinecart {
    public EntityMinecartFurnace(World world) {
        super(world);
    }

    public EntityMinecartFurnace(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityMinecartFurnace(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    public Type getType() {
        return Type.FURNACE;
    }
}
