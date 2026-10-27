package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 fishing bobber: a view of the modern one (angler, hooked entity, reeling in). {@code new EntityFishHook(world, player)} is a modern
 * bobber of that player; luck and lure speed are its own.
 */
public class EntityFishHook extends Entity {
    @Deprecated
    public EntityPlayer angler;
    @Deprecated
    public Entity caughtEntity;
    private EntityPlayer mcc$angler;
    private int mcc$luck;
    private int mcc$lureSpeed;

    public EntityFishHook(World world, EntityPlayer player) {
        super(world);
        setSize(0.25F, 0.25F);
        mcc$angler = player;
        var h = mcc$hook();
        if (h != null && player != null && player.mcc$entity != null) h.setOwner(player.mcc$entity);
    }

    public EntityFishHook(World world, EntityPlayer player, double x, double y, double z) {
        this(world, player);
        setPosition(x, y, z);
    }

    protected EntityFishHook(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.projectile.FishingHook mcc$hook() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.FishingHook h ? h : null;
    }

    public static EntityPlayer mcc$get$angler(EntityFishHook e) {
        return e.getAngler();
    }

    public static void mcc$set$angler(EntityFishHook e, EntityPlayer v) {
        e.mcc$angler = v;
    }

    public static Entity mcc$get$caughtEntity(EntityFishHook e) {
        var h = e.mcc$hook();
        return h != null ? org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(h.getHookedIn()) : null;
    }

    public static void mcc$set$caughtEntity(EntityFishHook e, Entity v) {
    }

    public EntityPlayer getAngler() {
        var h = mcc$hook();
        if (h != null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(h.getPlayerOwner()) instanceof EntityPlayer p ? p : mcc$angler;
        return mcc$angler;
    }

    public void setLureSpeed(int speed) {
        mcc$lureSpeed = speed;
    }

    public void setLuck(int luck) {
        mcc$luck = luck;
    }

    /** Reels in: the catch, or the hooked entity pulled closer; the rod damage (the modern retrieve). */
    public int handleHookRetraction() {
        var h = mcc$hook();
        if (h == null) {
            setDead();
            return 0;
        }
        var owner = h.getPlayerOwner();
        var rod = owner == null ? net.minecraft.world.item.ItemStack.EMPTY : owner.getMainHandItem();
        return h.retrieve(rod);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
