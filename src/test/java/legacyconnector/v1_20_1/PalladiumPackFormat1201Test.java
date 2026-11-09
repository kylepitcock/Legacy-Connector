package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Regression tests for Palladium's custom ADDON pack type on Minecraft 1.21.1. */
class PalladiumPackFormat1201Test {
    @Test
    void addonPackUsesCurrentResourcePackFormat() {
        assertEquals(34, Compat1201.packFormat("ADDON"));
    }

    @Test
    void vanillaPackTypesKeepTheirOwnFormats() {
        assertEquals(34, Compat1201.packFormat("CLIENT_RESOURCES"));
        assertEquals(48, Compat1201.packFormat("SERVER_DATA"));
    }

    @Test
    void unknownPackTypesFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> Compat1201.packFormat("UNKNOWN"));
    }
}
