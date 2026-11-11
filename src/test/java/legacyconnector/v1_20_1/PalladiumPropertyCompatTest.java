package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the Palladium property type alias without linking tests to a mod jar. */
class PalladiumPropertyCompatTest {
    private record FakeProperty(String type, String key) {
    }

    public static final class FakeLookup {
        private static final Map<String, Function<String, Object>> FACTORIES = new HashMap<>();

        static {
            register("integer", key -> new FakeProperty("integer", key));
        }

        public static void register(String type, Function<String, Object> factory) {
            FACTORIES.put(type, factory);
        }

        public static Object get(String type, String key) {
            Function<String, Object> factory = FACTORIES.get(type);
            return factory == null ? null : factory.apply(key);
        }

        static void reset() {
            FACTORIES.clear();
            register("integer", key -> new FakeProperty("integer", key));
        }
    }

    @BeforeEach
    void resetRegistry() {
        FakeLookup.reset();
    }

    @Test
    void registersIntAsAnAliasOfTheCanonicalIntegerFactory() throws Exception {
        assertTrue(PalladiumPropertyCompat.registerAlias(FakeLookup.class, "int", "integer"));

        FakeProperty value = (FakeProperty) FakeLookup.get("int", "minimum_score");
        assertEquals("integer", value.type());
        assertEquals("minimum_score", value.key());
    }

    @Test
    void aliasRegistrationIsIdempotentAndDoesNotReplaceAnExistingAlias() throws Exception {
        FakeLookup.register("int", key -> new FakeProperty("custom-int", key));

        assertFalse(PalladiumPropertyCompat.registerAlias(FakeLookup.class, "int", "integer"));

        FakeProperty value = (FakeProperty) FakeLookup.get("int", "existing");
        assertEquals("custom-int", value.type());
    }

    @Test
    void failsClosedWhenCanonicalTypeDoesNotExist() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> PalladiumPropertyCompat.registerAlias(FakeLookup.class, "whole_number", "missing"));
    }
}
