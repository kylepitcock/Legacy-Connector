package legacyconnector.v1_7_10.net.minecraft.entity;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;

/**
 * The knot a lead makes on a fence. Constructing this exact class makes a modern knot; a mod's knot (AgriCraft ties leads to its
 * stakes) is a legacy entity whose block is {@code field_146063_b/c/d}.
 */
public class EntityLeashKnot extends EntityHanging {
    public EntityLeashKnot(World world) {
        super(world);
    }

    public EntityLeashKnot(World world, int x, int y, int z) {
        super(world, x, y, z, 0);
        setPosition(x + 0.5D, y + 0.5D, z + 0.5D);
    }

    /** View of a modern knot. */
    public EntityLeashKnot(LeashFenceKnotEntity modern) {
        super(modern);
    }

    /** A knot does not turn. */
    @Override
    public void setDirection(int direction) {
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
        return distanceSq < 1024.0D;
    }

    @Override
    public void onBroken(Entity breaker) {
    }

    /** Knots are not saved on their own (the leashed mobs recreate them). */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
    }

    /**
     * A player holding a lead ties the mobs they lead to this knot; otherwise the click unties every mob tied here (dropping the
     * leads unless the player is in creative) and removes the knot.
     */
    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (mcc$attached() != null) return super.interactFirst(player);
        ItemStack held = player.getHeldItem();
        boolean tied = false;
        List<?> nearby = mcc$leashed();
        if (held != null && held.getItem() == Items.lead && !mcc$world().isRemote) {
            for (Object o : nearby) {
                if (o instanceof EntityLiving mob && mob.getLeashed() && mob.getLeashedToEntity() == player) {
                    mob.setLeashedToEntity(this, true);
                    tied = true;
                }
            }
        }
        if (!mcc$world().isRemote && !tied) {
            setDead();
            if (PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) {
                for (Object o : nearby) {
                    if (o instanceof EntityLiving mob && mob.getLeashed() && mob.getLeashedToEntity() == this) mob.clearLeashed(true, false);
                }
            }
        }
        return true;
    }

    private List<?> mcc$leashed() {
        int x = mcc$get$field_146063_b(this), y = mcc$get$field_146064_c(this), z = mcc$get$field_146062_d(this);
        double r = 7.0D;
        return mcc$world().getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r));
    }

    /** Whether the block is a fence (render type 11), or what the modern knot accepts. */
    @Override
    public boolean onValidSurface() {
        if (mcc$attached() != null) return super.onValidSurface();
        var block = mcc$world().getBlock(mcc$get$field_146063_b(this), mcc$get$field_146064_c(this), mcc$get$field_146062_d(this));
        return block != null && block.getRenderType() == 11;
    }

    /** Ties a new knot to the fence at the position (spawned). */
    public static EntityLeashKnot func_110129_a(World world, int x, int y, int z) {
        EntityLeashKnot knot = new EntityLeashKnot(world, x, y, z);
        knot.forceSpawn = true;
        world.spawnEntityInWorld(knot);
        return knot;
    }

    /** The knot already on the fence at the position, or null. */
    public static EntityLeashKnot getKnotForBlock(World world, int x, int y, int z) {
        List<?> list = world.getEntitiesWithinAABB(EntityLeashKnot.class, AxisAlignedBB.getBoundingBox(x - 1.0D, y - 1.0D, z - 1.0D, x + 1.0D, y + 1.0D, z + 1.0D));
        for (Object o : list) {
            if (o instanceof EntityLeashKnot knot && mcc$get$field_146063_b(knot) == x && mcc$get$field_146064_c(knot) == y && mcc$get$field_146062_d(knot) == z) return knot;
        }
        return null;
    }
}
