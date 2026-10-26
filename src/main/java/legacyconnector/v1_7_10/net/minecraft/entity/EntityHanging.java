package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.HangingEntity;

/**
 * An entity fixed to a block: paintings, item frames, leash knots. {@code field_146063_b/c/d} are the block it hangs on (for a
 * painting or frame the wall, for a knot the fence) and {@code hangingDirection} the side it faces (0 south, 1 west, 2 north,
 * 3 east, the modern 2D direction values). For a view of a modern painting, frame or knot these phantom fields read and move the
 * modern entity (whose own position is the block in front of the wall); a mod's hanging entity keeps them itself.
 */
public abstract class EntityHanging extends Entity {
    private int tickCounter1;

    /** Phantom: the facing (redirected to {@code mcc$get$hangingDirection}). */
    @Deprecated
    public int hangingDirection;
    private int mcc$local_hangingDirection;
    /** Phantom: the x of the block it hangs on (redirected to {@code mcc$get$field_146063_b}). */
    @Deprecated
    public int field_146063_b;
    private int mcc$local_field_146063_b;
    /** Phantom: the y of the block it hangs on (redirected to {@code mcc$get$field_146064_c}). */
    @Deprecated
    public int field_146064_c;
    private int mcc$local_field_146064_c;
    /** Phantom: the z of the block it hangs on (redirected to {@code mcc$get$field_146062_d}). */
    @Deprecated
    public int field_146062_d;
    private int mcc$local_field_146062_d;

    public EntityHanging(World world) {
        super(world);
        yOffset = 0.0F;
        setSize(0.5F, 0.5F);
    }

    public EntityHanging(World world, int x, int y, int z, int direction) {
        this(world);
        mcc$local_field_146063_b = x;
        mcc$local_field_146064_c = y;
        mcc$local_field_146062_d = z;
        mcc$local_hangingDirection = direction & 3;
        mcc$pushToModern();
    }

    /** View of a modern painting, item frame or leash knot. */
    protected EntityHanging(BlockAttachedEntity modern) {
        super(modern);
    }

    // ------------------------------------------------------------------ the modern entity this may be a view of

    /** The modern entity when this is a view of one (not for a mod's hanging entity, which a host runs). */
    protected final BlockAttachedEntity mcc$attached() {
        return mcc$entity instanceof BlockAttachedEntity a && !mcc$hosted() ? a : null;
    }

    /** The legacy block of a view: the modern entity's block, behind it for a painting or frame. */
    private BlockPos mcc$modernTile() {
        BlockAttachedEntity a = mcc$attached();
        if (a == null || a.getPos() == null) return null;
        return a instanceof HangingEntity h ? a.getPos().relative(h.getDirection().getOpposite()) : a.getPos();
    }

    /** Copies a view's modern block and facing into the local values (before one of them is changed). */
    private void mcc$pullFromModern() {
        BlockPos tile = mcc$modernTile();
        if (tile == null) return;
        mcc$local_field_146063_b = tile.getX();
        mcc$local_field_146064_c = tile.getY();
        mcc$local_field_146062_d = tile.getZ();
        if (mcc$attached() instanceof HangingEntity h && h.getDirection().getAxis().isHorizontal()) mcc$local_hangingDirection = h.getDirection().get2DDataValue();
    }

    /** Moves and turns a view's modern entity to the local block and facing. */
    private void mcc$pushToModern() {
        BlockAttachedEntity a = mcc$attached();
        if (a == null) return;
        BlockPos tile = new BlockPos(mcc$local_field_146063_b, mcc$local_field_146064_c, mcc$local_field_146062_d);
        if (a instanceof HangingEntity h) {
            Direction facing = Direction.from2DDataValue(mcc$local_hangingDirection);
            BlockPos pos = tile.relative(facing);
            a.setPos(pos.getX(), pos.getY(), pos.getZ());
            org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.setHangingDirection(h, facing);
        } else {
            a.setPos(tile.getX(), tile.getY(), tile.getZ());
        }
    }

    public static int mcc$get$hangingDirection(EntityHanging e) {
        if (e.mcc$attached() instanceof HangingEntity h && h.getPos() != null && h.getDirection().getAxis().isHorizontal()) return h.getDirection().get2DDataValue();
        return e.mcc$local_hangingDirection;
    }

    public static void mcc$set$hangingDirection(EntityHanging e, int v) {
        e.mcc$pullFromModern();
        e.mcc$local_hangingDirection = v & 3;
        e.mcc$pushToModern();
    }

    public static int mcc$get$field_146063_b(EntityHanging e) {
        BlockPos tile = e.mcc$modernTile();
        return tile != null ? tile.getX() : e.mcc$local_field_146063_b;
    }

    public static void mcc$set$field_146063_b(EntityHanging e, int v) {
        e.mcc$pullFromModern();
        e.mcc$local_field_146063_b = v;
        e.mcc$pushToModern();
    }

    public static int mcc$get$field_146064_c(EntityHanging e) {
        BlockPos tile = e.mcc$modernTile();
        return tile != null ? tile.getY() : e.mcc$local_field_146064_c;
    }

    public static void mcc$set$field_146064_c(EntityHanging e, int v) {
        e.mcc$pullFromModern();
        e.mcc$local_field_146064_c = v;
        e.mcc$pushToModern();
    }

    public static int mcc$get$field_146062_d(EntityHanging e) {
        BlockPos tile = e.mcc$modernTile();
        return tile != null ? tile.getZ() : e.mcc$local_field_146062_d;
    }

    public static void mcc$set$field_146062_d(EntityHanging e, int v) {
        e.mcc$pullFromModern();
        e.mcc$local_field_146062_d = v;
        e.mcc$pushToModern();
    }

    // ------------------------------------------------------------------ 1.7.10 behaviour

    @Override
    protected void entityInit() {
    }

    /** Turns to face {@code direction} and places the entity on that side of its block (a view turns the modern entity). */
    public void setDirection(int direction) {
        if (mcc$attached() != null) {
            mcc$set$hangingDirection(this, direction);
            return;
        }
        direction &= 3;
        mcc$local_hangingDirection = direction;
        float yaw = direction * 90;
        mcc$set$rotationYaw(this, yaw);
        mcc$set$prevRotationYaw(this, yaw);
        float halfX = getWidthPixels();
        float halfY = getHeightPixels();
        float halfZ = getWidthPixels();
        if (direction != 2 && direction != 0) halfX = 0.5F;
        else halfZ = 0.5F;
        halfX /= 32.0F;
        halfY /= 32.0F;
        halfZ /= 32.0F;
        float x = mcc$local_field_146063_b + 0.5F;
        float y = mcc$local_field_146064_c + 0.5F;
        float z = mcc$local_field_146062_d + 0.5F;
        float depth = 0.5625F;
        if (direction == 2) z -= depth;
        if (direction == 1) x -= depth;
        if (direction == 0) z += depth;
        if (direction == 3) x += depth;
        if (direction == 2) x -= mcc$sizeOffset(getWidthPixels());
        if (direction == 1) z += mcc$sizeOffset(getWidthPixels());
        if (direction == 0) x += mcc$sizeOffset(getWidthPixels());
        if (direction == 3) z -= mcc$sizeOffset(getWidthPixels());
        y += mcc$sizeOffset(getHeightPixels());
        // the host's box is as wide as the entity; the legacy posY is its middle (the modern y its bottom)
        setSize(Math.max(halfX, halfZ) * 2.0F - 0.0625F, halfY * 2.0F - 0.0625F);
        mcc$posYShift = halfY - 0.03125F;
        setPosition(x, y, z);
    }

    /** Paintings of an even block size are centred on the edge between two blocks. */
    private static float mcc$sizeOffset(int pixels) {
        return pixels == 32 ? 0.5F : pixels == 64 ? 0.5F : 0.0F;
    }

    /** Checks every 100 ticks that it can still hang there (and drops when it cannot). */
    @Override
    public void onUpdate() {
        if (mcc$attached() != null || mcc$entity == null) {
            super.onUpdate();
            return;
        }
        mcc$set$prevPosX(this, mcc$get$posX(this));
        mcc$set$prevPosY(this, mcc$get$posY(this));
        mcc$set$prevPosZ(this, mcc$get$posZ(this));
        if (tickCounter1++ == 100 && !mcc$world().isRemote) {
            tickCounter1 = 0;
            if (isEntityAlive() && !onValidSurface()) {
                setDead();
                onBroken(null);
            }
        }
    }

    /**
     * Whether it can stay: nothing solid where it is, every block behind it solid, and no other hanging entity overlapping (a view
     * answers with the modern survival check).
     */
    public boolean onValidSurface() {
        BlockAttachedEntity a = mcc$attached();
        if (a != null) return a.getPos() != null && a.survives();
        World world = mcc$world();
        if (world == null) return false;
        if (!world.getCollidingBoundingBoxes(this, mcc$get$boundingBox(this)).isEmpty()) return false;
        int wide = Math.max(1, getWidthPixels() / 16);
        int high = Math.max(1, getHeightPixels() / 16);
        int dir = mcc$local_hangingDirection;
        int bx = mcc$local_field_146063_b;
        int bz = mcc$local_field_146062_d;
        if (dir == 2) bx = MathHelper.floor_double(mcc$get$posX(this) - getWidthPixels() / 32.0F);
        if (dir == 1) bz = MathHelper.floor_double(mcc$get$posZ(this) - getWidthPixels() / 32.0F);
        if (dir == 0) bx = MathHelper.floor_double(mcc$get$posX(this) - getWidthPixels() / 32.0F);
        if (dir == 3) bz = MathHelper.floor_double(mcc$get$posZ(this) - getWidthPixels() / 32.0F);
        int by = MathHelper.floor_double(mcc$get$posY(this) - getHeightPixels() / 32.0F);
        for (int i = 0; i < wide; ++i) {
            for (int j = 0; j < high; ++j) {
                var block = dir != 2 && dir != 0 ? world.getBlock(mcc$local_field_146063_b, by + j, bz + i) : world.getBlock(bx + i, by + j, mcc$local_field_146062_d);
                if (block == null || !block.getMaterial().isSolid()) return false;
            }
        }
        for (Object o : world.getEntitiesWithinAABBExcludingEntity(this, mcc$get$boundingBox(this))) {
            if (o instanceof EntityHanging) return false;
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return mcc$attached() != null ? super.canBeCollidedWith() : true;
    }

    /** A player's punch breaks it. */
    @Override
    public boolean hitByEntity(Entity attacker) {
        return attacker instanceof EntityPlayer player && attackEntityFrom(DamageSource.causePlayerDamage(player), 0.0F);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (mcc$attached() != null) return super.attackEntityFrom(source, amount);
        if (isEntityInvulnerable()) return false;
        if (isEntityAlive() && !mcc$world().isRemote) {
            setDead();
            setBeenAttacked();
            onBroken(source == null ? null : source.getEntity());
        }
        return true;
    }

    /** Anything that moves it breaks it off. */
    @Override
    public void moveEntity(double dx, double dy, double dz) {
        if (mcc$attached() != null) {
            super.moveEntity(dx, dy, dz);
            return;
        }
        if (!mcc$world().isRemote && isEntityAlive() && dx * dx + dy * dy + dz * dz > 0.0D) {
            setDead();
            onBroken(null);
        }
    }

    @Override
    public void addVelocity(double dx, double dy, double dz) {
        if (mcc$attached() != null) {
            super.addVelocity(dx, dy, dz);
            return;
        }
        if (!mcc$world().isRemote && isEntityAlive() && dx * dx + dy * dy + dz * dz > 0.0D) {
            setDead();
            onBroken(null);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        int dir = mcc$get$hangingDirection(this);
        tag.setByte("Direction", (byte) dir);
        tag.setInteger("TileX", mcc$get$field_146063_b(this));
        tag.setInteger("TileY", mcc$get$field_146064_c(this));
        tag.setInteger("TileZ", mcc$get$field_146062_d(this));
        tag.setByte("Dir", (byte) switch (dir) {
            case 0 -> 2;
            case 2 -> 0;
            default -> dir;
        });
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        int dir;
        if (tag.hasKey("Direction", 99)) {
            dir = tag.getByte("Direction");
        } else {
            dir = switch (tag.getByte("Dir")) {
                case 2 -> 0;
                case 0 -> 2;
                default -> tag.getByte("Dir");
            };
        }
        mcc$local_field_146063_b = tag.getInteger("TileX");
        mcc$local_field_146064_c = tag.getInteger("TileY");
        mcc$local_field_146062_d = tag.getInteger("TileZ");
        setDirection(dir);
    }

    /** Width in pixels (16 per block). */
    public abstract int getWidthPixels();

    public abstract int getHeightPixels();

    /** It broke ({@code breaker} is who broke it, or null): drop its item. */
    public abstract void onBroken(Entity breaker);

    protected boolean shouldSetPosAfterLoading() {
        return false;
    }
}
