package legacyconnector.v1_12_2.net.minecraft.entity.monster;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/** 1.12.2 enderman: carries a block, screams when provoked, teleports. A view reads the modern enderman; the carriable-block
 * registry ({@link #setCarriable}) is the one the layer already kept for mods. */
public class EntityEnderman extends EntityMob {
    public EntityEnderman(World world) {
        super(world);
        setSize(0.6F, 2.9F);
    }

    protected EntityEnderman(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.EnderMan mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.EnderMan m ? m : null;
    }

    private static final DataParameter<Boolean> SCREAMING = EntityDataManager.createKey(EntityEnderman.class, DataSerializers.BOOLEAN);
    private legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState mcc$held;

    public static void setCarriable(legacyconnector.v1_12_2.net.minecraft.block.Block block, boolean canCarry) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.RuntimeApis1122.mcc$setCarriable(block, canCarry);
    }

    public static boolean getCarriable(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.RuntimeApis1122.mcc$isCarriable(block);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SCREAMING, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(7.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0D);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackMelee(this, 1.0D, false));
        tasks.addTask(7, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D, 0.0F));
        tasks.addTask(8, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(8, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, false));
    }

    public void setHeldBlockState(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        var m = mcc$self();
        if (m != null) m.setCarriedBlock(state == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state));
        mcc$held = state;
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getHeldBlockState() {
        var m = mcc$self();
        if (m != null) {
            var s = m.getCarriedBlock();
            return s == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(s);
        }
        return mcc$held;
    }

    public boolean isScreaming() {
        var m = mcc$self();
        return m != null ? m.isCreepy() : Boolean.TRUE.equals(dataManager.get(SCREAMING));
    }

    /** Teleports to a random spot within 32 blocks (1.12; the modern random teleport for views). */
    protected boolean teleportRandomly() {
        double x = mcc$x() + (rand.nextDouble() - 0.5D) * 64.0D;
        double y = mcc$y() + (rand.nextInt(64) - 32);
        double z = mcc$z() + (rand.nextDouble() - 0.5D) * 64.0D;
        return attemptTeleport(x, y, z);
    }

    @Override
    public float getEyeHeight() {
        return 2.55F;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() != null || mcc$held == null) return;
        var id = legacyconnector.v1_12_2.net.minecraft.block.Block.REGISTRY.getNameForObject(mcc$held.getBlock());
        tag.setString("carried", id == null ? "minecraft:air" : id.toString());
        tag.setShort("carriedData", (short) mcc$held.getBlock().getMetaFromState(mcc$held));
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() != null) return;
        var block = tag.hasKey("carried", 8) ? legacyconnector.v1_12_2.net.minecraft.block.Block.getBlockFromName(tag.getString("carried")) : null;
        mcc$held = block == null ? null : block.getStateFromMeta(tag.getShort("carriedData") & 65535);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(isScreaming() ? SoundEvents.ENDERMAN_SCREAM : SoundEvents.ENDERMAN_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.ENDERMAN_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.ENDERMAN_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/enderman");
    }
}
