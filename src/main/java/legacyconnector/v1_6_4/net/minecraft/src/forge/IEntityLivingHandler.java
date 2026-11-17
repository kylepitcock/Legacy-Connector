package legacyconnector.v1_6_4.net.minecraft.src.forge;

/**
 * Forge 3.4's living-entity hook for 1.2.5 (members read from the callers in real mods: {@code onEntityLivingDeath(EntityLiving, DamageSource)}).
 * Mods extend {@code forge.adaptors.EntityLivingHandlerAdaptor} and override what they need.
 */
public interface IEntityLivingHandler {
    /** An entity is about to die; true cancels the death (the entity survives). */
    boolean onEntityLivingDeath(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, legacyconnector.v1_7_10.net.minecraft.util.DamageSource source);
}
