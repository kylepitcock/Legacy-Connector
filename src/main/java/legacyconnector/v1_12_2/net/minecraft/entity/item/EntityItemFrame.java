package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityHanging;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/** 1.12.2 item frame: a view of the modern item frame (item, rotation); {@code new EntityItemFrame(world, pos, facing)} is a modern one. */
public class EntityItemFrame extends EntityHanging {
    private ItemStack mcc$item = ItemStack.EMPTY;
    private int mcc$rotation;
    private float itemDropChance = 1.0F;

    public EntityItemFrame(World world) {
        super(world);
    }

    public EntityItemFrame(World world, BlockPos pos, EnumFacing facing) {
        super(world, pos);
        updateFacingWithBoundingBox(facing);
    }

    protected EntityItemFrame(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.decoration.ItemFrame mcc$frame() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.decoration.ItemFrame f ? f : null;
    }

    @Override
    public float getCollisionBorderSize() {
        return 0.0F;
    }

    @Override
    public int getWidthPixels() {
        return 12;
    }

    @Override
    public int getHeightPixels() {
        return 12;
    }

    public ItemStack getDisplayedItem() {
        var f = mcc$frame();
        return f != null ? Stacks1122.view(f.getItem()) : mcc$item;
    }

    public void setDisplayedItem(ItemStack stack) {
        var f = mcc$frame();
        if (f != null) {
            f.setItem(Stacks1122.toModern(stack));
            return;
        }
        mcc$item = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!mcc$item.isEmpty()) mcc$item.setCount(1);
    }

    public int getRotation() {
        var f = mcc$frame();
        return f != null ? f.getRotation() : mcc$rotation;
    }

    public void setItemRotation(int rotation) {
        var f = mcc$frame();
        if (f != null) f.setRotation(rotation);
        else mcc$rotation = rotation % 8;
    }

    @Override
    public void onBroken(Entity brokenEntity) {
        playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.ITEM_FRAME_BREAK), 1.0F, 1.0F);
        dropItemOrSelf(brokenEntity, true);
    }

    public void dropItemOrSelf(Entity entity, boolean dropSelf) {
        if (mcc$entity == null || !mcc$entity.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) return;
        ItemStack item = getDisplayedItem();
        if (entity instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer p && p.isCreative()) {
            setDisplayedItem(ItemStack.EMPTY);
            return;
        }
        if (dropSelf) entityDropItem(new ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:item_frame")), 0.0F);
        if (!item.isEmpty() && rand.nextFloat() < itemDropChance) {
            entityDropItem(item.copy(), 0.0F);
        }
        setDisplayedItem(ItemStack.EMPTY);
    }

    @Override
    public void playPlaceSound() {
        playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.ITEM_FRAME_PLACE), 1.0F, 1.0F);
    }

    public int getAnalogOutput() {
        return getDisplayedItem().isEmpty() ? 0 : getRotation() % 8 + 1;
    }

    @Override
    public boolean ignoreItemEntityData() {
        return true;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$frame() != null) return;
        if (!mcc$item.isEmpty()) {
            tag.setTag("Item", mcc$item.writeToNBT(new NBTTagCompound()));
            tag.setByte("ItemRotation", (byte) mcc$rotation);
            tag.setFloat("ItemDropChance", itemDropChance);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$frame() != null) return;
        if (tag.hasKey("Item", 10)) {
            mcc$item = new ItemStack(tag.getCompoundTag("Item"));
            mcc$rotation = tag.getByte("ItemRotation");
            if (tag.hasKey("ItemDropChance", 99)) itemDropChance = tag.getFloat("ItemDropChance");
        }
    }
}
