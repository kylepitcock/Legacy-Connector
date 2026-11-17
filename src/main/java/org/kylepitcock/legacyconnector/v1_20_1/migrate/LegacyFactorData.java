package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.20.1's {@code MobEffectInstance.FactorData} (the loader maps the class name to this one): the blend state of a fading effect (the darkness effect).
 * 1.21.1 computes the same blend in {@code MobEffectInstance.getBlendFactor}; mods that construct or read this object get a working copy of the old maths,
 * nothing in 1.21.1's own effects reads it.
 */
public class LegacyFactorData {
    public int paddingDuration;
    public float factorStart;
    public float factorTarget;
    public float factorCurrent;
    public int ticksActive;
    public float factorPreviousFrame;
    public boolean hadEffectLastTick;

    public LegacyFactorData(int paddingDuration, float factorStart, float factorTarget, float factorCurrent, int ticksActive, float factorPreviousFrame,
            boolean hadEffectLastTick) {
        this.paddingDuration = paddingDuration;
        this.factorStart = factorStart;
        this.factorTarget = factorTarget;
        this.factorCurrent = factorCurrent;
        this.ticksActive = ticksActive;
        this.factorPreviousFrame = factorPreviousFrame;
        this.hadEffectLastTick = hadEffectLastTick;
    }

    public LegacyFactorData() {
        this(0, 0.0F, 1.0F, 0.0F, 0, 0.0F, false);
    }

    public void update(MobEffectInstance instance) {
        this.factorPreviousFrame = this.factorCurrent;
        boolean has = instance.getDuration() > this.paddingDuration;
        if (this.hadEffectLastTick != has) {
            this.hadEffectLastTick = has;
            this.ticksActive = 0;
        }
        this.ticksActive++;
        float f = Mth.clamp((float) this.ticksActive / (float) this.paddingDuration, 0.0F, 1.0F);
        this.factorCurrent = Mth.lerp(f, this.factorStart, this.factorTarget);
        if (!has) this.factorTarget = 0.0F;
    }

    public float getFactor(LivingEntity entity, float partialTick) {
        return Mth.lerp(partialTick, this.factorPreviousFrame, this.factorCurrent);
    }
}
