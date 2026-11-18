package legacyconnector.v1_6_4.facade.net.minecraft.world;

/** Facade of the 1.7.10 shim's {@code Explosion} for 1.5.2 and 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class Explosion {
    private Explosion() {}

    /** {@code func_94613_c()}: the entity that caused the explosion (1.7.10's {@code getExplosivePlacedBy}). */
    public static legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase func_94613_c(legacyconnector.v1_7_10.net.minecraft.world.Explosion self) {
        return self.getExplosivePlacedBy();
    }
}
