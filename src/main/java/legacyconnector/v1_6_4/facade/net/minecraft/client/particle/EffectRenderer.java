package legacyconnector.v1_6_4.facade.net.minecraft.client.particle;

/** Facade of the 1.7.10 shim's {@code EffectRenderer} for 1.4.7 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EffectRenderer {
    private EffectRenderer() {}

    /**
     * Forge 1.4.7 {@code addEffect(effect, source)}: the second argument was the object (usually the block) whose texture file the particle
     * draws from, so the renderer could bind that file for it. Sprites live on the one atlas here, so there is nothing to bind: the particle is added.
     */
    public static void addEffect(legacyconnector.v1_7_10.net.minecraft.client.particle.EffectRenderer self,
            legacyconnector.v1_7_10.net.minecraft.client.particle.EntityFX effect, Object source) {
        self.addEffect(effect);
    }
}
