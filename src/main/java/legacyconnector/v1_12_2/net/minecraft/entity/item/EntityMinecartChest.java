package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 chest minecart (27 slots). */
public class EntityMinecartChest extends EntityMinecartContainer {
    public EntityMinecartChest(World world) {
        super(world);
    }

    public EntityMinecartChest(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityMinecartChest(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    @Override
    public Type getType() {
        return Type.CHEST;
    }

    @Override
    public int getSizeInventory() {
        return 27;
    }

    public String getGuiID() {
        return "minecraft:chest";
    }
}
