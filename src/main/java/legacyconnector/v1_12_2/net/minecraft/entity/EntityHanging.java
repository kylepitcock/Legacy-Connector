package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 hanging entity (paintings, item frames, mods' wall decorations): hangs on the block face at {@link #hangingPosition} facing
 * {@link #facingDirection}. A view reads the modern hanging entity; a mod's subclass places its own box, checks every 100 ticks that
 * its wall is still there and breaks (dropping itself) otherwise, verbatim 1.12.
 */
public abstract class EntityHanging extends Entity {
    private int tickCounter1;
    @Deprecated
    public BlockPos hangingPosition;
    @Deprecated
    public EnumFacing facingDirection;
    private BlockPos mcc$hangingPosition;
    private EnumFacing mcc$facing;

    public EntityHanging(World world) {
        super(world);
        setSize(0.5F, 0.5F);
    }

    public EntityHanging(World world, BlockPos pos) {
        this(world);
        mcc$hangingPosition = pos;
    }

    protected EntityHanging(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.decoration.HangingEntity mcc$hanging() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.decoration.HangingEntity h ? h : null;
    }

    public static BlockPos mcc$get$hangingPosition(EntityHanging e) {
        return e.getHangingPosition();
    }

    public static void mcc$set$hangingPosition(EntityHanging e, BlockPos v) {
        e.mcc$hangingPosition = v;
    }

    public static EnumFacing mcc$get$facingDirection(EntityHanging e) {
        var h = e.mcc$hanging();
        return h != null ? EnumFacing.mcc$of(h.getDirection()) : e.mcc$facing;
    }

    public static void mcc$set$facingDirection(EntityHanging e, EnumFacing v) {
        e.mcc$facing = v;
    }

    /** Moves an entity a mod made (not yet in the world) through its saved data: position and facing of the modern hanging entity. */
    protected final void mcc$placeModern(BlockPos pos, EnumFacing facing) {
        var h = mcc$hanging();
        if (h == null || h.isAddedToLevel() || pos == null || facing == null) return;
        var tag = new net.minecraft.nbt.CompoundTag();
        h.saveWithoutId(tag);
        tag.putInt("TileX", pos.getX());
        tag.putInt("TileY", pos.getY());
        tag.putInt("TileZ", pos.getZ());
        tag.putByte("Facing", (byte) (h instanceof net.minecraft.world.entity.decoration.ItemFrame ? facing.mcc$modern().get3DDataValue()
                : facing.mcc$modern().get2DDataValue()));
        h.load(tag);
    }

    @Override
    protected void entityInit() {
    }

    /** Faces the entity and sets its box (horizontal facings only for 1.12's base class). */
    protected void updateFacingWithBoundingBox(EnumFacing facing) {
        if (facing == null) return;
        mcc$facing = facing;
        mcc$set$rotationYaw(this, facing.getHorizontalIndex() * 90);
        mcc$set$prevRotationYaw(this, mcc$get$rotationYaw(this));
        updateBoundingBox();
        mcc$placeModern(mcc$hangingPosition, facing);
    }

    protected void updateBoundingBox() {
        if (mcc$facing == null || mcc$hangingPosition == null || mcc$hanging() != null) return;
        double x = mcc$hangingPosition.getX() + 0.5D;
        double y = mcc$hangingPosition.getY() + 0.5D;
        double z = mcc$hangingPosition.getZ() + 0.5D;
        double offsetW = offs(getWidthPixels());
        double offsetH = offs(getHeightPixels());
        x -= mcc$facing.getXOffset() * 0.46875D;
        z -= mcc$facing.getZOffset() * 0.46875D;
        y += offsetH;
        EnumFacing ccw = mcc$facing.rotateYCCW();
        x += offsetW * ccw.getXOffset();
        z += offsetW * ccw.getZOffset();
        setPosition(x, y, z);
        double w = getWidthPixels();
        double h = getHeightPixels();
        double d = getWidthPixels();
        if (mcc$facing.getAxis() == EnumFacing.Axis.Z) d = 1.0D;
        else w = 1.0D;
        w /= 32.0D;
        h /= 32.0D;
        d /= 32.0D;
        setEntityBoundingBox(new AxisAlignedBB(x - w, y - h, z - d, x + w, y + h, z + d));
    }

    private double offs(int pixels) {
        return pixels % 32 == 0 ? 0.5D : 0.0D;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!mcc$hosted() || mcc$entity == null || mcc$entity.level().isClientSide()) return;
        if (tickCounter1++ == 100 && !mcc$get$isDead(this)) {
            tickCounter1 = 0;
            if (!onValidSurface()) {
                setDead();
                onBroken(null);
            }
        }
    }

    /** The wall behind is solid over the whole width and height and no other hanging entity overlaps (1.12). */
    public boolean onValidSurface() {
        var h = mcc$hanging();
        if (h != null) return h.survives();
        var m = mcc$entity;
        if (m == null || mcc$facing == null || mcc$hangingPosition == null) return true;
        var level = m.level();
        if (!level.noCollision(m)) return false;
        int w = Math.max(1, getWidthPixels() / 16);
        int hh = Math.max(1, getHeightPixels() / 16);
        BlockPos behind = mcc$hangingPosition.offset(mcc$facing.getOpposite());
        EnumFacing ccw = mcc$facing.rotateYCCW();
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < hh; ++j) {
                int dx = (w - 1) / -2;
                int dy = (hh - 1) / -2;
                BlockPos p = behind.offset(ccw, i + dx).up(j + dy);
                var state = level.getBlockState(p.mcc$modern());
                if (!state.isSolid() && !net.minecraft.world.level.block.DiodeBlock.isDiode(state)) return false;
            }
        }
        for (var other : level.getEntities(m, m.getBoundingBox(), e -> e instanceof net.minecraft.world.entity.decoration.HangingEntity
                || org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e) instanceof EntityHanging)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean hitByEntity(Entity attacker) {
        return attacker instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer p && attackEntityFrom(DamageSource.causePlayerDamage(p), 0.0F);
    }

    @Override
    public EnumFacing getHorizontalFacing() {
        EnumFacing f = mcc$get$facingDirection(this);
        return f == null ? super.getHorizontalFacing() : f;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!mcc$hosted()) return super.attackEntityFrom(source, amount);
        if (isEntityInvulnerable(source)) return false;
        if (!mcc$get$isDead(this) && mcc$entity != null && !mcc$entity.level().isClientSide()) {
            setDead();
            markVelocityChanged();
            onBroken(source == null ? null : source.getTrueSource());
        }
        return true;
    }

    @Override
    public void move(MoverType type, double x, double y, double z) {
        if (!mcc$hosted()) {
            super.move(type, x, y, z);
            return;
        }
        if (mcc$entity != null && !mcc$entity.level().isClientSide() && !mcc$get$isDead(this) && x * x + y * y + z * z > 0.0D) {
            setDead();
            onBroken(null);
        }
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        if (!mcc$hosted()) {
            super.addVelocity(x, y, z);
            return;
        }
        if (mcc$entity != null && !mcc$entity.level().isClientSide() && !mcc$get$isDead(this) && x * x + y * y + z * z > 0.0D) {
            setDead();
            onBroken(null);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (mcc$hanging() != null) return;
        EnumFacing f = mcc$facing;
        tag.setByte("Facing", (byte) (f == null ? 0 : f.getHorizontalIndex()));
        BlockPos p = getHangingPosition();
        if (p != null) {
            tag.setInteger("TileX", p.getX());
            tag.setInteger("TileY", p.getY());
            tag.setInteger("TileZ", p.getZ());
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (mcc$hanging() != null) return;
        mcc$hangingPosition = new BlockPos(tag.getInteger("TileX"), tag.getInteger("TileY"), tag.getInteger("TileZ"));
        updateFacingWithBoundingBox(EnumFacing.byHorizontalIndex(tag.getByte("Facing")));
    }

    public abstract int getWidthPixels();

    public abstract int getHeightPixels();

    public abstract void onBroken(Entity brokenEntity);

    public abstract void playPlaceSound();

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem entityDropItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack, float offsetY) {
        var f = mcc$facing;
        if (f == null || !mcc$hosted()) return super.entityDropItem(stack, offsetY);
        double x = mcc$x() + f.getXOffset() * 0.15F;
        double z = mcc$z() + f.getZOffset() * 0.15F;
        var item = new legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem(mcc$get$world(this), x, mcc$y() + offsetY, z, stack);
        item.setDefaultPickupDelay();
        mcc$get$world(this).spawnEntity(item);
        return item;
    }

    public BlockPos getHangingPosition() {
        var h = mcc$hanging();
        return h != null ? BlockPos.mcc$of(h.getPos()) : mcc$hangingPosition;
    }
}
