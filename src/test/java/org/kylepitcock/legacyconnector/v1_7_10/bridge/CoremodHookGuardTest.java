package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** The guards read from hooks' first instructions: the shapes of check the compiler makes, a hook without such a check, and a non-boolean hook. */
class CoremodHookGuardTest {
    /** Hooks as coremods write them; {@code Entity}/{@code Player} stand in for the shim's classes. */
    public static final class Hooks {
        public static boolean nullThenInstance(Object world, Entity e) {
            if (e == null) return false;
            if (!(e instanceof Player)) return false;
            return world != null;
        }

        public static boolean andChain(Entity e, Object block) {
            if (e != null && e instanceof Player && block != null) {
                return block.hashCode() > 3;
            }
            return false;
        }

        public static boolean instanceOnly(double a, Entity e) {
            if (!(e instanceof Player)) return false;
            return a > 1.0;
        }

        public static boolean noCheck(Entity e) {
            return e.hashCode() > 3;
        }

        public static boolean checkAfterWork(Object world, Entity e) {
            if (world.hashCode() == 7) return true;
            return e instanceof Player;
        }

        public static int notBoolean(Entity e) {
            return e == null ? 0 : 1;
        }
    }

    private static CoremodHookGuard guard(String name, int param) throws ReflectiveOperationException {
        for (Method m : Hooks.class.getDeclaredMethods()) {
            if (m.getName().equals(name)) return CoremodHookGuard.of(Hooks.class, m, param);
        }
        throw new NoSuchMethodException(name);
    }

    @Test
    void nullCheckThenInstanceCheck() throws Exception {
        CoremodHookGuard g = guard("nullThenInstance", 1);
        assertNotNull(g);
        assertTrue(g.nullFails());
        assertEquals(java.util.List.of(Player.class), g.mustBe());
        assertFalse(g.allows(null));
        assertFalse(g.allows(new Entity()));
        assertTrue(g.allows(new Player()));
    }

    @Test
    void conjunctionThatReturnsFalseAtTheEnd() throws Exception {
        CoremodHookGuard g = guard("andChain", 0);
        assertNotNull(g);
        assertTrue(g.nullFails());
        assertEquals(java.util.List.of(Player.class), g.mustBe());
        assertFalse(g.allows(new Entity()));
        assertTrue(g.allows(new Player()));
    }

    @Test
    void instanceCheckAloneAfterAWideParameter() throws Exception {
        CoremodHookGuard g = guard("instanceOnly", 1); // the double takes two slots
        assertNotNull(g);
        assertFalse(g.nullFails());
        assertFalse(g.allows(new Entity()));
        assertTrue(g.allows(new Player()));
        assertFalse(g.allows(null)); // an instanceof test is false for null, too
    }

    @Test
    void hooksThatStartWithOtherWorkHaveNoGuard() throws Exception {
        assertNull(guard("noCheck", 0));
        assertNull(guard("checkAfterWork", 1));
        assertNull(guard("notBoolean", 0));
    }

    public static class Entity {}

    public static final class Player extends Entity {}
}
