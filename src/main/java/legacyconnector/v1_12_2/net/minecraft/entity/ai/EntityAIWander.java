package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Walks to random spots now and then (the modern random stroll goal). */
public class EntityAIWander extends GoalTask1122 {
    protected final legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature entity;
    protected double speed;
    protected int executionChance;

    public EntityAIWander(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed) {
        this(creature, speed, 120);
    }

    public EntityAIWander(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed, int chance) {
        this(creature, speed, chance, pathfinder(m -> new net.minecraft.world.entity.ai.goal.RandomStrollGoal(m, speed, chance)));
    }

    protected EntityAIWander(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed, int chance,
            java.util.function.Function<net.minecraft.world.entity.Mob, net.minecraft.world.entity.ai.goal.Goal> goal) {
        super(creature, 1, goal);
        this.entity = creature;
        this.speed = speed;
        this.executionChance = chance;
    }

    public void makeUpdate() {
    }

    public void setExecutionChance(int chance) {
        this.executionChance = chance;
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getPosition() {
        return RandomPositionGenerator.findRandomTarget(entity, 10, 7);
    }

}
