package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.item.ItemEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.LiveStacks1122;

/**
 * A dropped item: a view of a modern item entity (an exact EntityItem a mod constructs is a new modern item entity). Its stack is live:
 * {@code entityItem.getItem().shrink(1)} changes the item on the ground.
 */
public class EntityItem extends Entity {
    /** Vanilla's item key: the modern item entity's stack. */
    public static final legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<ItemStack> ITEM = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122.of(6, legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers.ITEM_STACK, EntityItem.class, ItemStack.EMPTY,
            e -> e.mcc$item() == null ? ItemStack.EMPTY : Stacks1122.view(e.mcc$item().getItem()),
            (e, v) -> {
                if (e.mcc$item() != null) e.mcc$item().setItem(Stacks1122.toModern(v));
            });

    @Deprecated
    public float hoverStart;
    @Deprecated
    public int age;
    @Deprecated
    public int lifespan;
    @Deprecated
    public int pickupDelay;
    private ItemStack mcc$localStack = ItemStack.EMPTY;
    private int mcc$localAge;
    private int mcc$localPickupDelay;
    private int mcc$localLifespan = 6000;
    private String mcc$thrower;
    private String mcc$owner;

    public EntityItem(World world, double x, double y, double z) {
        super(world);
        setSize(0.25F, 0.25F);
        setPosition(x, y, z);
        mcc$set$rotationYaw(this, (float) (Math.random() * 360.0D));
        if (mcc$entity == null) {
            mcc$set$motionX(this, Math.random() * 0.20000000298023224D - 0.10000000149011612D);
            mcc$set$motionY(this, 0.20000000298023224D);
            mcc$set$motionZ(this, Math.random() * 0.20000000298023224D - 0.10000000149011612D);
        } else {
            mcc$entity.setDeltaMovement(Math.random() * 0.2D - 0.1D, 0.2D, Math.random() * 0.2D - 0.1D);
        }
    }

    public EntityItem(World world, double x, double y, double z, ItemStack stack) {
        this(world, x, y, z);
        setItem(stack);
    }

    public EntityItem(World world) {
        super(world);
        setSize(0.25F, 0.25F);
        setItem(ItemStack.EMPTY);
    }

    public EntityItem(ItemEntity modern) {
        super(modern);
    }

    private ItemEntity mcc$item() {
        return mcc$entity instanceof ItemEntity i ? i : null;
    }

    // ------------------------------------------------------------------ phantom fields

    public static float mcc$get$hoverStart(EntityItem e) {
        var i = e.mcc$item();
        return i != null ? i.bobOffs : 0.0F;
    }

    public static void mcc$set$hoverStart(EntityItem e, float v) {
    }

    public static int mcc$get$age(EntityItem e) {
        var i = e.mcc$item();
        return i != null ? i.getAge() : e.mcc$localAge;
    }

    public static void mcc$set$age(EntityItem e, int v) {
        var i = e.mcc$item();
        if (i != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ItemEntityAccess1122.setAge(i, v);
        else e.mcc$localAge = v;
    }

    public static int mcc$get$lifespan(EntityItem e) {
        var i = e.mcc$item();
        return i != null ? i.lifespan : e.mcc$localLifespan;
    }

    public static void mcc$set$lifespan(EntityItem e, int v) {
        var i = e.mcc$item();
        if (i != null) i.lifespan = v;
        e.mcc$localLifespan = v;
    }

    public static int mcc$get$pickupDelay(EntityItem e) {
        var i = e.mcc$item();
        return i != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ItemEntityAccess1122.pickupDelay(i) : e.mcc$localPickupDelay;
    }

    public static void mcc$set$pickupDelay(EntityItem e, int v) {
        e.setPickupDelay(v);
    }

    // ------------------------------------------------------------------ the stack

    public ItemStack getItem() {
        var i = mcc$item();
        if (i == null) return mcc$localStack;
        return LiveStacks1122.view(i, "item", new LiveStacks1122.Slot() {
            @Override
            public net.minecraft.world.item.ItemStack get() {
                return i.getItem();
            }

            @Override
            public void set(net.minecraft.world.item.ItemStack stack) {
                if (stack.isEmpty()) i.discard();
                else i.setItem(stack);
            }
        });
    }

    public void setItem(ItemStack stack) {
        var i = mcc$item();
        if (i == null) {
            mcc$localStack = stack == null ? ItemStack.EMPTY : stack;
            return;
        }
        LiveStacks1122.put(i, "item", new LiveStacks1122.Slot() {
            @Override
            public net.minecraft.world.item.ItemStack get() {
                return i.getItem();
            }

            @Override
            public void set(net.minecraft.world.item.ItemStack s) {
                if (s.isEmpty()) i.discard();
                else i.setItem(s);
            }
        }, stack == null ? ItemStack.EMPTY : stack);
    }

    // ------------------------------------------------------------------ pickup, despawn, owners

    public void setDefaultPickupDelay() {
        setPickupDelay(10);
    }

    public void setNoPickupDelay() {
        setPickupDelay(0);
    }

    /** Merges with the item entities around (the modern merge). */
    public void searchForOtherItemsNearby() {
        var i = mcc$item();
        if (i != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ItemEntityAccess1122.mergeWithNeighbours(i);
    }

    /** A display-only item: never picked up, and gone on its next tick. */
    public void makeFakeItem() {
        setInfinitePickupDelay();
        var i = mcc$item();
        if (i != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ItemEntityAccess1122.setAge(i, i.lifespan - 1);
    }

    public void setInfinitePickupDelay() {
        setPickupDelay(32767);
    }

    public void setPickupDelay(int ticks) {
        var i = mcc$item();
        if (i != null) i.setPickUpDelay(ticks);
        else mcc$localPickupDelay = ticks;
    }

    public boolean cannotPickup() {
        return mcc$get$pickupDelay(this) > 0;
    }

    public void setNoDespawn() {
        var i = mcc$item();
        if (i != null) i.setUnlimitedLifetime();
        else mcc$localAge = -6000;
    }

    public void setAgeToCreativeDespawnTime() {
        var i = mcc$item();
        if (i != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ItemEntityAccess1122.setAge(i, 4800);
        else mcc$localAge = 4800;
    }

    public int getAge() {
        return mcc$get$age(this);
    }

    public String getOwner() {
        var i = mcc$item();
        if (i != null && i.getTarget() != null) {
            var level = i.level();
            var p = level.getPlayerByUUID(i.getTarget());
            return p == null ? i.getTarget().toString() : p.getGameProfile().getName();
        }
        return mcc$owner;
    }

    public void setOwner(String owner) {
        mcc$owner = owner;
        var i = mcc$item();
        var id = owner == null ? null : mcc$uuidOf(i, owner);
        if (i != null) i.setTarget(id);
    }

    public String getThrower() {
        var i = mcc$item();
        if (i != null && i.getOwner() != null) {
            var owner = i.getOwner();
            return owner instanceof net.minecraft.world.entity.player.Player p ? p.getGameProfile().getName() : owner.getUUID().toString();
        }
        return mcc$thrower;
    }

    public void setThrower(String thrower) {
        mcc$thrower = thrower;
        var i = mcc$item();
        var id = thrower == null ? null : mcc$uuidOf(i, thrower);
        var player = i == null || id == null ? null : i.level().getPlayerByUUID(id);
        if (player != null) i.setThrower(player);
    }

    private static java.util.UUID mcc$uuidOf(ItemEntity i, String name) {
        if (i != null) {
            for (var p : i.level().players()) if (p.getGameProfile().getName().equals(name)) return p.getUUID();
        }
        try {
            return java.util.UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public boolean attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source, float amount) {
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public String getName() {
        if (hasCustomName()) return getCustomNameTag();
        ItemStack s = getItem();
        return s.isEmpty() ? "item" : "item." + s.getTranslationKey();
    }

    @Override
    public boolean canBeAttackedWithItem() {
        return false;
    }

    protected void dealFireDamage(int amount) {
        attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource.IN_FIRE, amount);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (mcc$item() != null) return;
        mcc$localAge = tag.getShort("Age");
        if (tag.hasKey("PickupDelay")) mcc$localPickupDelay = tag.getShort("PickupDelay");
        if (tag.hasKey("Lifespan")) mcc$localLifespan = tag.getInteger("Lifespan");
        mcc$localStack = new ItemStack(tag.getCompoundTag("Item"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (mcc$item() != null) return;
        tag.setShort("Age", (short) mcc$localAge);
        tag.setShort("PickupDelay", (short) mcc$localPickupDelay);
        tag.setInteger("Lifespan", mcc$localLifespan);
        if (!mcc$localStack.isEmpty()) tag.setTag("Item", mcc$localStack.writeToNBT(new NBTTagCompound()));
    }

    /** The modern stack (for bridges handing it to modern code). */
    public net.minecraft.world.item.ItemStack mcc$modernStack() {
        var i = mcc$item();
        return i != null ? i.getItem() : Stacks1122.toModern(mcc$localStack);
    }
}
