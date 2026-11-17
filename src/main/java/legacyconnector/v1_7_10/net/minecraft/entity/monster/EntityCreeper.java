package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 creeper: fuse state in watcher slot 16 (-1 idle, 1 lit), charged flag in slot 17, ignited (flint and steel) in slot 18.
 * A mod's creeper counts its fuse up here and explodes after {@code fuseTime} ticks; a view of a vanilla creeper uses the
 * modern creeper's own swell.
 */
public class EntityCreeper extends EntityMob {
    private int lastActiveTime;
    private int timeSinceIgnited;
    private int fuseTime = 30;
    private int explosionRadius = 3;

    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityCreeper(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityCreeper(World world) {
        super(world);
        // 1.7.10's constructor: a mod's creeper (Elemental Creepers) inherits this task list; without it the creeper never chases or swells
        tasks.addTask(1, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(2, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAICreeperSwell(this));
        tasks.addTask(3, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIAvoidEntity(this,
                legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot.class, 6.0F, 1.0D, 1.2D));
        tasks.addTask(4, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIAttackOnCollide(this, 1.0D, false));
        tasks.addTask(5, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIWander(this, 0.8D));
        tasks.addTask(6, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIWatchClosest(this,
                legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class, 8.0F));
        tasks.addTask(6, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAILookIdle(this));
        targetTasks.addTask(1, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAINearestAttackableTarget(this,
                legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class, 0, true));
        targetTasks.addTask(2, new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIHurtByTarget(this, false));
        setSize(0.6F, 1.8F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes.movementSpeed).setBaseValue(0.25D);
    }

    private net.minecraft.world.entity.monster.Creeper mcc$creeper() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Creeper c && !mcc$hosted() ? c : null;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) -1);
        dataWatcher.addObject(17, (byte) 0);
        dataWatcher.addObject(18, (byte) 0);
    }

    public boolean getPowered() {
        var c = mcc$creeper();
        if (c != null) return c.isPowered();
        return dataWatcher.getWatchableObjectByte(17) == 1;
    }

    public int getCreeperState() {
        var c = mcc$creeper();
        if (c != null) return c.getSwellDir();
        return dataWatcher.getWatchableObjectByte(16);
    }

    public void setCreeperState(int state) {
        var c = mcc$creeper();
        if (c != null) {
            c.setSwellDir(state);
            return;
        }
        dataWatcher.updateObject(16, (byte) state);
    }

    /** How far the fuse has burnt, 0..1 at {@code partialTicks} (the white flashing and swelling of the renderer). */
    public float getCreeperFlashIntensity(float partialTicks) {
        var c = mcc$creeper();
        if (c != null) return c.getSwelling(partialTicks);
        return (lastActiveTime + (timeSinceIgnited - lastActiveTime) * partialTicks) / (fuseTime - 2);
    }

    /** Whether the creeper was lit with flint and steel (it explodes whatever its target does). */
    public boolean func_146078_ca() {
        var c = mcc$creeper();
        if (c != null) return c.isIgnited();
        return dataWatcher.getWatchableObjectByte(18) != 0;
    }

    /** Lights the creeper as flint and steel does. */
    public void func_146079_cb() {
        var c = mcc$creeper();
        if (c != null) {
            c.ignite();
            return;
        }
        dataWatcher.updateObject(18, (byte) 1);
    }

    /** Counts a mod creeper's fuse (a vanilla creeper's own tick does this) and explodes it when the fuse ran out. */
    @Override
    public void onUpdate() {
        if (mcc$hosted() && isEntityAlive()) {
            lastActiveTime = timeSinceIgnited;
            if (func_146078_ca()) setCreeperState(1);
            int state = getCreeperState();
            if (state > 0 && timeSinceIgnited == 0) playSound("creeper.primed", 1.0F, 0.5F);
            timeSinceIgnited += state;
            if (timeSinceIgnited < 0) timeSinceIgnited = 0;
            if (timeSinceIgnited >= fuseTime) {
                timeSinceIgnited = fuseTime;
                func_146077_cc();
            }
        }
        super.onUpdate();
    }

    /** A fall burns part of the fuse (at most to five ticks before the end). */
    @Override
    protected void fall(float distance) {
        super.fall(distance);
        timeSinceIgnited = (int) (timeSinceIgnited + distance * 1.5F);
        if (timeSinceIgnited > fuseTime - 5) timeSinceIgnited = fuseTime - 5;
    }

    /** Lightning charges the creeper. */
    @Override
    public void onStruckByLightning(legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt bolt) {
        super.onStruckByLightning(bolt);
        if (mcc$creeper() == null) dataWatcher.updateObject(17, (byte) 1);
    }

    @Override
    protected String getHurtSound() {
        return "mob.creeper.say";
    }

    @Override
    protected String getDeathSound() {
        return "mob.creeper.death";
    }

    /** Blows the creeper up (private in 1.7.10 but named in mods that override it). */
    protected void func_146077_cc() {
        if (mcc$world() != null && !mcc$world().isRemote) {
            float power = getPowered() ? 2.0F : 1.0F;
            boolean griefing = mcc$world().getGameRules().getGameRuleBooleanValue("mobGriefing");
            mcc$world().createExplosion(this, mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), explosionRadius * power, griefing);
            setDead();
        }
    }

    /** 1.7.10: the task AI (mods' subclasses inherit it: their walking speed follows the move helper). */
    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (dataWatcher.getWatchableObjectByte(17) == 1) tag.setBoolean("powered", true);
        tag.setShort("Fuse", (short) fuseTime);
        tag.setByte("ExplosionRadius", (byte) explosionRadius);
        tag.setBoolean("ignited", func_146078_ca());
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$creeper() == null) dataWatcher.updateObject(17, (byte) (tag.getBoolean("powered") ? 1 : 0));
        if (tag.hasKey("Fuse")) fuseTime = tag.getShort("Fuse");
        if (tag.hasKey("ExplosionRadius")) explosionRadius = tag.getByte("ExplosionRadius");
        if (tag.getBoolean("ignited")) func_146079_cb();
    }
}
