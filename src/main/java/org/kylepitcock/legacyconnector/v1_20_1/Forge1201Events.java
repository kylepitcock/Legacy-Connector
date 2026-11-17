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
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Semantic adapters for Forge 1.20.1 events that NeoForge 1.21.1 renamed, split or removed: the old event is posted on the NeoForge bus (where the translated
 * mods listen) from the event that replaces it, and the outcome (cancelled, changed amount, result) is applied to the new event.
 * <ul>
 *   <li>{@code LivingAttackEvent} then {@code LivingHurtEvent} from {@code LivingIncomingDamageEvent} (NeoForge merged both into it);</li>
 *   <li>{@code LivingDamageEvent} (1.20.1: after armor) from {@code LivingDamageEvent.Pre};</li>
 *   <li>{@code LivingTickEvent} from {@code EntityTickEvent.Pre}; {@code ShieldBlockEvent} from {@code LivingShieldBlockEvent};</li>
 *   <li>{@code EntityItemPickupEvent} from {@code ItemEntityPickupEvent.Pre}; {@code PlayerSleepInBedEvent} from {@code CanPlayerSleepEvent};</li>
 *   <li>{@code EntityEvent.EyeHeight} from {@code EntityEvent.Size} (the eye height is part of the dimensions now);</li>
 *   <li>{@code PotionColorCalculationEvent} from {@code EffectParticleModificationEvent} (per effect instead of per entity);</li>
 *   <li>the result (ALLOW/DENY/DEFAULT) a mod set with Forge's {@code Event.setResult} on {@code CropGrowEvent.Pre} and {@code CreateFluidSourceEvent}
 *   (classes renamed by {@code forge-curated-renames.tsv}) becomes the NeoForge result of the same event (a LOWEST listener, after the mods').</li>
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
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, Forge1201Events::onEntitySize);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Forge1201Events::onEffectParticles);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Forge1201Events::onCropGrowPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Forge1201Events::onCreateFluidSource);
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

    private static void onEntitySize(net.neoforged.neoforge.event.entity.EntityEvent.Size e) {
        net.minecraft.world.entity.EntityDimensions size = e.getNewSize();
        if (size == null) return;
        legacyconnector.v1_20_1.net.minecraftforge.event.entity.EntityEvent$EyeHeight eye =
                new legacyconnector.v1_20_1.net.minecraftforge.event.entity.EntityEvent$EyeHeight(e.getEntity(), e.getPose(), size, size.eyeHeight());
        NeoForge.EVENT_BUS.post(eye);
        if (eye.getNewHeight() != eye.getOldHeight()) e.setNewSize(size.withEyeHeight(eye.getNewHeight()));
    }

    /** 1.21.1 asks per effect (particle options + visibility); 1.20.1 asked per entity (one blended colour). The old event gets the one effect and its colour. */
    private static void onEffectParticles(net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent e) {
        var options = e.getParticleOptions();
        if (!(options instanceof net.minecraft.core.particles.ColorParticleOption color)) return;
        int rgb = (Math.round(color.getRed() * 255.0F) << 16) | (Math.round(color.getGreen() * 255.0F) << 8) | Math.round(color.getBlue() * 255.0F);
        var old = new legacyconnector.v1_20_1.net.minecraftforge.event.entity.living.PotionColorCalculationEvent(
                e.getEntity(), rgb, !e.isVisible(), java.util.List.of(e.getEffect()));
        NeoForge.EVENT_BUS.post(old);
        if (old.getColor() != rgb) e.setParticleOptions(net.minecraft.core.particles.ColorParticleOption.create(color.getType(), 0xFF000000 | old.getColor()));
        if (old.areParticlesHidden() == e.isVisible()) e.setVisible(!old.areParticlesHidden());
    }

    private static void onCropGrowPre(CropGrowEvent.Pre e) {
        Event$Result result = legacyconnector.v1_20_1.ForgeApi1201.eventGetResult(e);
        if (result == Event$Result.ALLOW) e.setResult(CropGrowEvent.Pre.Result.GROW);
        else if (result == Event$Result.DENY) e.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    private static void onCreateFluidSource(CreateFluidSourceEvent e) {
        Event$Result result = legacyconnector.v1_20_1.ForgeApi1201.eventGetResult(e);
        if (result == Event$Result.ALLOW) e.setCanConvert(true);
        else if (result == Event$Result.DENY) e.setCanConvert(false);
    }
}
