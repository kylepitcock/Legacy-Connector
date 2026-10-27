package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Eats grass (the modern eat-block goal). */
public class EntityAIEatGrass extends GoalTask1122 {
    private final legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving grassEaterEntity;

    public EntityAIEatGrass(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity) {
        super(entity, 7, net.minecraft.world.entity.ai.goal.EatBlockGoal::new);
        this.grassEaterEntity = entity;
    }

    public int getEatingGrassTimer() {
        return mcc$goal() instanceof net.minecraft.world.entity.ai.goal.EatBlockGoal g ? g.getEatAnimationTick() : 0;
    }

}
