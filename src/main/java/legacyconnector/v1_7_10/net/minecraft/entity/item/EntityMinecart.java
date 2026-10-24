package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Base of minecarts (rideable 0, chest 1, furnace 2, TNT 3, spawner 4, hopper 5, command block 6) and of mods' carts. */
public abstract class EntityMinecart extends Entity {
    private String entityName;
    private boolean hasCustomName;

    public EntityMinecart(World world) {
        super(world);
        setSize(0.98F, 0.7F);
        yOffset = 0.35F;
    }

    public EntityMinecart(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y + yOffset, z);
    }

    /** The cart type, from {@link #func_94087_l} in 1.7.10; subclasses return their own. */
    public abstract int getMinecartType();

    public void setMinecartName(String name) {
        this.entityName = name;
        this.hasCustomName = name != null && !name.isEmpty();
    }

    public String func_95999_t() {
        return entityName;
    }

    public boolean hasCustomMinecartName() {
        return hasCustomName;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        if (tag.hasKey("CustomName")) setMinecartName(tag.getString("CustomName"));
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        if (hasCustomName) tag.setString("CustomName", entityName);
    }
}
