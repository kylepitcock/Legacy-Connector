package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/** FML's marker for projectile entities that know who threw them (the thrower is credited for damage and kills). */
public interface IThrowableEntity {
    Entity getThrower();

    void setThrower(Entity entity);
}
