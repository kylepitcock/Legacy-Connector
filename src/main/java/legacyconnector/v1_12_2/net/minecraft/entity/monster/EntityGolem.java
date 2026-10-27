package legacyconnector.v1_12_2.net.minecraft.entity.monster;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 EntityGolem: a constructed creature (no fall damage, never despawns, silent). */
public abstract class EntityGolem extends EntityCreature implements legacyconnector.v1_12_2.net.minecraft.entity.passive.IAnimals {
    public EntityGolem(World world) {
        super(world);
    }

    protected EntityGolem(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        if (!mcc$hosted()) super.fall(distance, damageMultiplier);
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getHurtSound(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        return null;
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }
}
