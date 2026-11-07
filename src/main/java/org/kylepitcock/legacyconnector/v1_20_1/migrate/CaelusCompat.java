package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Caelus 3 (1.20.1) to 7 (1.21.1): the API moved to {@code com.illusivesoulworks.caelus} and hands out the flight attribute as a holder (reflection: no compile dependency). */
public final class CaelusCompat {
    private CaelusCompat() {}

    @SuppressWarnings("unchecked")
    public static Attribute flightAttribute(Object api) {
        try {
            return ((Holder<Attribute>) api.getClass().getMethod("getFallFlyingAttribute").invoke(api)).value();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Caelus API without getFallFlyingAttribute", e);
        }
    }
}
