package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** An item frame on a wall holding one displayed item. */
public class EntityItemFrame extends Entity {
    private ItemStack displayed;
    private int itemRotation;
    public final int hangingDirection;
    public int field_146063_b;
    public int field_146064_c;
    public int field_146062_d;

    public EntityItemFrame(World world) {
        super(world);
        this.hangingDirection = 0;
    }

    public EntityItemFrame(World world, int x, int y, int z, int direction) {
        super(world);
        this.field_146063_b = x;
        this.field_146064_c = y;
        this.field_146062_d = z;
        this.hangingDirection = direction;
        setPosition(x + 0.5D, y + 0.5D, z + 0.5D);
    }

    public ItemStack getDisplayedItem() {
        return displayed;
    }

    public void setDisplayedItem(ItemStack stack) {
        this.displayed = stack == null ? null : stack.copy();
    }

    public int getRotation() {
        return itemRotation;
    }

    public void setItemRotation(int rotation) {
        this.itemRotation = rotation % 4;
    }

    /**
     * Whether the frame can stay: the block it hangs on is solid and no other hanging entity overlaps it (a modern item frame
     * answers with its own survival check).
     */
    public boolean onValidSurface() {
        if (mcc$entity instanceof net.minecraft.world.entity.decoration.HangingEntity hanging && !mcc$hosted()) return hanging.survives();
        World world = mcc$world();
        if (world == null) return false;
        if (!world.getBlock(field_146063_b, field_146064_c, field_146062_d).getMaterial().isSolid()) return false;
        for (Object o : world.getEntitiesWithinAABBExcludingEntity(this, mcc$get$boundingBox(this))) {
            if (o instanceof EntityItemFrame
                    || o instanceof Entity e && e.mcc$entity instanceof net.minecraft.world.entity.decoration.HangingEntity) return false;
        }
        return true;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        itemRotation = tag.getByte("ItemRotation");
        if (tag.hasKey("Item")) displayed = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Item"));
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        tag.setByte("ItemRotation", (byte) itemRotation);
        if (displayed != null) tag.setTag("Item", displayed.writeToNBT(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound()));
    }
}
