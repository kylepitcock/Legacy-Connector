package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import java.util.Collection;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Forge 1.20.1's potion colour event (the colour and visibility of the effect particles of an entity, computed from its active effects). 1.21.1 asks per
 * effect ({@code EffectParticleModificationEvent}: particle options plus a visibility flag); the layer posts this event from it with that one effect in
 * the list, the colour of the effect's particle as {@code getColor()} and applies the colour and the hide flag back (Forge1201Events).
 */
public class PotionColorCalculationEvent extends LivingEvent {
    private final int originalColor;
    private final boolean originalHideParticle;
    private final Collection<MobEffectInstance> effects;
    private int color;
    private boolean hideParticle;

    public PotionColorCalculationEvent(LivingEntity entity, int color, boolean hideParticle, Collection<MobEffectInstance> effects) {
        super(entity);
        this.originalColor = color;
        this.originalHideParticle = hideParticle;
        this.color = color;
        this.hideParticle = hideParticle;
        this.effects = effects;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean areParticlesHidden() {
        return hideParticle;
    }

    public void shouldHideParticles(boolean hideParticle) {
        this.hideParticle = hideParticle;
    }

    public void setHideParticle(boolean hideParticle) {
        this.hideParticle = hideParticle;
    }

    public Collection<MobEffectInstance> getEffects() {
        return effects;
    }

    public int getOriginalColor() {
        return originalColor;
    }

    public boolean getOriginalHideParticle() {
        return originalHideParticle;
    }
}
