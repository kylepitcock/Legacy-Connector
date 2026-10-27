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

/** 1.12.2 cave spider: smaller, poisons what it bites. */
public class EntityCaveSpider extends EntitySpider {
    public EntityCaveSpider(World world) {
        super(world);
        setSize(0.7F, 0.5F);
    }

    protected EntityCaveSpider(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.CaveSpider mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.CaveSpider m ? m : null;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(12.0D);
    }

    @Override
    public boolean attackEntityAsMob(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && mcc$entity != null && target != null && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            int seconds = switch (mcc$entity.level().getDifficulty()) {
                case NORMAL -> 7;
                case HARD -> 15;
                default -> 0;
            };
            if (seconds > 0) l.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, seconds * 20, 0), mcc$entity);
        }
        return hit;
    }

    @Override
    public float getEyeHeight() {
        return 0.45F;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/cave_spider");
    }
}
