package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** Damage dealt through another entity (an arrow, a fireball) on behalf of its shooter. */
public class EntityDamageSourceIndirect extends EntityDamageSource {
    private final Entity indirectEntity;

    public EntityDamageSourceIndirect(String type, Entity source, Entity indirect) {
        super(type, source);
        this.indirectEntity = indirect;
    }

    @Override
    public Entity getImmediateSource() {
        return damageSourceEntity;
    }

    @Override
    public Entity getTrueSource() {
        return indirectEntity;
    }
}
