package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Melee: path to the attack target and hit it in reach (a target class, if given, limits what it attacks). */
public class EntityAIAttackOnCollide extends GoalTask {
    private final Class<?> classTarget;

    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIAttackOnCollide(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e, float speed, boolean m) {
        this(mcc$creature(e), (double) speed, m);
    }

    public EntityAIAttackOnCollide(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e, Class<?> cls, float speed, boolean m) {
        this(mcc$creature(e), cls, (double) speed, m);
    }

    public EntityAIAttackOnCollide(EntityCreature entity, Class<?> targetClass, double speed, boolean longMemory) {
        super(entity, 3, m -> new MeleeAttackGoal(m, speed, longMemory));
        this.classTarget = targetClass;
    }

    public EntityAIAttackOnCollide(EntityCreature entity, double speed, boolean longMemory) {
        this(entity, null, speed, longMemory);
    }

    @Override
    public boolean shouldExecute() {
        var target = owner.getAttackTarget();
        if (classTarget != null && target != null && !classTarget.isInstance(target)) return false;
        return super.shouldExecute();
    }
}
