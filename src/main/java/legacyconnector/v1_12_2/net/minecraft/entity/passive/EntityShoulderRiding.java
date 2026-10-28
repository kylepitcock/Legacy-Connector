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

/** 1.12.2 tameable that rides on its owner's shoulder (parrots). */
public abstract class EntityShoulderRiding extends EntityTameable {
    private int rideCooldownCounter;

    public EntityShoulderRiding(World world) {
        super(world);
    }

    protected EntityShoulderRiding(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    /** Climbs onto the player's shoulder (the modern parrot does it itself for views). */
    public boolean setEntityOnShoulder(EntityPlayer player) {
        var m = mcc$entity;
        if (m instanceof net.minecraft.world.entity.animal.ShoulderRidingEntity s && player != null
                && player.mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp) {
            return s.setEntityOnShoulder(sp);
        }
        return false;
    }

    public boolean canSitOnShoulder() {
        return rideCooldownCounter > 100;
    }

    @Override
    public void onUpdate() {
        ++rideCooldownCounter;
        super.onUpdate();
    }
}
