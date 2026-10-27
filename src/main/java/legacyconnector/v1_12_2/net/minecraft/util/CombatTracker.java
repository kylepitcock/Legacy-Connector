package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

/**
 * 1.12.2 combat tracker: a view of the living entity's modern combat tracker (damage entries, death message, combat duration). Mods that
 * deal damage themselves record it with {@link #trackDamage} so the death message names the right cause.
 */
public class CombatTracker {
    private final EntityLivingBase fighter;

    public CombatTracker(EntityLivingBase fighter) {
        this.fighter = fighter;
    }

    private net.minecraft.world.damagesource.CombatTracker mcc$modern() {
        return fighter != null && fighter.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l.getCombatTracker() : null;
    }

    /** Fall suffixes are computed by the modern tracker when it builds the message. */
    public void calculateFallSuffix() {
    }

    /** {@code healthBefore} only served 1.12's own bookkeeping; the modern entry keeps the damage. */
    public void trackDamage(DamageSource source, float healthBefore, float damage) {
        var t = mcc$modern();
        if (t != null && source != null) t.recordDamage(source.mcc$modern(fighter.mcc$entity.level()), damage);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDeathMessage() {
        var t = mcc$modern();
        String text = t == null ? "" : t.getDeathMessage().getString();
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(text);
    }

    public EntityLivingBase getBestAttacker() {
        if (fighter == null || !(fighter.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l)) return null;
        var killer = l.getKillCredit();
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(killer) instanceof EntityLivingBase e ? e : null;
    }

    public int getCombatDuration() {
        var t = mcc$modern();
        return t == null ? 0 : t.getCombatDuration();
    }

    /** Ends a combat that is over (the modern tracker forgets its entries after the same 1.12 timeouts). */
    public void reset() {
        var t = mcc$modern();
        if (t != null) t.recheckStatus();
    }

    public EntityLivingBase getFighter() {
        return fighter;
    }
}
