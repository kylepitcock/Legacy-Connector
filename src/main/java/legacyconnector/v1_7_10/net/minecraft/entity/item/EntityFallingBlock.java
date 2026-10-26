package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.BlockFalling;
import legacyconnector.v1_7_10.net.minecraft.block.ITileEntityProvider;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess;

/**
 * A block that fell off its position (sand, gravel, anvils); mods drop their own blocks with it. Constructing this exact class
 * makes a modern falling block (its block, fall time and drop flag are the modern entity's); a mod's subclass falls by the 1.7.10
 * rules: lands as a block (with its tile entity data) or drops as an item, and hurts what it lands on when told to.
 */
public class EntityFallingBlock extends Entity {
    private Block blockObj;
    /** Phantom: the block's metadata (redirected to {@code mcc$get$metadata}). */
    @Deprecated
    public int metadata;
    private int mcc$local_metadata;
    /** Phantom: ticks it has fallen (redirected to {@code mcc$get$fallTime}). */
    @Deprecated
    public int fallTime;
    private int mcc$local_fallTime;
    /** Phantom: whether it drops as an item when it cannot land (redirected to {@code mcc$get$shouldDropItem}). */
    @Deprecated
    public boolean shouldDropItem;
    private boolean mcc$local_shouldDropItem = true;
    /** An anvil broke from the fall: it neither lands nor drops. */
    public boolean field_145808_f;
    public boolean hurtEntities;
    public int fallHurtMax = 40;
    public float fallHurtAmount = 2.0F;
    /** Tile entity data written into the block's tile entity when it lands. */
    public NBTTagCompound tileEntityData;

    public EntityFallingBlock(World world) {
        super(world);
    }

    public EntityFallingBlock(World world, double x, double y, double z, Block block) {
        this(world, x, y, z, block, 0);
    }

    public EntityFallingBlock(World world, double x, double y, double z, Block block, int meta) {
        super(world);
        this.blockObj = block;
        this.mcc$local_metadata = meta;
        preventEntitySpawning = true;
        setSize(0.98F, 0.98F);
        yOffset = mcc$get$height(this) / 2.0F;
        setPosition(x, y, z);
        setVelocity(0.0D, 0.0D, 0.0D);
        mcc$set$prevPosX(this, x);
        mcc$set$prevPosY(this, y);
        mcc$set$prevPosZ(this, z);
        var falling = mcc$falling();
        if (falling != null) {
            ModernAccess.setFallingBlockState(falling, BlockBridge.toModern(block, meta));
            falling.setStartPos(falling.blockPosition());
            // 1.7.10's falling block took its block out of the world on its first tick; the modern one expects that done already
            int bx = MathHelper.floor_double(x), by = MathHelper.floor_double(y), bz = MathHelper.floor_double(z);
            if (world != null && !world.isRemote && block != null && world.getBlock(bx, by, bz) == block) world.setBlockToAir(bx, by, bz);
        }
    }

    /** View of a modern falling block. */
    public EntityFallingBlock(FallingBlockEntity modern) {
        super(modern);
    }

    private FallingBlockEntity mcc$falling() {
        return mcc$entity instanceof FallingBlockEntity f && !mcc$hosted() ? f : null;
    }

    public static int mcc$get$metadata(EntityFallingBlock e) {
        var f = e.mcc$falling();
        return f != null ? BlockBridge.legacyMeta(f.getBlockState()) : e.mcc$local_metadata;
    }

    public static void mcc$set$metadata(EntityFallingBlock e, int v) {
        e.mcc$local_metadata = v;
        var f = e.mcc$falling();
        if (f != null) ModernAccess.setFallingBlockState(f, BlockBridge.toModern(e.getBlock(), v));
    }

    public static int mcc$get$fallTime(EntityFallingBlock e) {
        var f = e.mcc$falling();
        return f != null ? f.time : e.mcc$local_fallTime;
    }

    public static void mcc$set$fallTime(EntityFallingBlock e, int v) {
        var f = e.mcc$falling();
        if (f != null) f.time = v;
        else e.mcc$local_fallTime = v;
    }

    public static boolean mcc$get$shouldDropItem(EntityFallingBlock e) {
        var f = e.mcc$falling();
        return f != null ? f.dropItem : e.mcc$local_shouldDropItem;
    }

    public static void mcc$set$shouldDropItem(EntityFallingBlock e, boolean v) {
        var f = e.mcc$falling();
        if (f != null) f.dropItem = v;
        else e.mcc$local_shouldDropItem = v;
    }

    /** The falling block. */
    public Block getBlock() {
        var f = mcc$falling();
        return f != null ? BlockBridge.legacyBlock(f.getBlockState()) : blockObj;
    }

    public Block func_145805_f() {
        return getBlock();
    }

    /** Whether it hurts what it lands on (2 per block fallen, at most 40; anvils). */
    public void setHurtEntities(boolean hurts) {
        this.hurtEntities = hurts;
        var f = mcc$falling();
        if (f != null) ModernAccess.setFallingBlockHurts(f, hurts);
    }

    public World getWorldObj() {
        return mcc$world();
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public boolean canBeCollidedWith() {
        return mcc$falling() != null ? super.canBeCollidedWith() : isEntityAlive();
    }

    @Override
    public float getShadowSize() {
        return 0.0F;
    }

    public boolean canRenderOnFire() {
        return false;
    }

    /** One tick of a mod's falling block (a modern one falls by itself). */
    @Override
    public void onUpdate() {
        if (mcc$falling() != null || mcc$entity == null) {
            super.onUpdate();
            return;
        }
        Block block = blockObj;
        if (block == null || block.getMaterial() == Material.air) {
            setDead();
            return;
        }
        World world = mcc$world();
        mcc$set$prevPosX(this, mcc$get$posX(this));
        mcc$set$prevPosY(this, mcc$get$posY(this));
        mcc$set$prevPosZ(this, mcc$get$posZ(this));
        ++mcc$local_fallTime;
        mcc$set$motionY(this, mcc$get$motionY(this) - 0.03999999910593033D);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        setVelocity(mcc$get$motionX(this) * 0.9800000190734863D, mcc$get$motionY(this) * 0.9800000190734863D, mcc$get$motionZ(this) * 0.9800000190734863D);
        if (world.isRemote) return;
        int x = MathHelper.floor_double(mcc$get$posX(this));
        int y = MathHelper.floor_double(mcc$get$posY(this));
        int z = MathHelper.floor_double(mcc$get$posZ(this));
        if (mcc$local_fallTime == 1) {
            if (world.getBlock(x, y, z) != block) {
                setDead();
                return;
            }
            world.setBlockToAir(x, y, z);
        }
        if (mcc$get$onGround(this)) {
            setVelocity(mcc$get$motionX(this) * 0.699999988079071D, mcc$get$motionY(this) * -0.5D, mcc$get$motionZ(this) * 0.699999988079071D);
            if (world.getBlock(x, y, z) == Blocks.piston_extension) return;
            setDead();
            if (!field_145808_f && world.canPlaceEntityOnSide(block, x, y, z, true, 1, null, null) && !BlockFalling.canFallBelow(world, x, y - 1, z)
                    && world.setBlock(x, y, z, block, mcc$local_metadata, 3)) {
                if (tileEntityData != null && block instanceof ITileEntityProvider) {
                    var te = world.getTileEntity(x, y, z);
                    if (te != null) {
                        NBTTagCompound tag = new NBTTagCompound();
                        te.writeToNBT(tag);
                        for (String key : tileEntityData.getKeySet()) {
                            if (!key.equals("x") && !key.equals("y") && !key.equals("z")) tag.setTag(key, tileEntityData.getTag(key).copy());
                        }
                        te.readFromNBT(tag);
                        te.markDirty();
                    }
                }
            } else if (mcc$local_shouldDropItem && !field_145808_f) {
                entityDropItem(new ItemStack(block, 1, block.damageDropped(mcc$local_metadata)), 0.0F);
            }
        } else if (mcc$local_fallTime > 100 && (y < 1 || y > 256) || mcc$local_fallTime > 600) {
            if (mcc$local_shouldDropItem) entityDropItem(new ItemStack(block, 1, block.damageDropped(mcc$local_metadata)), 0.0F);
            setDead();
        }
    }

    /** Landing: hurts what is there (anvils chip and finally break). */
    @Override
    protected void fall(float distance) {
        if (mcc$falling() != null || !hurtEntities) {
            super.fall(distance);
            return;
        }
        int blocks = MathHelper.ceiling_float_int(distance - 1.0F);
        if (blocks <= 0) return;
        boolean anvil = blockObj == Blocks.anvil;
        DamageSource source = anvil ? DamageSource.anvil : DamageSource.fallingBlock;
        for (Object o : mcc$world().getEntitiesWithinAABBExcludingEntity(this, mcc$get$boundingBox(this))) {
            if (o instanceof Entity e) e.attackEntityFrom(source, Math.min(MathHelper.floor_float(blocks * fallHurtAmount), fallHurtMax));
        }
        if (anvil && rand.nextFloat() < 0.05000000074505806D + blocks * 0.05D) {
            int damage = (mcc$local_metadata >> 2) + 1;
            if (damage > 2) field_145808_f = true;
            else mcc$local_metadata = mcc$local_metadata & 3 | damage << 2;
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        Block block = getBlock();
        tag.setByte("Tile", (byte) Block.getIdFromBlock(block));
        tag.setInteger("TileID", Block.getIdFromBlock(block));
        tag.setByte("Data", (byte) mcc$get$metadata(this));
        tag.setByte("Time", (byte) mcc$get$fallTime(this));
        tag.setBoolean("DropItem", mcc$get$shouldDropItem(this));
        tag.setBoolean("HurtEntities", hurtEntities);
        tag.setFloat("FallHurtAmount", fallHurtAmount);
        tag.setInteger("FallHurtMax", fallHurtMax);
        if (tileEntityData != null) tag.setTag("TileEntityData", tileEntityData);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        blockObj = tag.hasKey("TileID", 99) ? Block.getBlockById(tag.getInteger("TileID")) : Block.getBlockById(tag.getByte("Tile") & 255);
        mcc$local_metadata = tag.getByte("Data") & 255;
        mcc$local_fallTime = tag.getByte("Time") & 255;
        if (tag.hasKey("HurtEntities", 99)) {
            hurtEntities = tag.getBoolean("HurtEntities");
            fallHurtAmount = tag.getFloat("FallHurtAmount");
            fallHurtMax = tag.getInteger("FallHurtMax");
        } else if (blockObj == Blocks.anvil) {
            hurtEntities = true;
        }
        if (tag.hasKey("DropItem", 99)) mcc$local_shouldDropItem = tag.getBoolean("DropItem");
        if (tag.hasKey("TileEntityData", 10)) tileEntityData = tag.getCompoundTag("TileEntityData");
        if (blockObj == null || blockObj.getMaterial() == Material.air) blockObj = Blocks.sand;
    }
}
