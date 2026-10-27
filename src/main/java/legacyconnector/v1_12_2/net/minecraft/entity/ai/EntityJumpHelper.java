package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.Mob;

/** The jump helper over the mob's modern jump control. */
public class EntityJumpHelper {
    private final EntityLiving entity;
    protected boolean isJumping;

    public EntityJumpHelper(EntityLiving entity) {
        this.entity = entity;
    }

    public void setJumping() {
        isJumping = true;
        if (getClass() == EntityJumpHelper.class && entity.mcc$entity instanceof Mob m) m.getJumpControl().jump();
    }

    /** Hands the jump to the entity (the modern jump control does this for the vanilla helper). */
    public void doJump() {
        if (getClass() == EntityJumpHelper.class) return;
        entity.setJumping(isJumping);
        isJumping = false;
    }
}
