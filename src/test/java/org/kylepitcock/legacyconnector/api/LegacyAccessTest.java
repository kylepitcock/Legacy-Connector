package org.kylepitcock.legacyconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.api.LegacyAccess.LegacyAccessException;

/** {@link LegacyAccess} needs no game: it is reflection by name and argument values. */
class LegacyAccessTest {
    public interface Marker {}

    public static class Base implements Marker {
        protected int hidden = 7;
        public static String label = "base";

        public String twice(String s) {
            return s + s;
        }

        public static int add(int a, long b) {
            return (int) (a + b);
        }
    }

    public static class Mod extends Base {
        private final String name;

        public Mod() {
            this("none");
        }

        public Mod(String name) {
            this.name = name;
        }

        public String pick(Object o) {
            return "object";
        }

        public String pick(String s) {
            return "string";
        }

        public Object nothing(Object o) {
            return o;
        }

        public void boom() {
            throw new IllegalStateException("legacy failure");
        }
    }

    @Test
    void invokesInheritedAndStaticMethodsByArgumentValues() {
        Mod mod = new Mod();
        assertEquals("abab", LegacyAccess.invoke(mod, "twice", "ab"));
        assertEquals(5, LegacyAccess.invokeStatic(Base.class, "add", 2, 3)); // Integer widens to long
    }

    @Test
    void prefersTheExactOverload() {
        Mod mod = new Mod();
        assertEquals("string", LegacyAccess.invoke(mod, "pick", "x"));
        assertEquals("object", LegacyAccess.invoke(mod, "pick", 5));
        assertNull(LegacyAccess.invoke(mod, "nothing", new Object[] {null})); // null matches any reference parameter
    }

    @Test
    void constructsAndReadsFields() {
        Object mod = LegacyAccess.newInstance(Mod.class, "n");
        assertEquals("n", LegacyAccess.getField(mod, "name"));
        assertEquals(7, LegacyAccess.getField(mod, "hidden")); // superclass, non-public
        LegacyAccess.setField(mod, "hidden", 9);
        assertEquals(9, LegacyAccess.getField(mod, "hidden"));
        assertEquals("base", LegacyAccess.getStatic(Base.class, "label"));
        LegacyAccess.setStatic(Base.class, "label", "changed");
        assertEquals("changed", LegacyAccess.getStatic(Mod.class, "label"));
        LegacyAccess.setStatic(Base.class, "label", "base");
    }

    @Test
    void answersQuestionsAboutTypes() {
        Mod mod = new Mod();
        assertTrue(LegacyAccess.isInstance(mod, Marker.class.getName()));
        assertTrue(LegacyAccess.isInstance(mod, Base.class.getName()));
        assertFalse(LegacyAccess.isInstance(mod, "no.Such"));
        assertFalse(LegacyAccess.isInstance(null, Base.class.getName()));
        assertTrue(LegacyAccess.hasMethod(Mod.class, "twice", 1));
        assertFalse(LegacyAccess.hasMethod(Mod.class, "twice", 2));
    }

    @Test
    void failuresAreClearAndKeepTheCause() {
        assertThrows(LegacyAccessException.class, () -> LegacyAccess.invoke(new Mod(), "missing"));
        assertThrows(LegacyAccessException.class, () -> LegacyAccess.getField(new Mod(), "missing"));
        assertThrows(LegacyAccessException.class, () -> LegacyAccess.invoke(null, "twice", "x"));
        LegacyAccessException e = assertThrows(LegacyAccessException.class, () -> LegacyAccess.invoke(new Mod(), "boom"));
        Throwable cause = e.getCause();
        assertTrue(cause instanceof IllegalStateException && "legacy failure".equals(cause.getMessage()), String.valueOf(cause));
        assertSame(e.getCause(), cause);
    }
}
