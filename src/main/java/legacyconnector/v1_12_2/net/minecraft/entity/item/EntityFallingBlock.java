package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/**
 * 1.12.2 falling block. {@code new EntityFallingBlock(world, x, y, z, state)} is a modern falling block of that state (it falls, lands
 * and drops as the modern one); its fields (fall time, drop, hurting, tile entity data) read and write the modern entity.
 */
public class EntityFallingBlock extends Entity {
    @Deprecated
    public int fallTime;
    @Deprecated
    public boolean shouldDropItem;
    @Deprecated
    public NBTTagCompound tileEntityData;
    @Deprecated
    public IBlockState fallTile;
    private IBlockState mcc$fallTile;
    private int mcc$fallTime;
    private boolean mcc$shouldDropItem = true;
    private NBTTagCompound mcc$tileEntityData;
    private boolean mcc$hurtEntities;
    private BlockPos mcc$origin = BlockPos.ORIGIN;

    public EntityFallingBlock(World world) {
        super(world);
    }

    public EntityFallingBlock(World world, double x, double y, double z, IBlockState state) {
        super(world);
        mcc$fallTile = state;
        preventEntitySpawning = true;
        setSize(0.98F, 0.98F);
        setPosition(x, y + (1.0F - mcc$get$height(this)) / 2.0F, z);
        mcc$set$motionX(this, 0.0D);
        mcc$set$motionY(this, 0.0D);
        mcc$set$motionZ(this, 0.0D);
        mcc$set$prevPosX(this, x);
        mcc$set$prevPosY(this, y);
        mcc$set$prevPosZ(this, z);
        setOrigin(new BlockPos(mcc$x(), mcc$y(), mcc$z()));
        mcc$applyState(state);
    }

    protected EntityFallingBlock(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.item.FallingBlockEntity mcc$falling() {
        return mcc$entity instanceof net.minecraft.world.entity.item.FallingBlockEntity f ? f : null;
    }

    /** Gives the modern falling block its state (through its saved data: the modern entity has no setter). */
    private void mcc$applyState(IBlockState state) {
        var f = mcc$falling();
        if (f == null || state == null) return;
        var tag = new net.minecraft.nbt.CompoundTag();
        f.saveWithoutId(tag);
        tag.put("BlockState", net.minecraft.nbt.NbtUtils.writeBlockState(StatesBridge1122.toModern(state)));
        f.load(tag);
    }

    public static IBlockState mcc$get$fallTile(EntityFallingBlock e) {
        return e.getBlock();
    }

    public static void mcc$set$fallTile(EntityFallingBlock e, IBlockState v) {
        e.mcc$fallTile = v;
        e.mcc$applyState(v);
    }

    public static int mcc$get$fallTime(EntityFallingBlock e) {
        var f = e.mcc$falling();
        return f != null ? f.time : e.mcc$fallTime;
    }

    public static void mcc$set$fallTime(EntityFallingBlock e, int v) {
        var f = e.mcc$falling();
        if (f != null) f.time = v;
        e.mcc$fallTime = v;
    }

    public static boolean mcc$get$shouldDropItem(EntityFallingBlock e) {
        var f = e.mcc$falling();
        return f != null ? f.dropItem : e.mcc$shouldDropItem;
    }

    public static void mcc$set$shouldDropItem(EntityFallingBlock e, boolean v) {
        var f = e.mcc$falling();
        if (f != null) f.dropItem = v;
        e.mcc$shouldDropItem = v;
    }

    public static NBTTagCompound mcc$get$tileEntityData(EntityFallingBlock e) {
        var f = e.mcc$falling();
        if (f != null) return f.blockData == null ? null : new NBTTagCompound(f.blockData);
        return e.mcc$tileEntityData;
    }

    public static void mcc$set$tileEntityData(EntityFallingBlock e, NBTTagCompound v) {
        var f = e.mcc$falling();
        if (f != null) f.blockData = v == null ? null : v.mcc$modern().copy();
        e.mcc$tileEntityData = v;
    }

    /** Phantom fields: {@link #mcc$get$hurtEntities}, {@link #mcc$get$fallHurtMax}, {@link #mcc$get$fallHurtAmount}. */
    @Deprecated
    private boolean hurtEntities;
    @Deprecated
    private int fallHurtMax;
    @Deprecated
    private float fallHurtAmount;
    private int mcc$fallHurtMax = 40;
    private float mcc$fallHurtAmount = 2.0F;

    /** hurtEntities: whether the block damages what it lands on (anvils). */
    public static boolean mcc$get$hurtEntities(EntityFallingBlock e) {
        var f = e.mcc$falling();
        return f != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.fallingHurts(f) : e.mcc$hurtEntities;
    }

    public static void mcc$set$hurtEntities(EntityFallingBlock e, boolean v) {
        var f = e.mcc$falling();
        if (f != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setFallingHurts(f, v);
        e.mcc$hurtEntities = v;
    }

    /** fallHurtMax: the most damage a landing deals. */
    public static int mcc$get$fallHurtMax(EntityFallingBlock e) {
        var f = e.mcc$falling();
        return f != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.fallDamageMax(f) : e.mcc$fallHurtMax;
    }

    public static void mcc$set$fallHurtMax(EntityFallingBlock e, int v) {
        var f = e.mcc$falling();
        if (f != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setFallDamageMax(f, v);
        e.mcc$fallHurtMax = v;
    }

    /** fallHurtAmount: the damage per block fallen. */
    public static float mcc$get$fallHurtAmount(EntityFallingBlock e) {
        var f = e.mcc$falling();
        return f != null ? org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.fallDamagePerDistance(f) : e.mcc$fallHurtAmount;
    }

    public static void mcc$set$fallHurtAmount(EntityFallingBlock e, float v) {
        var f = e.mcc$falling();
        if (f != null) org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.setFallDamagePerDistance(f, v);
        e.mcc$fallHurtAmount = v;
    }

    public void setHurtEntities(boolean hurt) {
        mcc$hurtEntities = hurt;
        var f = mcc$falling();
        if (f != null && hurt) f.setHurtsEntities(2.0F, 40);
    }

    public void setOrigin(BlockPos pos) {
        mcc$origin = pos;
        var f = mcc$falling();
        if (f != null && pos != null) f.setStartPos(pos.mcc$modern());
    }

    public BlockPos getOrigin() {
        var f = mcc$falling();
        return f != null ? BlockPos.mcc$of(f.getStartPos()) : mcc$origin;
    }

    public IBlockState getBlock() {
        var f = mcc$falling();
        return f != null ? StatesBridge1122.toLegacy(f.getBlockState()) : mcc$fallTile;
    }

    public World getWorldObj() {
        return mcc$get$world(this);
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
        return !mcc$get$isDead(this);
    }

    @Override
    public boolean ignoreItemEntityData() {
        return true;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (mcc$falling() != null) return;
        IBlockState state = getBlock();
        if (state != null) {
            var id = legacyconnector.v1_12_2.net.minecraft.block.Block.REGISTRY.getNameForObject(state.getBlock());
            tag.setString("Block", id == null ? "" : id.toString());
            tag.setByte("Data", (byte) state.getBlock().getMetaFromState(state));
        }
        tag.setInteger("Time", mcc$fallTime);
        tag.setBoolean("DropItem", mcc$shouldDropItem);
        tag.setBoolean("HurtEntities", mcc$hurtEntities);
        if (mcc$tileEntityData != null) tag.setTag("TileEntityData", mcc$tileEntityData);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (mcc$falling() != null) return;
        var block = legacyconnector.v1_12_2.net.minecraft.block.Block.getBlockFromName(tag.getString("Block"));
        if (block != null) mcc$fallTile = block.getStateFromMeta(tag.getByte("Data") & 255);
        mcc$fallTime = tag.getInteger("Time");
        if (tag.hasKey("DropItem", 99)) mcc$shouldDropItem = tag.getBoolean("DropItem");
        mcc$hurtEntities = tag.getBoolean("HurtEntities");
        if (tag.hasKey("TileEntityData", 10)) mcc$tileEntityData = tag.getCompoundTag("TileEntityData");
    }
}
