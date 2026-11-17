package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** A library translated as the fallback must not fold two of its own classes into one name (GeckoLib 4.8: GeoModel implementing CoreGeoModel; issue 309). */
class Forge1201OwnClassCollisionTest {
    private static final String CORE = "software/bernie/geckolib/core/animatable/model/CoreGeoModel";
    private static final String GEO = "software/bernie/geckolib/model/GeoModel";

    @Test
    void tableRenameIsSkippedWhenBothNamesAreClassesOfTheJar() {
        assertTrue(Forge1201Transformer.mergesOwnClasses(Set.of(CORE, GEO), CORE, GEO));
    }

    @Test
    void otherJarsStillFollowTheTable() {
        assertFalse(Forge1201Transformer.mergesOwnClasses(Set.of("mod/Foo"), CORE, GEO));
        assertFalse(Forge1201Transformer.mergesOwnClasses(Set.of(CORE), CORE, GEO));
    }

    @Test
    void geckoLibCoreModelIsStillMappedInTheTable() {
        assertEquals(GEO, Mc1201Tables.get().classes.get(CORE));
    }
}
