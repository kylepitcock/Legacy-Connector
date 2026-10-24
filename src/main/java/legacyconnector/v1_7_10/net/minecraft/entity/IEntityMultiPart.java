package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.boss.EntityDragonPart;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** An entity made of several hit boxes (the dragons). */
public interface IEntityMultiPart {
    World func_82194_d();

    boolean attackEntityFromPart(EntityDragonPart part, DamageSource source, float damage);
}
