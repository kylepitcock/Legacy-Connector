package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The storage minecart (27 slots); constructing this exact class makes a vanilla chest cart. */
public class EntityMinecartChest extends EntityMinecartContainer {
    public EntityMinecartChest(World world) {
        super(world);
    }

    public EntityMinecartChest(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    /** Breaks the cart; the chest drops too unless an explosion broke it. */
    @Override
    public void killMinecart(DamageSource source) {
        super.killMinecart(source);
        if (source == null || !source.isExplosion()) entityDropItem(new ItemStack(Blocks.chest, 1), 0.0F);
    }

    @Override
    public int getSizeInventory() {
        return 27;
    }

    @Override
    public int getMinecartType() {
        return 1;
    }

    @Override
    public Block getDefaultDisplayTile() {
        return Blocks.chest;
    }

    @Override
    public int getDefaultDisplayTileOffset() {
        return 8;
    }
}
