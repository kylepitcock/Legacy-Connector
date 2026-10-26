package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** Damage dealt by an entity (a mob's or player's attack). */
public class EntityDamageSource extends DamageSource {
    protected Entity damageSourceEntity;
    private boolean isThornsDamage;

    public EntityDamageSource(String type, Entity source) {
        super(type);
        this.damageSourceEntity = source;
    }

    public EntityDamageSource setIsThornsDamage() {
        this.isThornsDamage = true;
        return this;
    }

    public boolean getIsThornsDamage() {
        return isThornsDamage;
    }

    @Override
    public Entity getTrueSource() {
        return damageSourceEntity;
    }

    @Override
    public boolean isDifficultyScaled() {
        return damageSourceEntity != null && damageSourceEntity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity
                && !(damageSourceEntity.mcc$entity instanceof net.minecraft.world.entity.player.Player);
    }
}
