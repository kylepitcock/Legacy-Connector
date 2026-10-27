package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** The zombie's melee attack (arms raised while attacking). */
public class EntityAIZombieAttack extends EntityAIAttackMelee {
    private int raiseArmTicks;

    public EntityAIZombieAttack(EntityCreature zombie, double speedIn, boolean longMemoryIn) {
        super(zombie, speedIn, longMemoryIn);
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        raiseArmTicks = 0;
    }

    @Override
    public void updateTask() {
        super.updateTask();
        ++raiseArmTicks;
    }
}
