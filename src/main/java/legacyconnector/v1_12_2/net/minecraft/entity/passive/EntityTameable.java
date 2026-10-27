package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import com.google.common.base.Optional;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.IEntityOwnable;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.TamableAnimal;

/** 1.12.2 EntityTameable: owner, tamed and sitting (data the clients see). Views of modern tameable animals use the modern state. */
public abstract class EntityTameable extends EntityAnimal implements IEntityOwnable {
    protected static final DataParameter<Byte> TAMED = EntityDataManager.createKey(EntityTameable.class, DataSerializers.BYTE);
    protected static final DataParameter<Optional<UUID>> OWNER_UNIQUE_ID = EntityDataManager.createKey(EntityTameable.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    protected legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISit aiSit;

    public EntityTameable(World world) {
        super(world);
        setupTamedAI();
    }

    protected EntityTameable(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    private TamableAnimal mcc$tamable() {
        return mcc$entity instanceof TamableAnimal t ? t : null;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TAMED, (byte) 0);
        dataManager.register(OWNER_UNIQUE_ID, Optional.absent());
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$tamable() != null) return;
        tag.setString("OwnerUUID", getOwnerId() == null ? "" : getOwnerId().toString());
        tag.setBoolean("Sitting", isSitting());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$tamable() != null) return;
        String owner = tag.getString("OwnerUUID");
        if (!owner.isEmpty()) {
            try {
                setOwnerId(UUID.fromString(owner));
                setTamed(true);
            } catch (IllegalArgumentException e) {
                setTamed(false);
            }
        }
        if (aiSit != null) aiSit.setSitting(tag.getBoolean("Sitting"));
        setSitting(tag.getBoolean("Sitting"));
    }

    public boolean canBeLeashedTo(EntityPlayer player) {
        return !getLeashed();
    }

    protected void playTameEffect(boolean play) {
        if (mcc$entity == null || !(mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        var particle = play ? net.minecraft.core.particles.ParticleTypes.HEART : net.minecraft.core.particles.ParticleTypes.SMOKE;
        sl.sendParticles(particle, mcc$entity.getRandomX(1.0D), mcc$entity.getRandomY() + 0.5D, mcc$entity.getRandomZ(1.0D), 7, 0.2D, 0.2D, 0.2D, 0.02D);
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (mcc$hosted() && (id == 7 || id == 6)) {
            playTameEffect(id == 7);
            return;
        }
        super.handleStatusUpdate(id);
    }

    public boolean isTamed() {
        TamableAnimal t = mcc$tamable();
        if (t != null) return t.isTame();
        Byte b = dataManager.get(TAMED);
        return b != null && (b & 4) != 0;
    }

    public void setTamed(boolean tamed) {
        TamableAnimal t = mcc$tamable();
        if (t != null) {
            t.setTame(tamed, true);
            return;
        }
        Byte b = dataManager.get(TAMED);
        byte v = b == null ? 0 : b;
        dataManager.set(TAMED, (byte) (tamed ? v | 4 : v & -5));
        setupTamedAI();
    }

    protected void setupTamedAI() {
    }

    public boolean isSitting() {
        TamableAnimal t = mcc$tamable();
        if (t != null) return t.isOrderedToSit();
        Byte b = dataManager.get(TAMED);
        return b != null && (b & 1) != 0;
    }

    public void setSitting(boolean sitting) {
        TamableAnimal t = mcc$tamable();
        if (t != null) {
            t.setOrderedToSit(sitting);
            t.setInSittingPose(sitting);
            return;
        }
        Byte b = dataManager.get(TAMED);
        byte v = b == null ? 0 : b;
        dataManager.set(TAMED, (byte) (sitting ? v | 1 : v & -2));
    }

    @Override
    public UUID getOwnerId() {
        TamableAnimal t = mcc$tamable();
        if (t != null) return t.getOwnerUUID();
        Optional<UUID> id = dataManager.get(OWNER_UNIQUE_ID);
        return id == null ? null : id.orNull();
    }

    public void setOwnerId(UUID id) {
        TamableAnimal t = mcc$tamable();
        if (t != null) {
            t.setOwnerUUID(id);
            return;
        }
        dataManager.set(OWNER_UNIQUE_ID, Optional.fromNullable(id));
    }

    public void setTamedBy(EntityPlayer player) {
        setTamed(true);
        setOwnerId(player.getUniqueID());
        if (player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer sp && mcc$entity instanceof net.minecraft.world.entity.animal.Animal a) {
            net.minecraft.advancements.CriteriaTriggers.TAME_ANIMAL.trigger(sp, a);
        }
    }

    @Override
    public EntityLivingBase getOwner() {
        UUID id = getOwnerId();
        if (id == null || mcc$entity == null) return null;
        var p = mcc$entity.level().getPlayerByUUID(id);
        if (p != null) return (EntityLivingBase) org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p);
        if (mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel sl && sl.getEntity(id) instanceof net.minecraft.world.entity.LivingEntity l) {
            return (EntityLivingBase) org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(l);
        }
        return null;
    }

    public boolean isOwner(EntityLivingBase entity) {
        return entity != null && entity == getOwner();
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISit getAISit() {
        return aiSit;
    }

    public boolean shouldAttackEntity(EntityLivingBase target, EntityLivingBase owner) {
        return true;
    }

    @Override
    public boolean isOnSameTeam(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        if (isTamed()) {
            EntityLivingBase owner = getOwner();
            if (entity == owner) return true;
            if (owner != null) return owner.isOnSameTeam(entity);
        }
        return super.isOnSameTeam(entity);
    }

    @Override
    public void onDeath(legacyconnector.v1_12_2.net.minecraft.util.DamageSource cause) {
        if (mcc$hosted() && mcc$entity != null && !mcc$entity.level().isClientSide() && getOwner() instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP owner
                && mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            owner.mcc$serverPlayer().sendSystemMessage(l.getCombatTracker().getDeathMessage());
        }
        super.onDeath(cause);
    }
}
