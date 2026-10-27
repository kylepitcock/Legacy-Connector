package legacyconnector.v1_12_2.net.minecraft.entity;

/** An entity made of several hit boxes (the dragon): hits on a part go to the whole. */
public interface IEntityMultiPart {
    legacyconnector.v1_12_2.net.minecraft.world.World getWorld();

    boolean attackEntityFromPart(MultiPartEntityPart part, legacyconnector.v1_12_2.net.minecraft.util.DamageSource source, float damage);
}
