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

/** 1.12.2 giant. */
public class EntityGiantZombie extends EntityMob {
    public EntityGiantZombie(World world) {
        super(world);
        setSize(3.6F, 11.7F);
    }

    protected EntityGiantZombie(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Giant mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Giant m ? m : null;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(50.0D);
    }

    @Override
    public float getEyeHeight() {
        return 10.440001F;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/giant");
    }
}
