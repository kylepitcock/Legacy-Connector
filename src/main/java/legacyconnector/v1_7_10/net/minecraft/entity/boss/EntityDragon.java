package legacyconnector.v1_7_10.net.minecraft.entity.boss;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.IEntityMultiPart;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.IMob;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 ender dragon, the base of mods' dragon bosses (Draconic Evolution's), which usually replace its flight AI with
 * their own copy: the body parts, the 64-tick position ring buffer their models read and the part-damage rules. The parts
 * are not entities of the world; the dragon itself takes the hits (a modern player cannot target a part), counted as body
 * hits.
 */
public class EntityDragon extends EntityLiving implements IBossDisplayData, IEntityMultiPart, IMob {
    public double targetX;
    public double targetY;
    public double targetZ;
    public double[][] ringBuffer = new double[64][3];
    public int ringBufferIndex = -1;
    public EntityDragonPart[] dragonPartArray;
    public EntityDragonPart dragonPartHead;
    public EntityDragonPart dragonPartBody;
    public EntityDragonPart dragonPartTail1;
    public EntityDragonPart dragonPartTail2;
    public EntityDragonPart dragonPartTail3;
    public EntityDragonPart dragonPartWing1;
    public EntityDragonPart dragonPartWing2;
    public float prevAnimTime;
    public float animTime;
    public boolean forceNewTarget;
    public boolean slowed;
    public int deathTicks;

    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityDragon(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
        createParts();
    }

    public EntityDragon(World world) {
        super(world);
        createParts();
        setHealth(getMaxHealth());
        setSize(16.0F, 8.0F);
        mcc$set$noClip(this, true);
        this.isImmuneToFire = true;
        this.targetY = 100.0D;
    }

    private void createParts() {
        dragonPartArray = new EntityDragonPart[] {
                dragonPartHead = new EntityDragonPart(this, "head", 6.0F, 6.0F),
                dragonPartBody = new EntityDragonPart(this, "body", 8.0F, 8.0F),
                dragonPartTail1 = new EntityDragonPart(this, "tail", 4.0F, 4.0F),
                dragonPartTail2 = new EntityDragonPart(this, "tail", 4.0F, 4.0F),
                dragonPartTail3 = new EntityDragonPart(this, "tail", 4.0F, 4.0F),
                dragonPartWing1 = new EntityDragonPart(this, "wing", 4.0F, 4.0F),
                dragonPartWing2 = new EntityDragonPart(this, "wing", 4.0F, 4.0F)};
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(200.0D);
    }

    /** Yaw, height and pitch {@code ticksAgo} ticks back (the body segments trail the head), interpolated by {@code partialTicks}. */
    public double[] getMovementOffsets(int ticksAgo, float partialTicks) {
        if (getHealth() <= 0.0F) partialTicks = 0.0F;
        partialTicks = 1.0F - partialTicks;
        int j = ringBufferIndex - ticksAgo & 63;
        int k = ringBufferIndex - ticksAgo - 1 & 63;
        double[] out = new double[3];
        double yaw = ringBuffer[j][0];
        out[0] = yaw + MathHelper.wrapAngleTo180_double(ringBuffer[k][0] - yaw) * partialTicks;
        out[1] = ringBuffer[j][1] + (ringBuffer[k][1] - ringBuffer[j][1]) * partialTicks;
        out[2] = ringBuffer[j][2] + (ringBuffer[k][2] - ringBuffer[j][2]) * partialTicks;
        return out;
    }

    /** Records this tick's yaw and height (what vanilla's flight AI did first each tick; mods' copies do it themselves). */
    @Override
    public void onLivingUpdate() {
        prevAnimTime = animTime;
        animTime += 0.1F;
        float yaw = MathHelper.wrapAngleTo180_float(mcc$get$rotationYaw(this));
        mcc$set$rotationYaw(this, yaw);
        double y = mcc$get$posY(this);
        if (ringBufferIndex < 0) {
            for (double[] entry : ringBuffer) {
                entry[0] = yaw;
                entry[1] = y;
            }
        }
        if (++ringBufferIndex == ringBuffer.length) ringBufferIndex = 0;
        ringBuffer[ringBufferIndex][0] = yaw;
        ringBuffer[ringBufferIndex][1] = y;
        super.onLivingUpdate();
    }

    @Override
    public Entity[] getParts() {
        return dragonPartArray;
    }

    @Override
    public World func_82194_d() {
        return mcc$world();
    }

    /** Hits off the head do a quarter; only players and explosions hurt the dragon; it veers off when hit. */
    @Override
    public boolean attackEntityFromPart(EntityDragonPart part, DamageSource source, float damage) {
        if (part != dragonPartHead) damage = damage / 4.0F + 1.0F;
        float yaw = mcc$get$rotationYaw(this) * (float) Math.PI / 180.0F;
        targetX = mcc$get$posX(this) + MathHelper.sin(yaw) * 5.0F + (rand.nextFloat() - 0.5F) * 2.0F;
        targetY = mcc$get$posY(this) + rand.nextFloat() * 3.0F + 1.0D;
        targetZ = mcc$get$posZ(this) - MathHelper.cos(yaw) * 5.0F + (rand.nextFloat() - 0.5F) * 2.0F;
        if (source.getEntity() instanceof EntityPlayer || source.isExplosion()) func_82195_e(source, damage);
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage) {
        return attackEntityFromPart(dragonPartBody, source, damage);
    }

    /** The plain living-entity damage (what the parts' hits end in). */
    protected boolean func_82195_e(DamageSource source, float damage) {
        return super.attackEntityFrom(source, damage);
    }

    @Override
    protected void despawnEntity() {
    }

    @Override
    protected String getLivingSound() {
        return "mob.enderdragon.growl";
    }

    @Override
    protected String getHurtSound() {
        return "mob.enderdragon.hit";
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    public IChatComponent func_145748_c_() {
        return getFormattedCommandSenderName();
    }
}
