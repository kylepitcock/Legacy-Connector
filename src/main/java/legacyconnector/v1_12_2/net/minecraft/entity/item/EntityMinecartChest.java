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

    /** 1.12's loot filling hook, called when a player first opens the cart (the modern cart fills itself on open; a mod's own cart keeps its slots). */
    public void addLoot(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }
}
