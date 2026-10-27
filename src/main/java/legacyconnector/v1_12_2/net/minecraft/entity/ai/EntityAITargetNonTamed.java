package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Targets like {@link EntityAINearestAttackableTarget} while the tameable owner is not tamed. */
public class EntityAITargetNonTamed<T extends EntityLivingBase> extends EntityAINearestAttackableTarget<T> {
    private final legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable tameable;

    public EntityAITargetNonTamed(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable entity, Class<T> classTarget, boolean checkSight,
            com.google.common.base.Predicate<? super T> targetSelector) {
        super(entity, classTarget, 10, checkSight, false, targetSelector);
        this.tameable = entity;
    }

    @Override
    public boolean shouldExecute() {
        return !tameable.isTamed() && super.shouldExecute();
    }
}
