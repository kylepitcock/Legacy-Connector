package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Runs from nearby entities of a class (the modern avoid goal, filtered by the legacy class). */
public class EntityAIAvoidEntity extends GoalTask1122 {
    protected legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature entity;
    protected Class<?> classToAvoid;
    protected legacyconnector.v1_12_2.net.minecraft.entity.Entity closestLivingEntity;

    public EntityAIAvoidEntity(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, Class<?> classToAvoid, float avoidDistance, double farSpeed, double nearSpeed) {
        this(creature, classToAvoid, e -> true, avoidDistance, farSpeed, nearSpeed);
    }

    public EntityAIAvoidEntity(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, Class<?> classToAvoid, com.google.common.base.Predicate<?> selector, float avoidDistance,
            double farSpeed, double nearSpeed) {
        super(creature, 1, pathfinder(m -> new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(m, net.minecraft.world.entity.LivingEntity.class,
                legacyClass(classToAvoid).and(mcc$test(selector)), avoidDistance, farSpeed, nearSpeed,
                net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR::test)));
        this.entity = creature;
        this.classToAvoid = classToAvoid;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> mcc$test(com.google.common.base.Predicate selector) {
        if (selector == null) return e -> true;
        return e -> {
            try {
                return selector.apply(org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e));
            } catch (RuntimeException | LinkageError ex) {
                return false;
            }
        };
    }

}
