package legacyconnector.v1_12_2.net.minecraft.entity.passive;

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

/**
 * 1.12.2 horse base (horses, donkeys, mules, llamas, undead horses). A view reads and changes the modern horse (tame, owner, saddle,
 * temper, rearing, eating and mouth animations, jump strength); a mod's subclass keeps the 1.12 state in its data and NBT. (Riding and
 * the horse inventory of mods' own horses are not modelled; the modern horses ride as usual.)
 */
public abstract class AbstractHorse extends EntityAnimal implements legacyconnector.v1_12_2.net.minecraft.inventory.IInventoryChangedListener {
    private static final DataParameter<Byte> STATUS = EntityDataManager.createKey(AbstractHorse.class, DataSerializers.BYTE);
    private static final DataParameter<com.google.common.base.Optional<java.util.UUID>> OWNER_UNIQUE_ID = EntityDataManager.createKey(AbstractHorse.class,
            DataSerializers.OPTIONAL_UNIQUE_ID);
    protected static final legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute JUMP_STRENGTH =
            new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.RangedAttribute(null, "horse.jumpStrength", 0.7D, 0.0D, 2.0D)
                    .setDescription("Jump Strength").setShouldWatch(true);
    @Deprecated
    public int tailCounter;
    private int mcc$tailCounter;
    protected int temper;
    protected boolean horseJumping;
    protected float jumpPower;
    protected boolean canGallop = true;

    public AbstractHorse(World world) {
        super(world);
        setSize(1.3964844F, 1.6F);
        mcc$set$stepHeight(this, 1.0F);
    }

    protected AbstractHorse(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.horse.AbstractHorse mcc$horse() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h ? h : null;
    }

    public static int mcc$get$tailCounter(AbstractHorse e) {
        var h = e.mcc$horse();
        return h != null ? h.tailCounter : e.mcc$tailCounter;
    }

    public static void mcc$set$tailCounter(AbstractHorse e, int v) {
        var h = e.mcc$horse();
        if (h != null) h.tailCounter = v;
        else e.mcc$tailCounter = v;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATUS, (byte) 0);
        dataManager.register(OWNER_UNIQUE_ID, com.google.common.base.Optional.absent());
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIPanic(this, 1.2D));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMate(this, 1.0D, AbstractHorse.class));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIFollowParent(this, 1.0D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 0.7D));
        tasks.addTask(7, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(8, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(JUMP_STRENGTH);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(53.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.22499999403953552D);
    }

    protected boolean getHorseWatchableBoolean(int mask) {
        Byte b = dataManager.get(STATUS);
        return b != null && (b & mask) != 0;
    }

    protected void setHorseWatchableBoolean(int mask, boolean value) {
        Byte b = dataManager.get(STATUS);
        byte v = b == null ? 0 : b;
        dataManager.set(STATUS, (byte) (value ? v | mask : v & ~mask));
    }

    public boolean isTame() {
        var h = mcc$horse();
        return h != null ? h.isTamed() : getHorseWatchableBoolean(2);
    }

    public void setHorseTamed(boolean tamed) {
        var h = mcc$horse();
        if (h != null) h.setTamed(tamed);
        else setHorseWatchableBoolean(2, tamed);
    }

    public java.util.UUID getOwnerUniqueId() {
        var h = mcc$horse();
        if (h != null) return h.getOwnerUUID();
        var id = dataManager.get(OWNER_UNIQUE_ID);
        return id == null ? null : id.orNull();
    }

    public void setOwnerUniqueId(java.util.UUID id) {
        var h = mcc$horse();
        if (h != null) h.setOwnerUUID(id);
        else dataManager.set(OWNER_UNIQUE_ID, com.google.common.base.Optional.fromNullable(id));
    }

    public boolean setTamedBy(EntityPlayer player) {
        var h = mcc$horse();
        if (h != null && player != null && player.mcc$player() != null) return h.tameWithName(player.mcc$player());
        setOwnerUniqueId(player == null ? null : player.getUniqueID());
        setHorseTamed(true);
        return true;
    }

    public boolean isHorseSaddled() {
        var h = mcc$horse();
        return h != null ? h.isSaddled() : getHorseWatchableBoolean(4);
    }

    public void setHorseSaddled(boolean saddled) {
        var h = mcc$horse();
        if (h != null) {
            if (saddled && !h.isSaddled()) h.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
            return;
        }
        setHorseWatchableBoolean(4, saddled);
    }

    public boolean isBreeding() {
        var h = mcc$horse();
        return h != null ? h.isBred() : getHorseWatchableBoolean(8);
    }

    public void setBreeding(boolean breeding) {
        var h = mcc$horse();
        if (h != null) h.setBred(breeding);
        else setHorseWatchableBoolean(8, breeding);
    }

    public boolean isEatingHaystack() {
        var h = mcc$horse();
        return h != null ? h.isEating() : getHorseWatchableBoolean(16);
    }

    public void setEatingHaystack(boolean eating) {
        var h = mcc$horse();
        if (h != null) h.setEating(eating);
        else setHorseWatchableBoolean(16, eating);
    }

    public boolean isRearing() {
        var h = mcc$horse();
        return h != null ? h.isStanding() : getHorseWatchableBoolean(32);
    }

    public void setRearing(boolean rearing) {
        var h = mcc$horse();
        if (h != null) h.setStanding(rearing);
        else setHorseWatchableBoolean(32, rearing);
    }

    public int getTemper() {
        var h = mcc$horse();
        return h != null ? h.getTemper() : temper;
    }

    public void setTemper(int temper) {
        var h = mcc$horse();
        if (h != null) h.setTemper(temper);
        else this.temper = temper;
    }

    public int increaseTemper(int amount) {
        var h = mcc$horse();
        if (h != null) return h.modifyTemper(amount);
        int i = net.minecraft.util.Mth.clamp(getTemper() + amount, 0, getMaxTemper());
        setTemper(i);
        return i;
    }

    public int getMaxTemper() {
        return 100;
    }

    public double getHorseJumpStrength() {
        var h = mcc$horse();
        if (h != null) return h.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
        var a = getEntityAttribute(JUMP_STRENGTH);
        return a == null ? 0.7D : a.getAttributeValue();
    }

    public float getGrassEatingAmount(float partialTicks) {
        var h = mcc$horse();
        return h != null ? h.getEatAnim(partialTicks) : isEatingHaystack() ? 1.0F : 0.0F;
    }

    public float getRearingAmount(float partialTicks) {
        var h = mcc$horse();
        return h != null ? h.getStandAnim(partialTicks) : isRearing() ? 1.0F : 0.0F;
    }

    public float getMouthOpennessAngle(float partialTicks) {
        var h = mcc$horse();
        return h != null ? h.getMouthAnim(partialTicks) : 0.0F;
    }

    /** The child scale (half size growing to full). */
    public float getHorseSize() {
        return 0.5F;
    }

    public boolean isHorseJumping() {
        return horseJumping;
    }

    public void setHorseJumping(boolean jumping) {
        horseJumping = jumping;
    }

    public void setJumpPower(int power) {
        jumpPower = power >= 90 ? 1.0F : 0.4F + 0.4F * power / 90.0F;
    }

    public boolean canBeSaddled() {
        return true;
    }

    public boolean wearsArmor() {
        return false;
    }

    public boolean isArmor(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return false;
    }

    public void onInventoryChanged(legacyconnector.v1_12_2.net.minecraft.inventory.IInventory inventory) {
    }

    @Override
    public boolean canBeSteered() {
        return getControllingPassenger() instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        if (!mcc$hosted()) super.fall(distance, damageMultiplier);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$horse() != null) return;
        tag.setBoolean("EatingHaystack", isEatingHaystack());
        tag.setBoolean("Bred", isBreeding());
        tag.setInteger("Temper", getTemper());
        tag.setBoolean("Tame", isTame());
        if (getOwnerUniqueId() != null) tag.setString("OwnerUUID", getOwnerUniqueId().toString());
        tag.setBoolean("Saddle", isHorseSaddled());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$horse() != null) return;
        setEatingHaystack(tag.getBoolean("EatingHaystack"));
        setBreeding(tag.getBoolean("Bred"));
        setTemper(tag.getInteger("Temper"));
        setHorseTamed(tag.getBoolean("Tame"));
        String owner = tag.getString("OwnerUUID");
        if (!owner.isEmpty()) setOwnerUniqueId(java.util.UUID.fromString(owner));
        setHorseSaddled(tag.getBoolean("Saddle"));
    }
}
