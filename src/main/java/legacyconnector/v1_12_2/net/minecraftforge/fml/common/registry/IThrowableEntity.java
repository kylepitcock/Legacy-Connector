package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** A projectile that knows who threw it (FML sent the thrower's id with the spawn). */
public interface IThrowableEntity {
    Entity getThrower();

    void setThrower(Entity entity);
}
