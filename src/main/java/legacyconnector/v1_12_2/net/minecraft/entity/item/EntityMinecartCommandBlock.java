package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 command block minecart. */
public class EntityMinecartCommandBlock extends EntityMinecart {
    public EntityMinecartCommandBlock(World world) {
        super(world);
    }

    public EntityMinecartCommandBlock(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityMinecartCommandBlock(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    public Type getType() {
        return Type.COMMAND_BLOCK;
    }
}
