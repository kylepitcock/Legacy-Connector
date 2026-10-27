package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Potion effects being added to, checked on, expiring from or removed from a living entity. */
public class PotionEvent extends LivingEvent {
    protected final legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect;

    public PotionEvent(EntityLivingBase entity, legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect) {
        super(entity);
        this.effect = effect;
    }

    public legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect getPotionEffect() {
        return effect;
    }

    /** An effect is removed (cancel to keep it). */
    @Cancelable
    public static class PotionRemoveEvent extends PotionEvent {
        private final legacyconnector.v1_12_2.net.minecraft.potion.Potion potion;

        public PotionRemoveEvent(EntityLivingBase living, legacyconnector.v1_12_2.net.minecraft.potion.Potion potion) {
            super(living, living.getActivePotionEffect(potion));
            this.potion = potion;
        }

        public PotionRemoveEvent(EntityLivingBase living, legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect) {
            super(living, effect);
            this.potion = effect == null ? null : effect.getPotion();
        }

        public legacyconnector.v1_12_2.net.minecraft.potion.Potion getPotion() {
            return potion;
        }
    }

    /** Whether an effect can be applied (ALLOW, DENY, DEFAULT). */
    @Event.HasResult
    public static class PotionApplicableEvent extends PotionEvent {
        public PotionApplicableEvent(EntityLivingBase living, legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect) {
            super(living, effect);
        }
    }

    /** An effect was added. */
    public static class PotionAddedEvent extends PotionEvent {
        private final legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect oldEffect;

        public PotionAddedEvent(EntityLivingBase living, legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect oldEffect, legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect newEffect) {
            super(living, newEffect);
            this.oldEffect = oldEffect;
        }

        public legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect getOldPotionEffect() {
            return oldEffect;
        }
    }

    /** An effect ran out. */
    public static class PotionExpiryEvent extends PotionEvent {
        public PotionExpiryEvent(EntityLivingBase living, legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect) {
            super(living, effect);
        }
    }
}
