package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonNull;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.MiscCompat;

/** Preserves the nullable Component serialization behavior used by Forge 1.20.1 mods. */
class MiscCompatNullableComponentTest {
    @Test
    void serializesNullComponentAsJsonNullWithoutCallingTheModernCodec() {
        assertEquals("null", MiscCompat.toJson(null));
    }

    @Test
    void deserializesJsonNullBackToANullComponent() {
        assertNull(MiscCompat.fromJson("null"));
        assertNull(MiscCompat.fromJson("  null  "));
        assertNull(MiscCompat.fromJson(JsonNull.INSTANCE));
    }
}
