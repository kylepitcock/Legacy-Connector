package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityHanging;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.decoration.ItemFrame;

/**
 * An item frame on a wall holding one displayed item. Constructing this exact class makes a modern item frame (RFTools places
 * them in its dimensions); its item and rotation (0-3, the modern frame's eighths halved) are the modern frame's.
 */
public class EntityItemFrame extends EntityHanging {
    private float itemDropChance = 1.0F;
    private ItemStack displayed;
    private int itemRotation;

    public EntityItemFrame(World world) {
        super(world);
    }

    public EntityItemFrame(World world, int x, int y, int z, int direction) {
        super(world, x, y, z, direction);
        setDirection(direction);
    }

    /** View of a modern item frame. */
    public EntityItemFrame(ItemFrame modern) {
        super(modern);
    }

    private ItemFrame mcc$frame() {
        return mcc$attached() instanceof ItemFrame f ? f : null;
    }

    public ItemStack getDisplayedItem() {
        var frame = mcc$frame();
        if (frame != null) return frame.getItem().isEmpty() ? null : ItemStack.mcc$wrap(frame.getItem());
        return displayed;
    }

    /** Shows one of the stack (a copy). */
    public void setDisplayedItem(ItemStack stack) {
        ItemStack one = stack == null ? null : stack.copy();
        if (one != null) one.mcc$setCount(1);
        var frame = mcc$frame();
        if (frame != null) {
            frame.setItem(ItemStack.mcc$toModern(one));
            return;
        }
        this.displayed = one;
    }

    public int getRotation() {
        var frame = mcc$frame();
        return frame != null ? frame.getRotation() / 2 : itemRotation;
    }

    public void setItemRotation(int rotation) {
        var frame = mcc$frame();
        if (frame != null) frame.setRotation((rotation % 4) * 2);
        else this.itemRotation = rotation % 4;
    }

    @Override
    public int getWidthPixels() {
        return 9;
    }

    @Override
    public int getHeightPixels() {
        return 9;
    }

    @Override
    public boolean isInRangeToRenderDist(double distanceSq) {
        double d = 16.0D * 64.0D * renderDistanceWeight;
        return distanceSq < d * d;
    }

    /** Hitting a frame with an item knocks the item out first. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (mcc$frame() != null) return super.attackEntityFrom(source, amount);
        if (isEntityInvulnerable()) return false;
        if (getDisplayedItem() != null) {
            if (!mcc$world().isRemote) {
                func_146065_b(source == null ? null : source.getEntity(), false);
                setDisplayedItem(null);
            }
            return true;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void onBroken(Entity breaker) {
        func_146065_b(breaker, true);
    }

    /** Drops the frame (when {@code dropFrame}) and its item; a creative player gets nothing. */
    public void func_146065_b(Entity breaker, boolean dropFrame) {
        ItemStack item = getDisplayedItem();
        if (breaker instanceof EntityPlayer p && PlayerCapabilities.mcc$get$isCreativeMode(p.capabilities)) return;
        if (dropFrame) entityDropItem(new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.item_frame), 0.0F);
        if (item != null && rand.nextFloat() < itemDropChance) entityDropItem(item.copy(), 0.0F);
    }

    /** An empty frame takes the held item; a full one turns its item a quarter. */
    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (mcc$frame() != null) return super.interactFirst(player);
        if (getDisplayedItem() == null) {
            ItemStack held = player.getHeldItem();
            if (held != null && !mcc$world().isRemote) {
                setDisplayedItem(held);
                if (!PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) {
                    held.mcc$setCount(held.mcc$count() - 1);
                    if (held.mcc$count() <= 0) {
                        player.inventory.setInventorySlotContents(legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer.mcc$get$currentItem(player.inventory), null);
                    }
                }
            }
        } else if (!mcc$world().isRemote) {
            setItemRotation(getRotation() + 1);
        }
        return true;
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("Item")) {
            setDisplayedItem(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Item")));
            setItemRotation(tag.getByte("ItemRotation"));
            if (tag.hasKey("ItemDropChance", 99)) itemDropChance = tag.getFloat("ItemDropChance");
        }
        super.readEntityFromNBT(tag);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        ItemStack item = getDisplayedItem();
        if (item != null) {
            tag.setTag("Item", item.writeToNBT(new NBTTagCompound()));
            tag.setByte("ItemRotation", (byte) getRotation());
            tag.setFloat("ItemDropChance", itemDropChance);
        }
        super.writeEntityToNBT(tag);
    }
}
