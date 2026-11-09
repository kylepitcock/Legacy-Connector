package legacyconnector.v1_20_1;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Runtime bridge for Palladium 4.5.x addon-pack scripts after its KubeJS 6
 * ScriptFileInfo subclass has been removed by the 1.21.1 jar translator.
 *
 * KubeJS 7 stores parsed script metadata on ScriptFile, not ScriptFileInfo.
 * This bridge materializes selected resource-pack scripts into a stable,
 * per-script-type cache and lets KubeJS 7's own ScriptFile constructor parse
 * them. KubeJS classes are accessed reflectively so the Legacy Connector
 * runtime does not acquire a hard dependency on KubeJS.
 */
public final class PalladiumKubeJSCompat {
    private static final System.Logger LOGGER = System.getLogger("LegacyConnector/PalladiumKubeJSCompat");
    private static final String PALLADIUM_ADDON_PACK_MANAGER =
            "net.threetag.palladium.addonpack.AddonPackManager";
    private static final String PACK_TYPE = "net.minecraft.server.packs.PackType";
    private static final String PACK_RESOURCES_OUTPUT =
            "net.minecraft.server.packs.PackResources$ResourceOutput";
    private static final String SCRIPT_PACK_INFO = "dev.latvian.mods.kubejs.script.ScriptPackInfo";
    private static final String SCRIPT_PACK = "dev.latvian.mods.kubejs.script.ScriptPack";
    private static final String SCRIPT_FILE_INFO = "dev.latvian.mods.kubejs.script.ScriptFileInfo";
    private static final String SCRIPT_FILE = "dev.latvian.mods.kubejs.script.ScriptFile";
    private static final String SCRIPT_ROOT = "kubejs_scripts";
    private static final String CACHE_ROOT = "kubejs/palladium_addon_scripts";

    private record AddonScript(Path backingFile, String resourcePath) {
    }

    private static final class ScriptGroup {
        final String resourceNamespace;
        final List<AddonScript> scripts = new ArrayList<>();

        ScriptGroup(String resourceNamespace) {
            this.resourceNamespace = resourceNamespace;
        }
    }

    private PalladiumKubeJSCompat() {
    }

    /**
     * Called by the translated Palladium ScriptManagerMixin at the end of
     * KubeJS 7 ScriptManager.loadFromDirectory().
     *
     * @param manager the KubeJS 7 ScriptManager instance
     * @param namespacePrefix prefix from Palladium's original StringConcatFactory recipe
     * @param namespaceSuffix suffix from Palladium's original StringConcatFactory recipe
     */
    public static void loadAddonScripts(Object manager, String namespacePrefix, String namespaceSuffix) {
        if (manager == null) {
            LOGGER.log(System.Logger.Level.ERROR, "Cannot load Palladium addon scripts: KubeJS ScriptManager was null");
            return;
        }

        try {
            ClassLoader loader = manager.getClass().getClassLoader();
            Object scriptType = publicField(manager, "scriptType");
            String scriptTypeName = scriptType instanceof Enum<?> value ? value.name() : String.valueOf(scriptType);
            Object packType = packTypeFor(scriptTypeName, loader);

            Object addonPackManager = invokeStatic(
                    Class.forName(PALLADIUM_ADDON_PACK_MANAGER, true, loader),
                    new String[] {"getInstance"});
            Object repository = invoke(addonPackManager, new String[] {"getPackList"});
            invoke(repository, new String[] {"reload", "m_10506_"});
            Object availablePacks = invoke(repository, new String[] {"getAvailablePacks", "m_10514_"});
            invoke(repository, new String[] {"setSelected", "m_10509_"}, availablePacks);
            Object selectedPacks = invoke(repository, new String[] {"getSelectedPacks", "m_10519_"});

            Path typeCache = FMLPaths.GAMEDIR.get().resolve(CACHE_ROOT).resolve(scriptTypeName.toLowerCase(Locale.ROOT));
            Files.createDirectories(typeCache);

            Map<String, ScriptGroup> groups = new LinkedHashMap<>();
            int packIndex = 0;
            for (Object pack : asCollection(selectedPacks)) {
                String packId = stringResult(invoke(pack, new String[] {"getId"}), "pack-" + packIndex);
                String stablePackKey = safeSegment(packId) + "-" + Integer.toHexString(packId.hashCode());
                Path packCache = typeCache.resolve(stablePackKey);
                try {
                    collectPackScripts(pack, packType, packCache, groups, loader);
                } catch (Throwable packError) {
                    LOGGER.log(System.Logger.Level.ERROR,
                            "Failed to collect Palladium scripts from selected pack " + packId, unwrap(packError));
                }
                packIndex++;
            }

            int discovered = 0;
            int loaded = 0;
            Map<String, Object> managerPacks = asMap(publicField(manager, "packs"));
            Class<?> scriptManagerClass = Class.forName("dev.latvian.mods.kubejs.script.ScriptManager", true, loader);
            Class<?> packInfoClass = Class.forName(SCRIPT_PACK_INFO, true, loader);
            Class<?> scriptPackClass = Class.forName(SCRIPT_PACK, true, loader);
            Class<?> scriptFileInfoClass = Class.forName(SCRIPT_FILE_INFO, true, loader);
            Class<?> scriptFileClass = Class.forName(SCRIPT_FILE, true, loader);

            Constructor<?> packInfoConstructor = packInfoClass.getConstructor(String.class, String.class);
            // StartupScriptManager / ClientScriptManager / ServerScriptManager all extend ScriptManager.
            Constructor<?> scriptPackConstructor = scriptPackClass.getConstructor(scriptManagerClass, packInfoClass);
            Constructor<?> fileInfoConstructor = scriptFileInfoClass.getConstructor(packInfoClass, Path.class, String.class);
            Constructor<?> scriptFileConstructor = scriptFileClass.getConstructor(scriptPackClass, scriptFileInfoClass);
            Field infoScriptsField = packInfoClass.getField("scripts");
            Field packScriptsField = scriptPackClass.getField("scripts");

            for (ScriptGroup group : groups.values()) {
                String packNamespace = namespacePrefix + group.resourceNamespace + namespaceSuffix;
                Object packInfo = packInfoConstructor.newInstance(packNamespace, "");
                Object scriptPack = scriptPackConstructor.newInstance(manager, packInfo);
                @SuppressWarnings("unchecked")
                List<Object> infoScripts = (List<Object>) infoScriptsField.get(packInfo);
                @SuppressWarnings("unchecked")
                List<Object> loadedScripts = (List<Object>) packScriptsField.get(scriptPack);

                for (AddonScript script : group.scripts) {
                    discovered++;
                    try {
                        Object fileInfo = fileInfoConstructor.newInstance(packInfo, script.backingFile(), script.resourcePath());
                        infoScripts.add(fileInfo);
                        Object scriptFile = scriptFileConstructor.newInstance(scriptPack, fileInfo);
                        String skipReason = String.valueOf(invoke(scriptFile, new String[] {"skipLoading"}));
                        if (skipReason.isEmpty()) {
                            loadedScripts.add(scriptFile);
                            loaded++;
                        } else {
                            LOGGER.log(System.Logger.Level.INFO,
                                    "Skipped Palladium addon script {0}:{1}: {2}",
                                    packNamespace, script.resourcePath(), skipReason);
                        }
                    } catch (Throwable scriptError) {
                        LOGGER.log(System.Logger.Level.ERROR,
                                "Failed to prepare Palladium addon script " + packNamespace + ":" + script.resourcePath(),
                                unwrap(scriptError));
                    }
                }

                loadedScripts.sort(null);
                managerPacks.put(packNamespace, scriptPack);
            }

            LOGGER.log(System.Logger.Level.INFO,
                    "KubeJS {0}: discovered {1} Palladium addon script(s), registered {2} for loading across {3} pack namespace(s)",
                    scriptTypeName, discovered, loaded, groups.size());
        } catch (Throwable error) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "Palladium addon scripts could not be adapted to the KubeJS 7 ScriptFile model; ordinary KubeJS scripts remain available",
                    unwrap(error));
        }
    }

    private static Object packTypeFor(String scriptTypeName, ClassLoader loader) throws ReflectiveOperationException {
        Class<?> packTypeClass = Class.forName(PACK_TYPE, true, loader);
        if ("CLIENT".equals(scriptTypeName)) {
            return enumConstant(packTypeClass, "CLIENT_RESOURCES");
        }
        if ("SERVER".equals(scriptTypeName)) {
            return enumConstant(packTypeClass, "SERVER_DATA");
        }
        Class<?> managerClass = Class.forName(PALLADIUM_ADDON_PACK_MANAGER, true, loader);
        return invokeStatic(managerClass, new String[] {"getPackType"});
    }

    private static Object enumConstant(Class<?> enumClass, String name) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException(enumClass.getName() + " is not an enum type");
        }
        for (Object value : enumClass.getEnumConstants()) {
            if (value instanceof Enum<?> constant && constant.name().equals(name)) return value;
        }
        throw new IllegalArgumentException("No " + enumClass.getName() + " constant named " + name);
    }

    private static void collectPackScripts(
            Object pack,
            Object packType,
            Path packCache,
            Map<String, ScriptGroup> groups,
            ClassLoader loader) throws ReflectiveOperationException {
        Object resources = invoke(pack, new String[] {"open", "m_10445_"});
        if (resources == null) return;

        try {
            Object namespacesValue = invoke(resources, new String[] {"getNamespaces", "m_5698_"}, packType);
            for (Object namespaceValue : asCollection(namespacesValue)) {
                String namespace = String.valueOf(namespaceValue);
                Class<?> outputClass = Class.forName(PACK_RESOURCES_OUTPUT, true, loader);
                InvocationHandler callback = (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "PalladiumAddonScriptOutput";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    if (args != null && args.length == 2) {
                        try {
                            collectOneResource(args[0], args[1], namespace, packCache, groups, loader);
                        } catch (Throwable resourceError) {
                            LOGGER.log(System.Logger.Level.ERROR,
                                    "Failed to materialize one resource from Palladium addon pack namespace " + namespace,
                                    unwrap(resourceError));
                        }
                    }
                    return null;
                };
                Object output = Proxy.newProxyInstance(loader, new Class<?>[] {outputClass}, callback);
                invoke(resources, new String[] {"listResources", "m_8031_"},
                        packType, namespace, SCRIPT_ROOT, output);
            }
        } finally {
            try {
                invoke(resources, new String[] {"close"});
            } catch (Throwable closeError) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to close a Palladium pack resource handle", unwrap(closeError));
            }
        }
    }

    private static void collectOneResource(
            Object resourceLocation,
            Object ioSupplier,
            String namespace,
            Path packCache,
            Map<String, ScriptGroup> groups,
            ClassLoader loader) throws Throwable {
        String resourcePath = String.valueOf(invoke(resourceLocation, new String[] {"getPath", "m_135815_"}));
        if (!resourcePath.startsWith(SCRIPT_ROOT + "/") || !resourcePath.endsWith(".js")) return;

        String relative = resourcePath.substring(SCRIPT_ROOT.length() + 1);
        Path namespaceRoot = packCache.resolve(safeSegment(namespace)).normalize();
        Path backingFile = namespaceRoot.resolve(relative).normalize();
        if (!backingFile.startsWith(namespaceRoot)) {
            LOGGER.log(System.Logger.Level.WARNING, "Rejected unsafe Palladium addon script resource path: {0}", resourcePath);
            return;
        }

        Files.createDirectories(backingFile.getParent());
        byte[] contents;
        try (InputStream input = openInputStream(ioSupplier, loader)) {
            contents = input.readAllBytes();
        }
        if (!Files.exists(backingFile) || !java.util.Arrays.equals(contents, Files.readAllBytes(backingFile))) {
            Path temporary = backingFile.resolveSibling(backingFile.getFileName() + ".tmp");
            Files.write(temporary, contents);
            try {
                Files.move(temporary, backingFile,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, backingFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        groups.computeIfAbsent(namespace, ScriptGroup::new).scripts.add(new AddonScript(backingFile, resourcePath));
    }

    private static InputStream openInputStream(Object supplier, ClassLoader loader) throws Throwable {
        try {
            Object stream = invoke(supplier, new String[] {"get", "m_247737_"});
            if (stream instanceof InputStream input) return input;
        } catch (Throwable ignored) {
            // Some mapped IoSupplier builds expose the SAM through an inherited interface method.
        }

        Class<?> ioSupplierClass = Class.forName("net.minecraft.server.packs.resources.IoSupplier", true, loader);
        for (Method method : ioSupplierClass.getMethods()) {
            if (method.getParameterCount() == 0) {
                Object stream = method.invoke(supplier);
                if (stream instanceof InputStream input) return input;
            }
        }
        throw new IOException("Could not invoke the selected pack's IoSupplier to open " + supplier);
    }

    private static Object publicField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getField(name);
        return field.get(owner);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new IllegalStateException("Expected a map, got " + (value == null ? "null" : value.getClass().getName()));
    }

    private static Collection<?> asCollection(Object value) {
        if (value instanceof Collection<?> collection) return collection;
        throw new IllegalStateException("Expected a collection, got " + (value == null ? "null" : value.getClass().getName()));
    }

    private static String stringResult(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static String safeSegment(String value) {
        String safe = value.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank() || safe.equals(".") || safe.equals("..")) return "unnamed";
        return safe;
    }

    private static Object invokeStatic(Class<?> owner, String[] names, Object... arguments) throws ReflectiveOperationException {
        Method method = compatibleMethod(owner, names, arguments);
        return method.invoke(null, arguments);
    }

    private static Object invoke(Object target, String[] names, Object... arguments) throws ReflectiveOperationException {
        Objects.requireNonNull(target, "Reflective invocation target");
        Method method = compatibleMethod(target.getClass(), names, arguments);
        return method.invoke(target, arguments);
    }

    private static Method compatibleMethod(Class<?> owner, String[] names, Object[] arguments) throws NoSuchMethodException {
        for (String name : names) {
            for (Method method : owner.getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != arguments.length) continue;
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean compatible = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (arguments[i] != null && !boxed(parameterTypes[i]).isInstance(arguments[i])) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) return method;
            }
        }
        throw new NoSuchMethodException(owner.getName() + " has no compatible method "
                + String.join("/", names) + " with " + arguments.length + " argument(s)");
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return Void.class;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof java.lang.reflect.InvocationTargetException
                || current instanceof java.lang.reflect.UndeclaredThrowableException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
