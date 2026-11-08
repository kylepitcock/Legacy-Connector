package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Vanilla Beta arrow impacts on the native arrow physics, tracking and rendering lane. */
public final class LegacyB173VanillaArrow extends Arrow {
    public LegacyB173VanillaArrow(EntityType<? extends Arrow> type,Level level){super(type,level);}
    public LegacyB173VanillaArrow(Level level){this(org.kylepitcock.legacyconnector.LegacyConnector.BETA_VANILLA_ARROW.get(),level);}

    @Override protected double getDefaultGravity(){return .03D;}
    @Override protected float getWaterInertia(){return .8F;}

    @Override protected void onHitEntity(EntityHitResult hit){
        // Original sl.w_: fixed damage attributed to the shooter, without modern critical,
        // enchantment, piercing, tipped-arrow or burning-arrow impact additions.
        if(hit.getEntity().hurt(damageSources().arrow(this,getOwner()),4F)){
            playSound(SoundEvents.ARROW_HIT,1F,1.2F/(random.nextFloat()*.2F+.9F));
            discard();
        }else{
            setDeltaMovement(getDeltaMovement().scale(-.1D));
            setYRot(getYRot()+180F);yRotO+=180F;
            // Beta resets its ticks-in-air counter here solely for shooter collision grace.
            // The native projectile retains its own owner-collision policy and flight age.
        }
    }
}
