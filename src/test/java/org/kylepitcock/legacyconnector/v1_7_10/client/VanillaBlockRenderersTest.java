package org.kylepitcock.legacyconnector.v1_7_10.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class VanillaBlockRenderersTest {
    @Test
    void registryMapsTypesToHandlers() {
        assertEquals(Set.of(7, 18, 41), VanillaBlockRenderers.handledTypes());
        assertNotNull(VanillaBlockRenderers.forRenderType(7));
        assertSame(VanillaBlockRenderers.forRenderType(18), VanillaBlockRenderers.forRenderType(41));
        assertNotSame(VanillaBlockRenderers.forRenderType(7), VanillaBlockRenderers.forRenderType(18));
        assertNull(VanillaBlockRenderers.forRenderType(26));
        assertNull(VanillaBlockRenderers.forRenderType(0));
        assertNull(VanillaBlockRenderers.forRenderType(-1));
        assertNull(VanillaBlockRenderers.forRenderType(1000));
    }

    @Test
    void unhandledVanillaTypesAreTheOnesNoModelOrHandlerDraws() {
        for (int handled : new int[] {-1, 0, 1, 6, 7, 18, 22, 27, 40, 41}) assertFalse(VanillaBlockRenderers.isUnhandledVanillaType(handled), "type " + handled);
        for (int unhandled : new int[] {2, 3, 4, 5, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39}) {
            assertTrue(VanillaBlockRenderers.isUnhandledVanillaType(unhandled), "type " + unhandled);
        }
        // a mod's own ids are not vanilla types
        assertFalse(VanillaBlockRenderers.isUnhandledVanillaType(42));
        assertFalse(VanillaBlockRenderers.isUnhandledVanillaType(4000));
    }
}
