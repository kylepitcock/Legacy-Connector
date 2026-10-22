package legacyconnector.v1_7_10.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

public class EntityDamageSourceIndirect extends EntityDamageSource {
    private final Entity indirectEntity;

    public EntityDamageSourceIndirect(String type, Entity direct, Entity indirect) {
        super(type, direct);
        this.indirectEntity = indirect;
    }

    @Override
    public Entity getSourceOfDamage() {
        return damageSourceEntity;
    }

    @Override
    public Entity getEntity() {
        return indirectEntity;
    }
}
