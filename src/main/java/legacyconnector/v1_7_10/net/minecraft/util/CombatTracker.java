package legacyconnector.v1_7_10.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.entity.LivingEntity;

/**
 * Remembers who damaged an entity recently (for death messages). A view over the modern entity's own tracker, so damage
 * mods record here shows up in the vanilla death message and the other way round.
 */
public class CombatTracker {
    private final EntityLivingBase fighter;

    public CombatTracker(EntityLivingBase fighter) {
        this.fighter = fighter;
    }

    private net.minecraft.world.damagesource.CombatTracker mcc$modern() {
        return fighter != null && fighter.mcc$entity instanceof LivingEntity l ? l.getCombatTracker() : null;
    }

    /** Records damage about to be taken ({@code health} is the health before it). */
    public void func_94547_a(DamageSource source, float health, float damage) {
        var modern = mcc$modern();
        if (modern != null && source != null) modern.recordDamage(source.mcc$modern(fighter.mcc$entity.level()), damage);
    }

    /** Works out how a fall started (ladder, vines, water); the modern tracker does this when the damage is recorded. */
    public void func_94545_a() {
    }

    public IChatComponent func_151521_b() {
        var modern = mcc$modern();
        return modern == null ? new ChatComponentText("") : IChatComponent.mcc$fromModern(modern.getDeathMessage());
    }

    /** The living entity that did the most damage (the kill credit). */
    public EntityLivingBase func_94550_c() {
        if (fighter == null || !(fighter.mcc$entity instanceof LivingEntity l)) return null;
        return org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(l.getKillCredit()) instanceof EntityLivingBase e ? e : null;
    }

    /** Ends the fight once the entity has been left alone long enough. */
    public void func_94549_h() {
        var modern = mcc$modern();
        if (modern != null) modern.recheckStatus();
    }
}
