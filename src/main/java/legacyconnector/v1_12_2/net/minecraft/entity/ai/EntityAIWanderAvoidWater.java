package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Wanders, staying out of water (the modern water-avoiding stroll goal). */
public class EntityAIWanderAvoidWater extends EntityAIWander {
    protected final float probability;

    public EntityAIWanderAvoidWater(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed) {
        this(creature, speed, 0.001F);
    }

    public EntityAIWanderAvoidWater(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed, float probability) {
        super(creature, speed, 120, pathfinder(m -> new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(m, speed, probability)));
        this.probability = probability;
    }
}
