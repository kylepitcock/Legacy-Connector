package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 iron golem: whether a player built it is bit 0 of watcher slot 16 (such golems never attack players). A plain one is the
 * modern iron golem; mods' golems run here.
 */
public class EntityIronGolem extends EntityGolem {
    /** View of a modern iron golem. */
    public EntityIronGolem(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityIronGolem(World world) {
        super(world);
        setSize(1.4F, 2.9F);
    }

    private net.minecraft.world.entity.animal.IronGolem mcc$golem() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.IronGolem g && !mcc$hosted() ? g : null;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(100.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.25D);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    public boolean isPlayerCreated() {
        var g = mcc$golem();
        if (g != null) return g.isPlayerCreated();
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    public void setPlayerCreated(boolean created) {
        var g = mcc$golem();
        if (g != null) {
            g.setPlayerCreated(created);
            return;
        }
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (created ? flags | 1 : flags & -2));
    }

    @Override
    public boolean canAttackClass(Class<?> cls) {
        if (isPlayerCreated() && legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class.isAssignableFrom(cls)) return false;
        return super.canAttackClass(cls);
    }

    /** Throws the target up into the air (7 to 21 damage). */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (mcc$golem() != null) return super.attackEntityAsMob(target);
        boolean hit = target.attackEntityFrom(legacyconnector.v1_7_10.net.minecraft.util.DamageSource.causeMobDamage(this), 7 + rand.nextInt(15));
        if (hit) mcc$set$motionY(target, mcc$get$motionY(target) + 0.4000000059604645D);
        playSound("mob.irongolem.throw", 1.0F, 1.0F);
        return hit;
    }

    @Override
    protected String getHurtSound() {
        return "mob.irongolem.hit";
    }

    @Override
    protected String getDeathSound() {
        return "mob.irongolem.death";
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("PlayerCreated", isPlayerCreated());
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setPlayerCreated(tag.getBoolean("PlayerCreated"));
    }
}
