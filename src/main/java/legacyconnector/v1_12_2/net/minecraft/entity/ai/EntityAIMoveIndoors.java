package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Walks indoors at night (no modern equivalent outside villages: never runs). */
public class EntityAIMoveIndoors extends GoalTask1122 {
    public EntityAIMoveIndoors(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature) {
        super(creature, 1, m -> null);
    }

}
