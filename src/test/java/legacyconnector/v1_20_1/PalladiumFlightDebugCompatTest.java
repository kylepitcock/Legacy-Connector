package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PalladiumFlightDebugCompatTest {
    private static final String PROPERTY = "legacyconnector.debug.palladiumFlight";

    @Test
    void diagnosticEnablementReadsTheJvmSystemProperty() {
        String original = System.getProperty(PROPERTY);
        try {
            System.clearProperty(PROPERTY);
            assertFalse(PalladiumFlightDebugCompat.isEnabled());

            System.setProperty(PROPERTY, "true");
            assertTrue(PalladiumFlightDebugCompat.isEnabled());

            System.setProperty(PROPERTY, "false");
            assertFalse(PalladiumFlightDebugCompat.isEnabled());
        } finally {
            if (original == null) System.clearProperty(PROPERTY);
            else System.setProperty(PROPERTY, original);
        }
    }
}
