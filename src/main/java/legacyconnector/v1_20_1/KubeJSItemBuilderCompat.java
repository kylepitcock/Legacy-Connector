package legacyconnector.v1_20_1;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Narrow KubeJS 6 to KubeJS 7 adapter for the old BasicItemJS.Builder glow
 * configuration pattern used by addon-pack scripts.
 *
 * <p>KubeJS 7 represents the glow override as Minecraft's
 * ENCHANTMENT_GLINT_OVERRIDE item component. The component is resolved lazily
 * so merely loading this optional compatibility helper does not initialize
 * Minecraft's component registry (for example, during source-rewrite tests).</p>
 */
public final class KubeJSItemBuilderCompat {
    private static final String OLD_BASIC_ITEM_BUILDER =
            "dev.latvian.mods.kubejs.item.custom.BasicItemJS$Builder";
    private static final String KUBEJS7_ITEM_BUILDER =
            "dev.latvian.mods.kubejs.item.ItemBuilder";
    private static final String DATA_COMPONENTS =
            "net.minecraft.core.component.DataComponents";
    private static final String GLINT_OVERRIDE_FIELD = "ENCHANTMENT_GLINT_OVERRIDE";

    private KubeJSItemBuilderCompat() {
    }

    /** Restores the old ItemBuilder.glow(boolean) script method. */
    public static Object glow(Object builder, boolean enabled) {
        Objects.requireNonNull(builder, "KubeJS item builder");
        return glow(builder, enabled, glintOverrideComponent(builder.getClass().getClassLoader()));
    }

    /**
     * Testable implementation that accepts the component key explicitly.
     * Production callers use the public overload above.
     */
    static Object glow(Object builder, boolean enabled, Object glintOverride) {
        Objects.requireNonNull(builder, "KubeJS item builder");
        Objects.requireNonNull(glintOverride, "Minecraft enchantment glint override component");
        try {
            Field field = publicField(builder, "components");
            Object value = field.get(builder);
            Map<Object, Object> components;
            if (value == null) {
                components = new HashMap<>();
                field.set(builder, components);
            } else if (value instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> typed = (Map<Object, Object>) map;
                components = typed;
            } else {
                throw new IllegalStateException("KubeJS ItemBuilder.components is not a Map");
            }

            // null means clear the component, preserving vanilla enchantment
            // glint behavior when the old builder's glow flag is false.
            components.put(glintOverride, enabled ? Boolean.TRUE : null);
            return builder;
        } catch (ReflectiveOperationException error) {
            throw compatibilityFailure("glow(boolean)", error);
        }
    }

    /**
     * Adapts the exact KubeJS 6 pattern used by glow_crystals.js. We intentionally
     * copy only the verified glint override; any other builder component fails
     * closed until its KubeJS 7 behavior has been audited separately.
     */
    public static void setItemBuilder(Object modifications, Object builder) {
        Objects.requireNonNull(builder, "KubeJS item builder");
        setItemBuilder(modifications, builder,
                glintOverrideComponent(builder.getClass().getClassLoader()));
    }

    /** Testable implementation that accepts the component key explicitly. */
    static void setItemBuilder(Object modifications, Object builder, Object glintOverride) {
        Objects.requireNonNull(modifications, "KubeJS item modification event");
        Objects.requireNonNull(builder, "KubeJS item builder");
        Objects.requireNonNull(glintOverride, "Minecraft enchantment glint override component");

        try {
            Object raw = publicField(builder, "components").get(builder);
            if (!(raw instanceof Map<?, ?> components) || !components.containsKey(glintOverride)) {
                throw new IllegalStateException(
                        "Legacy setItemBuilder requires an explicit glow(boolean) setting");
            }
            for (Object key : components.keySet()) {
                if (!glintOverride.equals(key)) {
                    throw new UnsupportedOperationException(
                            "Legacy setItemBuilder only adapts the verified enchantment glint component; found " + key);
                }
            }

            Object value = Boolean.TRUE.equals(components.get(glintOverride)) ? Boolean.TRUE : null;
            Method override = findComponentOverride(modifications.getClass(), glintOverride);
            override.invoke(modifications, glintOverride, value);
        } catch (ReflectiveOperationException error) {
            throw compatibilityFailure("ItemModifications.setItemBuilder(builder)", error);
        }
    }

    private static Object glintOverrideComponent(ClassLoader loader) {
        try {
            Class<?> dataComponents = Class.forName(DATA_COMPONENTS, true, loader);
            return dataComponents.getField(GLINT_OVERRIDE_FIELD).get(null);
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure("resolving DataComponents." + GLINT_OVERRIDE_FIELD, error);
        }
    }

    /**
     * Locate the mixin-added kjs$override method without eagerly linking this
     * optional adapter to Minecraft's component classes.
     */
    private static Method findComponentOverride(Class<?> modificationsClass, Object component)
            throws NoSuchMethodException {
        for (Method method : modificationsClass.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getName().equals("kjs$override")
                    && parameters.length == 2
                    && parameters[0].isInstance(component)
                    && parameters[1] == Object.class) {
                return method;
            }
        }
        throw new NoSuchMethodException(
                modificationsClass.getName() + ".kjs$override(" + component.getClass().getName() + ", Object)");
    }

    /**
     * Rewrites only scripts that use the inspected KubeJS 6 builder class
     * together with both glow() and setItemBuilder(). Other scripts are left
     * byte-for-byte unchanged.
     */
    public static byte[] rewriteLegacyBuilderSource(byte[] source) {
        Objects.requireNonNull(source, "KubeJS addon script source");
        String script = new String(source, StandardCharsets.UTF_8);
        if (!script.contains(OLD_BASIC_ITEM_BUILDER)
                || !script.contains("setItemBuilder(")
                || !script.contains(".glow(")) {
            return source;
        }

        String rewritten = script.replace(OLD_BASIC_ITEM_BUILDER, KUBEJS7_ITEM_BUILDER);
        return rewritten.equals(script) ? source : rewritten.getBytes(StandardCharsets.UTF_8);
    }

    private static Field publicField(Object owner, String name) throws NoSuchFieldException {
        return owner.getClass().getField(name);
    }

    private static IllegalStateException compatibilityFailure(String operation, Throwable cause) {
        Throwable root = cause instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : cause;
        return new IllegalStateException(
                "KubeJS 7 item-builder " + operation + " compatibility could not access the inspected public API",
                root);
    }
}
