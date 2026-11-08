package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Forge 1.20.1's looting level event (looting is an enchantment effect in 1.21.1); a stand-in that is not fired. */
public class LootingLevelEvent extends LivingEvent {
    private final DamageSource damageSource;
    private int lootingLevel;

    public LootingLevelEvent(LivingEntity entity, DamageSource damageSource, int lootingLevel) {
        super(entity);
        this.damageSource = damageSource;
        this.lootingLevel = lootingLevel;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public int getLootingLevel() {
        return lootingLevel;
    }

    public void setLootingLevel(int lootingLevel) {
        this.lootingLevel = lootingLevel;
    }
}
