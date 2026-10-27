package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Stays inside at night (no modern equivalent outside villages: never runs). */
public class EntityAIRestrictOpenDoor extends GoalTask1122 {
    public EntityAIRestrictOpenDoor(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature) {
        super(creature, 0, m -> null);
    }

}
