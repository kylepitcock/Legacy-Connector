package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/**
 * The colour of the particles of an entity's active effects (what its potion swirl looks like) is being worked out: a mod may change the
 * colour or hide the particles. Posted by {@link legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils#mcc$entityColor}
 * when the layer computes the colour for an entity view; a modern native entity's own swirl is drawn by the host and is not changed.
 */
@Cancelable
public class PotionColorCalculationEvent extends LivingEvent {
    private final int originalColor;
    private int color;
    private boolean hideParticle;
    private final Collection<PotionEffect> effects;

    public PotionColorCalculationEvent(EntityLivingBase entity, int color, boolean hideParticle, Collection<PotionEffect> effects) {
        super(entity);
        this.originalColor = color;
        this.color = color;
        this.effects = effects;
        this.hideParticle = hideParticle;
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

    public Collection<PotionEffect> getEffects() {
        return effects;
    }

    public int getOriginalColor() {
        return originalColor;
    }
}
