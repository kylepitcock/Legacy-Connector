package legacyconnector.v1_12_2.net.minecraft.entity.boss.dragon.phase;

import legacyconnector.v1_12_2.net.minecraft.entity.MultiPartEntityPart;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** One phase of the ender dragon's fight (views of the modern phase instances). */
public interface IPhase {
    boolean getIsStationary();

    void doClientRenderEffects();

    void doLocalUpdate();

    void onCrystalDestroyed(legacyconnector.v1_12_2.net.minecraft.entity.item.EntityEnderCrystal crystal, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            DamageSource source, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player);

    void initPhase();

    void removeAreaEffect();

    float getMaxRiseOrFall();

    float getYawFactor();

    PhaseList<? extends IPhase> getType();

    Vec3d getTargetLocation();

    float getAdjustedDamage(MultiPartEntityPart part, DamageSource source, float damage);
}
