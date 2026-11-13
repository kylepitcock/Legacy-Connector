package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regression tests for the KubeJS 6 client-init key mapping migration. */
class KubeJSKeybindCompatTest {
    private interface FakeKeyConflictContextApi {}
    private enum FakeKeyConflictContext implements FakeKeyConflictContextApi { UNIVERSAL }
    private enum FakeKeyModifier { NONE }
    private enum FakeInputType { KEYSYM }

    public static final class FakeKeyMapping {
        public FakeKeyMapping(String name, FakeKeyConflictContext context, FakeKeyModifier modifier,
                FakeInputType inputType, int code, String category) {}

        public FakeKeyMapping(String name, FakeKeyConflictContextApi context, FakeKeyModifier modifier,
                FakeInputType inputType, int code, String category) {}
    }

    @Test
    void resolvesTheNeoForgeConstructorUsingTheConflictContextInterface() throws Exception {
        Constructor<?> constructor = KubeJSKeybindCompat.findLegacyMappingConstructor(
                FakeKeyMapping.class,
                FakeKeyConflictContextApi.class,
                FakeKeyModifier.class,
                FakeInputType.class);

        assertEquals(FakeKeyConflictContextApi.class, constructor.getParameterTypes()[1]);
        assertEquals(FakeKeyMapping.class, constructor.getDeclaringClass());
    }

    @Test
    void rewritesTheInspectedQuickChangeScriptToKubeJs7RegistryEvent() throws Exception {
        String original = """
                if (Platform.isClientEnvironment()) {
                    ClientEvents.init(event => {
                      const $KeyMappingRegistry = Java.loadClass("dev.architectury.registry.client.keymappings.KeyMappingRegistry");
                      const $KeyMapping = Java.loadClass("net.minecraft.client.KeyMapping");
                      const $GLFWKey = Java.loadClass("org.lwjgl.glfw.GLFW");

                      for (let i = 1; i <= 10; i++) {
                        let keyName = `QUICKCHANGE${i.toString().padStart(2, '0')}`;
                        global[keyName] = new $KeyMapping(
                          `key.alienevo.key${i}`,
                          $GLFWKey.GLFW_KEY_UNKNOWN,
                          "key.categories.alienevo"
                        );

                        $KeyMappingRegistry.register(global[keyName]);
                      }

                      global["QUICKCHANGEWHEEL"] = new $KeyMapping(
                        "key.alienevo.wheel",
                        $GLFWKey.GLFW_KEY_UNKNOWN,
                        "key.categories.alienevo"
                      );

                      $KeyMappingRegistry.register(global["QUICKCHANGEWHEEL"]);
                    })
                }
                """;

        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        byte[] result = KubeJSKeybindCompat.rewriteLegacyQuickChangeSource(input);
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertFalse(rewritten.contains("ClientEvents.init"));
        assertFalse(rewritten.contains("KeyMappingRegistry"));
        assertEquals(1, count(rewritten, "KeyBindEvents.registry("));
        assertEquals(2, count(rewritten, "event.registerLegacyMapping("));
        assertTrueContains(rewritten, "key.alienevo.key${i}");
        assertTrueContains(rewritten, "key.alienevo.wheel");
        assertTrueContains(rewritten, "key.categories.alienevo");
    }

    @Test
    void leavesUnrelatedClientInitEventsUnchanged() {
        byte[] input = "ClientEvents.init(event => event.registerMenuScreen());".getBytes(StandardCharsets.UTF_8);

        assertSame(input, KubeJSKeybindCompat.rewriteLegacyQuickChangeSource(input));
    }

    @Test
    void doesNotPartiallyRewriteAnUnrecognizedKeyMappingScript() {
        byte[] input = """
                ClientEvents.init(event => {
                  const KeyMappingRegistry = Java.loadClass("dev.architectury.registry.client.keymappings.KeyMappingRegistry");
                });
                """.getBytes(StandardCharsets.UTF_8);

        assertSame(input, KubeJSKeybindCompat.rewriteLegacyQuickChangeSource(input));
    }

    @Test
    void migratesLegacyKeyMappingFieldReadsToTheCurrentAccessor() {
        String original = """
                ClientEvents.tick(event => {
                    const quickChangeKeys = ["QUICKCHANGE01"];
                    quickChangeKeys.forEach(keyName => {
                        let key = global[keyName];
                        let bind = key.key.getValue();
                    });
                    let wheelKey = global["QUICKCHANGEWHEEL"];
                    let wheelBind = wheelKey.key.getValue();
                });
                """;
        byte[] input = original.getBytes(StandardCharsets.UTF_8);

        byte[] result = KubeJSKeybindCompat.rewriteLegacyKeyMappingFieldAccess(input);
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertEquals(2, count(rewritten, ".getKey().getValue()"));
        assertFalse(rewritten.contains(".key.getValue()"));
    }

    @Test
    void appendsPendingKubeKeysWithoutReplacingExistingBuiltMappings() {
        Object a = new Object();
        Object b = new Object();

        List<?> result = KubeJSKeybindCompat.mergeMappings(List.of(a), List.of(b));

        assertEquals(2, result.size());
        assertSame(a, result.get(0));
        assertSame(b, result.get(1));
    }

    private static int count(String text, String needle) {
        return text.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }

    private static void assertTrueContains(String text, String needle) {
        org.junit.jupiter.api.Assertions.assertTrue(text.contains(needle), "missing: " + needle);
    }
}
