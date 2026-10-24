package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;

/** Walk through a village's doors. The layer reports no villages, so this task never starts. */
public class EntityAIMoveThroughVillage extends EntityAIBase {
    public EntityAIMoveThroughVillage(EntityCreature entity, double speed, boolean nightOnly) {
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }
}
