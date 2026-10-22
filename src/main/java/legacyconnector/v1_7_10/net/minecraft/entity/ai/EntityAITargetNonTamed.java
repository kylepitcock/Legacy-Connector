package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityTameable;

public class EntityAITargetNonTamed extends EntityAINearestAttackableTarget {
    private final EntityTameable tameable;

    public EntityAITargetNonTamed(EntityTameable owner, Class<?> targetClass, int chance, boolean checkSight) {
        super(owner, targetClass, chance, checkSight);
        this.tameable = owner;
    }

    @Override
    public boolean shouldExecute() {
        return !tameable.isTamed() && super.shouldExecute();
    }
}
