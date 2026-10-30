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
    /** Ticks of galloping, for the gallop sound (plain state: the modern counter is the host's own). */
    protected int gallopTime;
    protected legacyconnector.v1_12_2.net.minecraft.inventory.ContainerHorseChest horseChest;

    public AbstractHorse(World world) {
        super(world);
        setSize(1.3964844F, 1.6F);
        mcc$set$stepHeight(this, 1.0F);
        try {
            initHorseChest();
        } catch (RuntimeException | LinkageError e) {
            // a subclass's inventory size needing state its constructor sets later: made again on first use
        }
    }

    // ------------------------------------------------------------------ inventory

    /** Slots of the horse's inventory: saddle and armor. */
    protected int getInventorySize() {
        return 2;
    }

    /** (Re)makes the inventory at the current size, keeping what fits, and listens to it (saddle state). */
    protected void initHorseChest() {
        legacyconnector.v1_12_2.net.minecraft.inventory.ContainerHorseChest old = horseChest;
        horseChest = new legacyconnector.v1_12_2.net.minecraft.inventory.ContainerHorseChest("HorseChest", getInventorySize());
        horseChest.setCustomName(getName());
        if (old != null) {
            old.removeInventoryChangeListener(this);
            int n = Math.min(old.getSizeInventory(), horseChest.getSizeInventory());
            for (int i = 0; i < n; ++i) {
                var stack = old.getStackInSlot(i);
                if (!stack.isEmpty()) horseChest.setInventorySlotContents(i, stack.copy());
            }
        } else {
            var h = mcc$horse();
            if (h != null) {
                // a vanilla horse: start from its saddle and armor
                if (h.isSaddled()) horseChest.setInventorySlotContents(0, new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(
                        legacyconnector.v1_12_2.net.minecraft.init.Items.SADDLE));
                if (horseChest.getSizeInventory() > 1 && !h.getBodyArmorItem().isEmpty()) {
                    horseChest.setInventorySlotContents(1, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(h.getBodyArmorItem()));
                }
            }
        }
        horseChest.addInventoryChangeListener(this);
        updateHorseSlots();
    }

    /** Saddled while the saddle slot holds something. */
    protected void updateHorseSlots() {
        var world = mcc$get$world(this);
        if (world != null && !world.isRemote && mcc$horse() == null && horseChest != null) {
            setHorseSaddled(!horseChest.getStackInSlot(0).isEmpty() && canBeSaddled());
        }
    }

    /** Opens the horse's inventory for its rider or owner (a tamed horse). */
    public void openGUI(EntityPlayer player) {
        var world = mcc$get$world(this);
        if (world == null || world.isRemote || player == null || !isTame()) return;
        if (isBeingRidden() && !isPassenger(player)) return;
        var h = mcc$horse();
        if (h != null && player.mcc$player() != null) h.openCustomInventoryScreen(player.mcc$player());
    }

    // ------------------------------------------------------------------ riding, mood, eating, breeding

    /** The player takes the horse's facing and mounts it. */
    protected void mountTo(EntityPlayer player) {
        mcc$set$rotationYaw(player, mcc$get$rotationYaw(this));
        mcc$set$rotationPitch(player, mcc$get$rotationPitch(this));
        setEatingHaystack(false);
        setRearing(false);
        var world = mcc$get$world(this);
        if (world != null && !world.isRemote) player.startRiding(this);
    }

    /** Rears up with its angry sound (bucking a rider off). */
    public void makeMad() {
        var h = mcc$horse();
        if (h != null) {
            h.makeMad();
            return;
        }
        setRearing(true);
        var sound = getAngrySound();
        if (sound != null) playSound(sound, getSoundVolume(), getSoundPitch());
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getAngrySound() {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.HORSE_ANGRY);
    }

    /** Feeding: heals, grows foals, raises temper and puts tame adults in love (golden food), as 1.12's horse food did. */
    protected boolean handleEating(EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        boolean ate = false;
        float heal = 0.0F;
        int growth = 0;
        int temperGain = 0;
        Object item = stack == null || stack.isEmpty() ? null : stack.getItem().mcc$modern;
        if (item == net.minecraft.world.item.Items.WHEAT) {
            heal = 2.0F;
            growth = 20;
            temperGain = 3;
        } else if (item == net.minecraft.world.item.Items.SUGAR) {
            heal = 1.0F;
            growth = 30;
            temperGain = 3;
        } else if (item == net.minecraft.world.item.Items.HAY_BLOCK) {
            heal = 20.0F;
            growth = 180;
        } else if (item == net.minecraft.world.item.Items.APPLE) {
            heal = 3.0F;
            growth = 60;
            temperGain = 3;
        } else if (item == net.minecraft.world.item.Items.GOLDEN_CARROT || item == net.minecraft.world.item.Items.GOLDEN_APPLE) {
            boolean apple = item == net.minecraft.world.item.Items.GOLDEN_APPLE;
            heal = apple ? 10.0F : 4.0F;
            growth = apple ? 240 : 60;
            temperGain = apple ? 10 : 5;
            if (isTame() && getGrowingAge() == 0 && !isInLove()) {
                ate = true;
                setInLove(player);
            }
        }
        if (getHealth() < getMaxHealth() && heal > 0.0F) {
            heal(heal);
            ate = true;
        }
        var world = mcc$get$world(this);
        boolean server = world != null && !world.isRemote;
        if (isChild() && growth > 0) {
            if (server) addGrowth(growth);
            ate = true;
        }
        if (temperGain > 0 && (ate || !isTame()) && getTemper() < getMaxTemper()) {
            ate = true;
            if (server) increaseTemper(temperGain);
        }
        if (ate && !isSilent()) {
            playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.HORSE_EAT), 1.0F,
                    1.0F + (getRNG().nextFloat() - getRNG().nextFloat()) * 0.2F);
        }
        return ate;
    }

    /** Ready to breed: a tame, healthy, in-love adult nobody rides. */
    protected boolean canMate() {
        return !isBeingRidden() && !isRiding() && isTame() && !isChild() && getHealth() >= getMaxHealth() && isInLove();
    }

    /** Random max health for a foal's average (15 to 30). */
    protected float getModifiedMaxHealth() {
        return 15.0F + getRNG().nextInt(8) + getRNG().nextInt(9);
    }

    /** Random jump strength for a foal's average (0.4 to 1.0). */
    protected double getModifiedJumpStrength() {
        return 0.4000000059604645D + getRNG().nextDouble() * 0.2D + getRNG().nextDouble() * 0.2D + getRNG().nextDouble() * 0.2D;
    }

    /** Random speed for a foal's average (0.1125 to 0.3375). */
    protected double getModifiedMovementSpeed() {
        return (0.44999998807907104D + getRNG().nextDouble() * 0.3D + getRNG().nextDouble() * 0.3D + getRNG().nextDouble() * 0.3D) * 0.25D;
    }

    /** A foal's health, jump strength and speed: the average of both parents and a random value. */
    protected void setOffspringAttributes(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable mate, AbstractHorse child) {
        double health = getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue()
                + mate.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() + getModifiedMaxHealth();
        child.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(health / 3.0D);
        var jump = getEntityAttribute(JUMP_STRENGTH);
        var mateJump = mate.getEntityAttribute(JUMP_STRENGTH);
        var childJump = child.getEntityAttribute(JUMP_STRENGTH);
        if (jump != null && mateJump != null && childJump != null) {
            childJump.setBaseValue((jump.getBaseValue() + mateJump.getBaseValue() + getModifiedJumpStrength()) / 3.0D);
        }
        double speed = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue()
                + mate.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() + getModifiedMovementSpeed();
        child.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(speed / 3.0D);
    }

    protected AbstractHorse(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    /** A foal (not eating) walks towards the nearest grown horse within 16 blocks once it is more than 2 blocks away. */
    protected void followMother() {
        var h = mcc$horse();
        if (h != null) {
            org.kylepitcock.legacyconnector.v1_12_2.entity.ModernAccess1122.followMommy(h);
            return;
        }
        var world = mcc$get$world(this);
        if (world == null || !isChild()) return;
        AbstractHorse closest = null;
        double best = Double.MAX_VALUE;
        for (Object o : world.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().grow(16.0D))) {
            if (o instanceof AbstractHorse other && !other.isChild()) {
                double d = getDistanceSq(other);
                if (d < best) {
                    best = d;
                    closest = other;
                }
            }
        }
        if (closest != null && best > 4.0D) getNavigator().tryMoveToEntityLiving(closest, 1.0D);
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
