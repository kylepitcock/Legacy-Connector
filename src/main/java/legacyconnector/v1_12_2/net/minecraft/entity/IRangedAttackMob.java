package legacyconnector.v1_12_2.net.minecraft.entity;

/** A mob that shoots (skeletons, witches, snow golems): the ranged attack goals call these. */
public interface IRangedAttackMob {
    void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor);

    void setSwingingArms(boolean swingingArms);
}
