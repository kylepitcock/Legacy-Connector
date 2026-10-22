package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import java.util.UUID;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.IEntityOwnable;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.LivingEntity;

/** 1.7.10 EntityTameable: tamed/sitting flags in watcher slot 16, owner UUID string in slot 17. */
public abstract class EntityTameable extends EntityAnimal implements IEntityOwnable {
    public EntityTameable(World world) {
        super(world);
    }

    protected EntityTameable(LivingEntity modern) {
        super(modern);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, Byte.valueOf((byte) 0));
        dataWatcher.addObject(17, "");
    }

    private byte flags() {
        if (mcc$entity instanceof net.minecraft.world.entity.TamableAnimal t && !mcc$hosted()) {
            return (byte) ((t.isTame() ? 4 : 0) | (t.isOrderedToSit() ? 1 : 0));
        }
        return dataWatcher.getWatchableObjectByte(16);
    }

    public boolean isTamed() {
        return (flags() & 4) != 0;
    }

    public void setTamed(boolean tamed) {
        byte f = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, Byte.valueOf((byte) (tamed ? f | 4 : f & ~4)));
    }

    public boolean isSitting() {
        return (flags() & 1) != 0;
    }

    public void setSitting(boolean sitting) {
        byte f = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, Byte.valueOf((byte) (sitting ? f | 1 : f & ~1)));
    }

    @Override
    public String func_152113_b() {
        if (mcc$entity instanceof net.minecraft.world.entity.TamableAnimal t && !mcc$hosted()) {
            return t.getOwnerUUID() == null ? "" : t.getOwnerUUID().toString();
        }
        return dataWatcher.getWatchableObjectString(17);
    }

    public void func_152115_b(String owner) {
        dataWatcher.updateObject(17, owner == null ? "" : owner);
    }

    @Override
    public EntityLivingBase getOwner() {
        String id = func_152113_b();
        if (id == null || id.isEmpty() || mcc$world() == null) return null;
        try {
            return mcc$world().func_152378_a(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean func_152114_e(EntityLivingBase entity) {
        return entity != null && entity == getOwner();
    }

    protected void playTameEffect(boolean success) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("OwnerUUID", func_152113_b() == null ? "" : func_152113_b());
        tag.setBoolean("Sitting", isSitting());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        String owner = tag.hasKey("OwnerUUID") ? tag.getString("OwnerUUID") : tag.getString("Owner");
        if (owner != null && !owner.isEmpty()) {
            func_152115_b(owner);
            setTamed(true);
        }
        setSitting(tag.getBoolean("Sitting"));
    }

    public boolean func_142018_a(EntityLivingBase target, EntityLivingBase owner) {
        return true;
    }
}
