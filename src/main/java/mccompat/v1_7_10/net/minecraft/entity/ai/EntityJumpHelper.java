package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.Mob;

public class EntityJumpHelper {
    protected final EntityLiving entity;

    public EntityJumpHelper(EntityLiving entity) {
        this.entity = entity;
    }

    public void setJumping() {
        if (entity.mcc$entity instanceof Mob m) m.getJumpControl().jump();
    }

    public void doJump() {
    }
}
