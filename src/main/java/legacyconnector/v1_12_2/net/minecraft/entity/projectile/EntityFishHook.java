package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 fishing bobber: a view of the modern one (angler, hooked entity, reeling in). {@code new EntityFishHook(world, player)} is a modern
 * bobber of that player; luck and lure speed are its own.
 */
public class EntityFishHook extends Entity {
    /** Vanilla's hooked-entity key (entity id + 1, 0 for none): the modern hook's hooked entity. */
    public static final legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<Integer> DATA_HOOKED_ENTITY = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122.of(6, legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers.VARINT, EntityFishHook.class, 0, e -> {
        var h = e.mcc$hook();
        return h == null || h.getHookedIn() == null ? 0 : h.getHookedIn().getId() + 1;
    }, null);

    @Deprecated
    public boolean inGround;
    private boolean mcc$local_inGround;
    @Deprecated
    public int ticksInGround;
    private int mcc$local_ticksInGround;
    /** Ticks in flight: the modern hook does not count them, so this is the layer's own counter. */
    public int ticksInAir;
    @Deprecated
    public int ticksCatchable;
    private int mcc$local_ticksCatchable;
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
        return e.mcc$base$getAngler();
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

    public static boolean mcc$get$inGround(EntityFishHook e) {
        var h = e.mcc$hook();
        return h != null ? h.onGround() : e.mcc$local_inGround;
    }

    public static void mcc$set$inGround(EntityFishHook e, boolean v) {
        e.mcc$local_inGround = v;
    }

    public static int mcc$get$ticksInGround(EntityFishHook e) {
        var h = e.mcc$hook();
        return h != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.hookTicks(h, false) : e.mcc$local_ticksInGround;
    }

    public static void mcc$set$ticksInGround(EntityFishHook e, int v) {
        var h = e.mcc$hook();
        if (h != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setHookTicks(h, false, v);
        else e.mcc$local_ticksInGround = v;
    }

    public static int mcc$get$ticksCatchable(EntityFishHook e) {
        var h = e.mcc$hook();
        return h != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.hookTicks(h, true) : e.mcc$local_ticksCatchable;
    }

    public static void mcc$set$ticksCatchable(EntityFishHook e, int v) {
        var h = e.mcc$hook();
        if (h != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setHookTicks(h, true, v);
        else e.mcc$local_ticksCatchable = v;
    }

    /** Pulls the hooked entity towards the angler (as reeling in does). */
    protected void bringInHookedEntity() {
        var h = mcc$hook();
        if (h != null) {
            org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.pullEntity(h, h.getHookedIn());
            return;
        }
        Entity caught = mcc$get$caughtEntity(this);
        EntityPlayer angler = getAngler();
        if (caught == null || angler == null || caught.mcc$entity == null) return;
        double dx = angler.mcc$x() - mcc$x(), dy = angler.mcc$y() - mcc$y(), dz = angler.mcc$z() - mcc$z();
        caught.mcc$entity.setDeltaMovement(caught.mcc$entity.getDeltaMovement().add(dx * 0.1D, dy * 0.1D, dz * 0.1D));
    }

    /** Whether the hook may catch the entity (the modern projectile rule: not spectators, not its angler while leaving). */
    protected boolean canBeHooked(Entity entity) {
        var h = mcc$hook();
        if (h != null) return entity != null && org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.canHitEntity(h, entity.mcc$entity);
        return entity != null && entity.canBeCollidedWith() && entity != getAngler();
    }

    /** Throws the hook from the angler's eyes along the angler's look, with vanilla's speed and spread. */
    protected void shoot() {
        EntityPlayer angler = getAngler();
        var m = mcc$entity;
        if (angler == null || angler.mcc$entity == null || m == null) return;
        var p = angler.mcc$entity;
        float pitch = p.getXRot();
        float yaw = p.getYRot();
        float f2 = net.minecraft.util.Mth.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f3 = net.minecraft.util.Mth.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = -net.minecraft.util.Mth.cos(-pitch * (float) (Math.PI / 180.0));
        float f5 = net.minecraft.util.Mth.sin(-pitch * (float) (Math.PI / 180.0));
        m.moveTo(p.getX() - f3 * 0.3D, p.getEyeY(), p.getZ() - f2 * 0.3D, yaw, pitch);
        var v = new net.minecraft.world.phys.Vec3(-f3, net.minecraft.util.Mth.clamp(-(f5 / f4), -5.0F, 5.0F), -f2);
        double len = v.length();
        var random = m.getRandom();
        v = v.multiply(0.6D / len + random.triangle(0.5D, 0.0103365D), 0.6D / len + random.triangle(0.5D, 0.0103365D),
                0.6D / len + random.triangle(0.5D, 0.0103365D));
        m.setDeltaMovement(v);
        m.setYRot((float) (net.minecraft.util.Mth.atan2(v.x, v.z) * (180.0D / Math.PI)));
        m.setXRot((float) (net.minecraft.util.Mth.atan2(v.y, v.horizontalDistance()) * (180.0D / Math.PI)));
        m.yRotO = m.getYRot();
        m.xRotO = m.getXRot();
    }

    public EntityPlayer getAngler() {
        return mcc$base$getAngler();
    }

    /** The value of {@link #getAngler()}, for the phantom field accessor (mods override the getter to read the field). */
    private EntityPlayer mcc$base$getAngler() {
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
