package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Runs away after being hurt or while burning (the modern panic goal). */
public class EntityAIPanic extends GoalTask1122 {
    protected final legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature;
    protected double speed;
    protected double randPosX;
    protected double randPosY;
    protected double randPosZ;

    public EntityAIPanic(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed) {
        super(creature, 1, pathfinder(m -> new net.minecraft.world.entity.ai.goal.PanicGoal(m, speed)));
        this.creature = creature;
        this.speed = speed;
    }

    protected boolean findRandomPosition() {
        var v = RandomPositionGenerator.findRandomTarget(creature, 5, 4);
        if (v == null) return false;
        randPosX = v.x;
        randPosY = v.y;
        randPosZ = v.z;
        return true;
    }

}
