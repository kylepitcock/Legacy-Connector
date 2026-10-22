package legacyconnector.v1_7_10.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

public class EntityDamageSource extends DamageSource {
    protected Entity damageSourceEntity;

    public EntityDamageSource(String type, Entity source) {
        super(type);
        this.damageSourceEntity = source;
    }

    @Override
    public Entity getEntity() {
        return damageSourceEntity;
    }
}
