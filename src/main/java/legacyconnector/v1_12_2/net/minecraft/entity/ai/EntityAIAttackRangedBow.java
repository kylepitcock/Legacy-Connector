package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Shoots with a bow, strafing (the modern ranged bow goal for bow users). */
public class EntityAIAttackRangedBow extends GoalTask1122 {
    public EntityAIAttackRangedBow(legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityMob mob, double speed, int attackCooldown, float maxAttackDistance) {
        super(mob, 3, m -> m instanceof net.minecraft.world.entity.monster.Monster mon && m instanceof net.minecraft.world.entity.monster.RangedAttackMob
                ? mcc$bow(mon, speed, attackCooldown, maxAttackDistance) : null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static net.minecraft.world.entity.ai.goal.Goal mcc$bow(net.minecraft.world.entity.monster.Monster m, double speed, int cooldown, float distance) {
        return new net.minecraft.world.entity.ai.goal.RangedBowAttackGoal((net.minecraft.world.entity.monster.Monster & net.minecraft.world.entity.monster.RangedAttackMob) (Object) m,
                speed, cooldown, distance);
    }

    public void setAttackCooldown(int cooldown) {
    }

}
