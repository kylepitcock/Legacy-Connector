package legacyconnector.v1_20_1;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * KubeJS 7 adapter for legacy ClientEvents.init key-mapping registration.
 *
 * <p>KubeJS 7 moved key mappings to KeyBindEvents.registry and ultimately
 * RegisterKeyMappingsEvent. This bridge lets translated 1.20.1 addon scripts
 * keep their original KeyMapping labels/category/default key while routing
 * registration through KubeJS 7's current registration event.</p>
 */
public final class KubeJSKeybindCompat {
    private static final String KUBEJS_KEYBINDS =
            "dev.latvian.mods.kubejs.client.KubeJSKeybinds";
    private static final String KEYBINDING_EVENT =
            "dev.latvian.mods.kubejs.client.KeybindRegistryKubeEvent";
    private static final String KEY_MAPPING = "net.minecraft.client.KeyMapping";
    private static final String KEY_CONFLICT_CONTEXT =
            "net.neoforged.neoforge.client.settings.KeyConflictContext";
    private static final String KEY_MODIFIER =
            "net.neoforged.neoforge.client.settings.KeyModifier";
    private static final String INPUT_TYPE = "com.mojang.blaze3d.platform.InputConstants$Type";

    private record PendingMapping(Object kubeKey, Object keyMapping) {
    }

    private static final Map<Object, Map<String, PendingMapping>> PENDING =
            Collections.synchronizedMap(new WeakHashMap<>());

    private KubeJSKeybindCompat() {
    }

    /**
     * Creates the modern KeyMapping with the exact legacy name/category/default
     * and associates it with KubeJS 7's KubeKey. The returned object is the raw
     * KeyMapping, preserving the global values expected by existing client
     * scripts. The associated KubeKey is appended to KeybindRegistryKubeEvent.build().
     */
    public static Object registerLegacyMapping(
            Object registryEvent,
            String id,
            String translationKey,
            int defaultKey,
            String category) {
        Objects.requireNonNull(registryEvent, "KubeJS keybind registry event");
        requireText(id, "id");
        requireText(translationKey, "translationKey");
        requireText(category, "category");

        try {
            ClassLoader loader = registryEvent.getClass().getClassLoader();
            Class<?> keybindsClass = Class.forName(KUBEJS_KEYBINDS, true, loader);
            Method getOrCreate = keybindsClass.getMethod("getOrCreate", String.class);
            Object kubeKey = getOrCreate.invoke(null, id);

            Class<?> mappingClass = Class.forName(KEY_MAPPING, true, loader);
            Class<?> conflictContextClass = Class.forName(KEY_CONFLICT_CONTEXT, true, loader);
            Class<?> keyModifierClass = Class.forName(KEY_MODIFIER, true, loader);
            Class<?> inputTypeClass = Class.forName(INPUT_TYPE, true, loader);

            Object universal = publicStaticField(conflictContextClass, "UNIVERSAL").get(null);
            Object noModifier = publicStaticField(keyModifierClass, "NONE").get(null);
            Object keysym = publicStaticField(inputTypeClass, "KEYSYM").get(null);

            Constructor<?> constructor = mappingClass.getConstructor(
                    String.class,
                    conflictContextClass,
                    keyModifierClass,
                    inputTypeClass,
                    int.class,
                    String.class);
            Object keyMapping = constructor.newInstance(
                    translationKey, universal, noModifier, keysym, defaultKey, category);

            Field mappingField = publicField(kubeKey, "mapping");
            Object previousMapping = mappingField.get(kubeKey);
            if (previousMapping != null && previousMapping != keyMapping) {
                throw new IllegalStateException("KubeJS key id '" + id + "' already has a different mapping");
            }
            mappingField.set(kubeKey, keyMapping);

            synchronized (PENDING) {
                Map<String, PendingMapping> byId =
                        PENDING.computeIfAbsent(registryEvent, ignored -> new LinkedHashMap<>());
                PendingMapping previous = byId.putIfAbsent(id, new PendingMapping(kubeKey, keyMapping));
                if (previous != null && (previous.kubeKey() != kubeKey || previous.keyMapping() != keyMapping)) {
                    throw new IllegalStateException("Duplicate legacy KubeJS key mapping id '" + id + "'");
                }
            }
            return keyMapping;
        } catch (ReflectiveOperationException error) {
            throw compatibilityFailure("registerLegacyMapping", error);
        }
    }

    /**
     * Appends manually constructed legacy KeyMappings to the list returned by
     * KubeJS 7's keybind registry event. KubeJSClientEventHandler then registers
     * each mapping through the live RegisterKeyMappingsEvent, not Architectury's
     * after-event fallback.
     */
    public static List<?> appendLegacyMappings(Object registryEvent, List<?> built) {
        Objects.requireNonNull(registryEvent, "KubeJS keybind registry event");
        Objects.requireNonNull(built, "KubeJS built key mappings");

        Map<String, PendingMapping> pending;
        synchronized (PENDING) {
            pending = PENDING.remove(registryEvent);
        }
        if (pending == null || pending.isEmpty()) {
            return built;
        }

        List<Object> merged = new ArrayList<>(built);
        for (Map.Entry<String, PendingMapping> entry : pending.entrySet()) {
            PendingMapping value = entry.getValue();
            if (merged.contains(value.kubeKey())) {
                try {
                    Object actualMapping = publicField(value.kubeKey(), "mapping").get(value.kubeKey());
                    if (actualMapping != value.keyMapping()) {
                        throw new IllegalStateException("KubeJS key id '" + entry.getKey()
                                + "' was also registered with a different mapping");
                    }
                } catch (ReflectiveOperationException error) {
                    throw compatibilityFailure("appendLegacyMappings", error);
                }
            } else {
                merged.add(value.kubeKey());
            }
        }
        return merged;
    }

    /**
     * Migrates only the inspected Alien Evolution 1.20.1 ClientEvents.init
     * script that constructs its quick-change KeyMappings and registers them
     * through Architectury's static KeyMappingRegistry.
     *
     * <p>The event is moved to KubeJS 7's KeyBindEvents.registry, and each
     * mapping is handed to the compatibility method above so the original
     * translation key, unknown default key, category, and global KeyMapping
     * object are preserved.</p>
     */
    public static byte[] rewriteLegacyQuickChangeSource(byte[] source) {
        Objects.requireNonNull(source, "KubeJS addon script source");
        String script = new String(source, java.nio.charset.StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        if (!script.contains("ClientEvents.init(event => {")
                || !script.contains("dev.architectury.registry.client.keymappings.KeyMappingRegistry")
                || !script.contains("QUICKCHANGEWHEEL")
                || !script.contains("key.alienevo.key")
                || !script.contains("key.categories.alienevo")) {
            return source;
        }

        String oldLoop = """
                global[keyName] = new $KeyMapping(
                          `key.alienevo.key${i}`,
                          $GLFWKey.GLFW_KEY_UNKNOWN,
                          "key.categories.alienevo"
                        );

                        $KeyMappingRegistry.register(global[keyName]);""";
        String newLoop = """
                global[keyName] = event.registerLegacyMapping(
                          keyName,
                          `key.alienevo.key${i}`,
                          $GLFWKey.GLFW_KEY_UNKNOWN,
                          "key.categories.alienevo"
                        );""";
        String oldWheel = """
                      global["QUICKCHANGEWHEEL"] = new $KeyMapping(
                        "key.alienevo.wheel",
                        $GLFWKey.GLFW_KEY_UNKNOWN,
                        "key.categories.alienevo"
                      );
                      
                      $KeyMappingRegistry.register(global["QUICKCHANGEWHEEL"]);""";
        String newWheel = """
                      global["QUICKCHANGEWHEEL"] = event.registerLegacyMapping(
                        "QUICKCHANGEWHEEL",
                        "key.alienevo.wheel",
                        $GLFWKey.GLFW_KEY_UNKNOWN,
                        "key.categories.alienevo"
                      );""";

        if (count(script, oldLoop) != 1 || count(script, oldWheel) != 1) {
            return source;
        }

        String rewritten = script
                .replace("ClientEvents.init(event => {", "KeyBindEvents.registry(event => {")
                .replace("      const $KeyMappingRegistry = Java.loadClass(\"dev.architectury.registry.client.keymappings.KeyMappingRegistry\");\n", "")
                .replace("      const $KeyMapping = Java.loadClass(\"net.minecraft.client.KeyMapping\");\n", "")
                .replace(oldLoop, newLoop)
                .replace(oldWheel, newWheel);

        if (rewritten.contains("ClientEvents.init")
                || rewritten.contains("KeyMappingRegistry")
                || !rewritten.contains("event.registerLegacyMapping(")) {
            return source;
        }
        return rewritten.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int count(String text, String needle) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Legacy key mapping " + name + " must not be blank");
        }
    }

    private static Field publicStaticField(Class<?> owner, String name) throws NoSuchFieldException {
        return owner.getField(name);
    }

    private static Field publicField(Object owner, String name) throws NoSuchFieldException {
        return owner.getClass().getField(name);
    }

    private static IllegalStateException compatibilityFailure(String operation, Throwable cause) {
        Throwable root = cause instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : cause;
        return new IllegalStateException(
                "KubeJS 7 keybind " + operation + " compatibility could not access the inspected API",
                root);
    }
}
