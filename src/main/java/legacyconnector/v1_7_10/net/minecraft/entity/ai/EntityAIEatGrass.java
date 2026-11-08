package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;

/** The grazing goal (sheep): the modern {@link EatBlockGoal} does the eating; the timer drives the head animation. */
public class EntityAIEatGrass extends GoalTask {
    public EntityAIEatGrass(EntityLiving entity) {
        super(entity, 7, EatBlockGoal::new);
    }

    /** Ticks left of the eating animation (40 down to 0; 0 when not eating). */
    public int getEatingGrassTimer() {
        return goal() instanceof EatBlockGoal eat ? eat.getEatAnimationTick() : 0;
    }
}
