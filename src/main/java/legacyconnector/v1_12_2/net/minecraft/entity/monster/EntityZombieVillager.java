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

/** 1.12.2 zombie villager: keeps the villager profession it had and can be cured. */
public class EntityZombieVillager extends EntityZombie {
    public EntityZombieVillager(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    protected EntityZombieVillager(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.ZombieVillager mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.ZombieVillager m ? m : null;
    }

    private static final DataParameter<Boolean> CONVERTING = EntityDataManager.createKey(EntityZombieVillager.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> PROFESSION = EntityDataManager.createKey(EntityZombieVillager.class, DataSerializers.VARINT);
    @Deprecated
    public int conversionTime;
    private int mcc$conversionTime;

    public static int mcc$get$conversionTime(EntityZombieVillager e) {
        return e.mcc$conversionTime;
    }

    public static void mcc$set$conversionTime(EntityZombieVillager e, int v) {
        e.mcc$conversionTime = v;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(CONVERTING, false);
        dataManager.register(PROFESSION, 0);
    }

    public void setProfession(int profession) {
        var m = mcc$self();
        if (m != null) {
            m.setVillagerData(m.getVillagerData().setProfession(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager.mcc$modernProfession(profession)));
            return;
        }
        dataManager.set(PROFESSION, profession);
    }

    public int getProfession() {
        var m = mcc$self();
        if (m != null) return legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager.mcc$legacyProfession(m.getVillagerData().getProfession());
        Integer p = dataManager.get(PROFESSION);
        return p == null ? 0 : Math.max(p % 6, 0);
    }

    public boolean isConverting() {
        var m = mcc$self();
        return m != null ? m.isConverting() : Boolean.TRUE.equals(dataManager.get(CONVERTING));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() != null) return;
        tag.setInteger("Profession", getProfession());
        tag.setInteger("ConversionTime", isConverting() ? mcc$conversionTime : -1);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() != null) return;
        setProfession(tag.getInteger("Profession"));
        if (tag.hasKey("ConversionTime", 99) && tag.getInteger("ConversionTime") > -1) {
            mcc$conversionTime = tag.getInteger("ConversionTime");
            dataManager.set(CONVERTING, true);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.ZOMBIE_VILLAGER_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.ZOMBIE_VILLAGER_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.ZOMBIE_VILLAGER_DEATH);
    }

    @Override
    protected SoundEvent getStepSound() {
        return legacy(SoundEvents.ZOMBIE_VILLAGER_STEP);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/zombie_villager");
    }
}
