package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/** Regression tests for the reflective KubeJS 7 Palladium builder callback bridge. */
class PalladiumKubeJSCompatTest {
    public interface TestBuilderFactory {
    }

    public interface TestCallback {
        void addDefault(Class<?> builderClass, TestBuilderFactory factory);

        default void add(String type, Class<?> builderClass, TestBuilderFactory factory) {
            throw new UnsupportedOperationException("test callback must capture the named type");
        }
    }

    private static final class CapturingCallback implements TestCallback {
        Class<?> defaultBuilderClass;
        TestBuilderFactory defaultFactory;
        String namedType;
        Class<?> namedBuilderClass;
        TestBuilderFactory namedFactory;

        @Override
        public void addDefault(Class<?> builderClass, TestBuilderFactory factory) {
            defaultBuilderClass = builderClass;
            defaultFactory = factory;
        }

        @Override
        public void add(String type, Class<?> builderClass, TestBuilderFactory factory) {
            namedType = type;
            namedBuilderClass = builderClass;
            namedFactory = factory;
        }
    }

    @Test
    void registersBasicBuilderAsBothDefaultAndNamedAlias() throws Exception {
        CapturingCallback callback = new CapturingCallback();
        TestBuilderFactory factory = new TestBuilderFactory() {
        };

        PalladiumKubeJSCompat.registerBasicBuilderType(
                callback,
                TestCallback.class,
                StringBuilder.class,
                TestBuilderFactory.class,
                factory);

        assertEquals(StringBuilder.class, callback.defaultBuilderClass);
        assertSame(factory, callback.defaultFactory);
        assertEquals("basic", callback.namedType);
        assertEquals(StringBuilder.class, callback.namedBuilderClass);
        assertSame(factory, callback.namedFactory);
    }
}
