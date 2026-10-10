package mccompat.v1_7_10.net.minecraft.entity.item;

import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.item.ItemEntity;

public class EntityItem extends Entity {
    @Deprecated
    public int delayBeforeCanPickup;
    @Deprecated
    public int age;
    public int lifespan = 6000;
    /** Phantom: render bobbing phase. */
    @Deprecated
    public float hoverStart;
    private float mcc$hover = (float) (Math.random() * Math.PI * 2.0D);

    public EntityItem(World world, double x, double y, double z, ItemStack stack) {
        super(new ItemEntity(world.mcc$fullLevel(), x, y, z, ItemStack.mcc$toModern(stack).copy()));
    }

    public EntityItem(World world, double x, double y, double z) {
        super(new ItemEntity(world.mcc$fullLevel(), x, y, z, net.minecraft.world.item.ItemStack.EMPTY));
    }

    public EntityItem(World world) {
        this(world, 0, 0, 0);
    }

    public EntityItem(ItemEntity modern) {
        super(modern);
    }

    public ItemEntity mcc$item() {
        return (ItemEntity) mcc$entity;
    }

    public static int mcc$get$delayBeforeCanPickup(EntityItem e) {
        return e.mcc$item().hasPickUpDelay() ? 10 : 0;
    }

    public static void mcc$set$delayBeforeCanPickup(EntityItem e, int v) {
        e.mcc$item().setPickUpDelay(v);
    }

    public static float mcc$get$hoverStart(EntityItem e) {
        return e.mcc$entity instanceof ItemEntity ie ? ie.bobOffs : e.mcc$hover;
    }

    public static void mcc$set$hoverStart(EntityItem e, float v) {
        e.mcc$hover = v;
    }

    public static int mcc$get$age(EntityItem e) {
        return e.mcc$item().getAge();
    }

    public static void mcc$set$age(EntityItem e, int v) {
    }

    public ItemStack getEntityItem() {
        return ItemStack.mcc$wrap(mcc$item().getItem());
    }

    public void setEntityItemStack(ItemStack stack) {
        mcc$item().setItem(ItemStack.mcc$toModern(stack));
    }

    public String getThrower() {
        var t = mcc$item().getOwner();
        return t == null ? null : t.getName().getString();
    }

    public void setAgeToCreativeDespawnTime() {
        mcc$item().setExtendedLifetime();
    }
}
