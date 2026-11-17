package legacyconnector.v1_6_4.net.minecraft.src.forge.adaptors;

import legacyconnector.v1_6_4.net.minecraft.src.forge.IEntityLivingHandler;

/** Forge 3.4's convenience base class for {@link IEntityLivingHandler}: every hook does nothing. */
public class EntityLivingHandlerAdaptor implements IEntityLivingHandler {
    @Override
    public boolean onEntityLivingDeath(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, legacyconnector.v1_7_10.net.minecraft.util.DamageSource source) {
        return false;
    }
}
