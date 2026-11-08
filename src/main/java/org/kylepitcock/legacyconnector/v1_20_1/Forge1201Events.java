package org.kylepitcock.legacyconnector.v1_20_1;

import legacyconnector.v1_20_1.net.minecraftforge.event.entity.living.LivingAttackEvent;
import legacyconnector.v1_20_1.net.minecraftforge.event.entity.living.LivingDamageEvent;
import legacyconnector.v1_20_1.net.minecraftforge.event.entity.living.LivingEvent$LivingTickEvent;
import legacyconnector.v1_20_1.net.minecraftforge.event.entity.living.LivingHurtEvent;
import legacyconnector.v1_20_1.net.minecraftforge.event.entity.living.ShieldBlockEvent;
import legacyconnector.v1_20_1.net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import legacyconnector.v1_20_1.net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Semantic adapters for Forge 1.20.1 events that NeoForge 1.21.1 renamed, split or removed: the old event is posted on the NeoForge bus (where the translated
 * mods listen) from the event that replaces it, and the outcome (cancelled, changed amount, result) is applied to the new event.
 * <ul>
 *   <li>{@code LivingAttackEvent} then {@code LivingHurtEvent} from {@code LivingIncomingDamageEvent} (NeoForge merged both into it);</li>
 *   <li>{@code LivingDamageEvent} (1.20.1: after armor) from {@code LivingDamageEvent.Pre};</li>
 *   <li>{@code LivingTickEvent} from {@code EntityTickEvent.Pre}; {@code ShieldBlockEvent} from {@code LivingShieldBlockEvent};</li>
 *   <li>{@code EntityItemPickupEvent} from {@code ItemEntityPickupEvent.Pre}; {@code PlayerSleepInBedEvent} from {@code CanPlayerSleepEvent}.</li>
 * </ul>
 */
final class Forge1201Events {
    private Forge1201Events() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Events::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Events::onDamagePre);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Events::onEntityTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Events::onShieldBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Events::onItemPickup);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Events::onCanSleep);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Events::onSleepingTime);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingAttackEvent attack = new LivingAttackEvent(e.getEntity(), e.getSource(), e.getAmount());
        NeoForge.EVENT_BUS.post(attack);
        if (attack.isCanceled()) {
            e.setCanceled(true);
            return;
        }
        LivingHurtEvent hurt = new LivingHurtEvent(e.getEntity(), e.getSource(), e.getAmount());
        NeoForge.EVENT_BUS.post(hurt);
        if (hurt.isCanceled()) e.setCanceled(true);
        else if (hurt.getAmount() != e.getAmount()) e.setAmount(hurt.getAmount());
    }

    private static void onDamagePre(Pre e) {
        LivingDamageEvent damage = new LivingDamageEvent(e.getEntity(), e.getSource(), e.getNewDamage());
        NeoForge.EVENT_BUS.post(damage);
        e.setNewDamage(damage.isCanceled() ? 0 : damage.getAmount());
    }

    private static void onEntityTick(EntityTickEvent.Pre e) {
        if (!(e.getEntity() instanceof LivingEntity living)) return;
        if (NeoForge.EVENT_BUS.post(new LivingEvent$LivingTickEvent(living)).isCanceled()) e.setCanceled(true);
    }

    private static void onShieldBlock(LivingShieldBlockEvent e) {
        ShieldBlockEvent old = new ShieldBlockEvent(e.getEntity(), e.getDamageSource(), e.getBlockedDamage());
        NeoForge.EVENT_BUS.post(old);
        if (old.isCanceled()) {
            e.setBlocked(false);
            return;
        }
        e.setBlockedDamage(old.getBlockedDamage());
        if (!old.shieldTakesDamage()) e.setShieldDamage(0);
    }

    private static void onItemPickup(ItemEntityPickupEvent.Pre e) {
        EntityItemPickupEvent old = new EntityItemPickupEvent(e.getPlayer(), e.getItemEntity());
        NeoForge.EVENT_BUS.post(old);
        Event$Result result = old.getResult();
        if (old.isCanceled() || result == Event$Result.DENY) e.setCanPickup(TriState.FALSE);
        else if (result == Event$Result.ALLOW) e.setCanPickup(TriState.TRUE);
    }

    private static void onCanSleep(CanPlayerSleepEvent e) {
        PlayerSleepInBedEvent old = new PlayerSleepInBedEvent(e.getEntity(), e.getPos());
        NeoForge.EVENT_BUS.post(old);
        if (old.getResultStatus() != null) e.setProblem(old.getResultStatus());
    }

    private static void onSleepingTime(net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent e) {
        var sleepPos = e.getEntity().getSleepingPos();
        if (sleepPos.isPresent()) {
            var location = new legacyconnector.v1_20_1.net.minecraftforge.event.entity.player.SleepingLocationCheckEvent(e.getEntity(), sleepPos.get());
            NeoForge.EVENT_BUS.post(location);
            if (location.getResult() == Event$Result.ALLOW) e.setContinueSleeping(true);
            else if (location.getResult() == Event$Result.DENY) e.setContinueSleeping(false);
        }
        if (!(e.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        legacyconnector.v1_20_1.net.minecraftforge.event.entity.player.SleepingTimeCheckEvent old =
                new legacyconnector.v1_20_1.net.minecraftforge.event.entity.player.SleepingTimeCheckEvent(player, player.getSleepingPos());
        NeoForge.EVENT_BUS.post(old);
        if (old.getResult() == Event$Result.ALLOW) e.setContinueSleeping(true);
        else if (old.getResult() == Event$Result.DENY) e.setContinueSleeping(false);
    }
}
