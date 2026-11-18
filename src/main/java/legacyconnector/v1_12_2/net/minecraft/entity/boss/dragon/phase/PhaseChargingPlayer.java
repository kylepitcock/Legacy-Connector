package legacyconnector.v1_12_2.net.minecraft.entity.boss.dragon.phase;

import legacyconnector.v1_12_2.net.minecraft.entity.MultiPartEntityPart;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** The dragon's charge at a player: a phase that also takes the point it rushes to (the modern charge phase's target). */
public class PhaseChargingPlayer implements IPhase {
    private final IPhase base;
    private final net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase modern;

    PhaseChargingPlayer(IPhase base, net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase modern) {
        this.base = base;
        this.modern = modern;
    }

    /** Sets the point the dragon charges to. */
    public void setTarget(Vec3d target) {
        if (target != null) modern.setTarget(new net.minecraft.world.phys.Vec3(target.x, target.y, target.z));
    }

    @Override
    public boolean getIsStationary() {
        return base.getIsStationary();
    }

    @Override
    public void doClientRenderEffects() {
        base.doClientRenderEffects();
    }

    @Override
    public void doLocalUpdate() {
        base.doLocalUpdate();
    }

    @Override
    public void onCrystalDestroyed(legacyconnector.v1_12_2.net.minecraft.entity.item.EntityEnderCrystal crystal, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, DamageSource source, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        base.onCrystalDestroyed(crystal, pos, source, player);
    }

    @Override
    public void initPhase() {
        base.initPhase();
    }

    @Override
    public void removeAreaEffect() {
        base.removeAreaEffect();
    }

    @Override
    public float getMaxRiseOrFall() {
        return base.getMaxRiseOrFall();
    }

    @Override
    public float getYawFactor() {
        return base.getYawFactor();
    }

    @Override
    public PhaseList<? extends IPhase> getType() {
        return base.getType();
    }

    @Override
    public Vec3d getTargetLocation() {
        return base.getTargetLocation();
    }

    @Override
    public float getAdjustedDamage(MultiPartEntityPart part, DamageSource source, float damage) {
        return base.getAdjustedDamage(part, source, damage);
    }
}
