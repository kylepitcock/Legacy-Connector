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

        List<Object> pendingKeys = new ArrayList<>();
        for (Map.Entry<String, PendingMapping> entry : pending.entrySet()) {
            PendingMapping value = entry.getValue();
            if (built.contains(value.kubeKey())) {
                try {
                    Object actualMapping = publicField(value.kubeKey(), "mapping").get(value.kubeKey());
                    if (actualMapping != value.keyMapping()) {
                        throw new IllegalStateException("KubeJS key id '" + entry.getKey()
                                + "' was also registered with a different mapping");
                    }
                } catch (ReflectiveOperationException error) {
                    throw compatibilityFailure("appendLegacyMappings", error);
                }
            }
            pendingKeys.add(value.kubeKey());
        }
        return mergeMappings(built, pendingKeys);
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

        List<String> lines = new ArrayList<>(java.util.Arrays.asList(script.split("\n", -1)));
        int eventLine = uniqueLine(lines, line -> line.trim().equals("ClientEvents.init(event => {"));
        int registryClassLine = uniqueLine(lines, line -> line.trim().equals(
                "const $KeyMappingRegistry = Java.loadClass(\"dev.architectury.registry.client.keymappings.KeyMappingRegistry\");"));
        int mappingClassLine = uniqueLine(lines, line -> line.trim().equals(
                "const $KeyMapping = Java.loadClass(\"net.minecraft.client.KeyMapping\");"));
        int loopStart = uniqueLine(lines, line -> line.trim().equals("global[keyName] = new $KeyMapping("));
        int wheelStart = uniqueLine(lines, line -> line.trim().equals(
                "global[\"QUICKCHANGEWHEEL\"] = new $KeyMapping("));

        if (eventLine < 0 || registryClassLine < 0 || mappingClassLine < 0
                || loopStart < 0 || wheelStart < 0
                || !matchesBlock(lines, loopStart, new String[] {
                        "global[keyName] = new $KeyMapping(",
                        String.valueOf((char) 96) + "key.alienevo.key" + "$" + "{i}" + (char) 96 + ",",
                        "$GLFWKey.GLFW_KEY_UNKNOWN,",
                        "\"key.categories.alienevo\"",
                        ");",
                        "",
                        "$KeyMappingRegistry.register(global[keyName]);"
                })
                || !matchesBlock(lines, wheelStart, new String[] {
                        "global[\"QUICKCHANGEWHEEL\"] = new $KeyMapping(",
                        "\"key.alienevo.wheel\",",
                        "$GLFWKey.GLFW_KEY_UNKNOWN,",
                        "\"key.categories.alienevo\"",
                        ");",
                        "",
                        "$KeyMappingRegistry.register(global[\"QUICKCHANGEWHEEL\"]);"
                })) {
            return source;
        }

        List<String> rewrittenLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == eventLine) {
                rewrittenLines.add(line.replace("ClientEvents.init", "KeyBindEvents.registry"));
            } else if (i == registryClassLine || i == mappingClassLine) {
                // KubeJS 7 provides the live key-mapping registry event. Do not
                // load the old Architectury registry or the legacy constructor.
            } else if (i == loopStart) {
                String indent = leadingWhitespace(line);
                rewrittenLines.add(indent + "global[keyName] = event.registerLegacyMapping(");
                rewrittenLines.add(indent + "  keyName,");
                rewrittenLines.add(indent + "  " + (char) 96 + "key.alienevo.key" + "$" + "{i}" + (char) 96 + ",");
                rewrittenLines.add(indent + "  $GLFWKey.GLFW_KEY_UNKNOWN,");
                rewrittenLines.add(indent + "  \"key.categories.alienevo\"");
                rewrittenLines.add(indent + ");");
                i += 6;
            } else if (i == wheelStart) {
                String indent = leadingWhitespace(line);
                rewrittenLines.add(indent + "global[\"QUICKCHANGEWHEEL\"] = event.registerLegacyMapping(");
                rewrittenLines.add(indent + "  \"QUICKCHANGEWHEEL\",");
                rewrittenLines.add(indent + "  \"key.alienevo.wheel\",");
                rewrittenLines.add(indent + "  $GLFWKey.GLFW_KEY_UNKNOWN,");
                rewrittenLines.add(indent + "  \"key.categories.alienevo\"");
                rewrittenLines.add(indent + ");");
                i += 6;
            } else {
                rewrittenLines.add(line);
            }
        }

        String rewritten = String.join("\n", rewrittenLines);
        if (rewritten.contains("ClientEvents.init")
                || rewritten.contains("KeyMappingRegistry")
                || count(rewritten, "event.registerLegacyMapping(") != 2) {
            return source;
        }
        return rewritten.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    static List<Object> mergeMappings(List<?> built, List<?> pending) {
        List<Object> merged = new ArrayList<>(built);
        for (Object key : pending) {
            if (!merged.contains(key)) {
                merged.add(key);
            }
        }
        return merged;
    }
    private static int uniqueLine(List<String> lines, java.util.function.Predicate<String> predicate) {
        int found = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (predicate.test(lines.get(i))) {
                if (found >= 0) {
                    return -1;
                }
                found = i;
            }
        }
        return found;
    }

    private static boolean matchesBlock(List<String> lines, int start, String[] expectedTrimmedLines) {
        if (start < 0 || start + expectedTrimmedLines.length > lines.size()) {
            return false;
        }
        for (int i = 0; i < expectedTrimmedLines.length; i++) {
            if (!lines.get(start + i).trim().equals(expectedTrimmedLines[i])) {
                return false;
            }
        }
        return true;
    }

    private static String leadingWhitespace(String line) {
        int end = 0;
        while (end < line.length() && (line.charAt(end) == ' ' || line.charAt(end) == '\t')) {
            end++;
        }
        return line.substring(0, end);
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
