package legacyconnector.v1_20_1;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Restores KubeJS 6 BlockBuilder methods used by Forge 1.20.1 addon-pack scripts
 * on KubeJS 7 / Minecraft 1.21.1.
 *
 * <p>This is deliberately reflective: KubeJS is optional and the connector must
 * remain loadable when KubeJS is absent. The public fields used here are part of
 * the inspected KubeJS 7.1.x builder API.</p>
 */
public final class KubeJSBlockBuilderCompat {
    private static final String RESOURCE_LOCATION = "net.minecraft.resources.ResourceLocation";
    private static final String CARDINAL_BUILDER =
            "dev.latvian.mods.kubejs.block.custom.CardinalBlockBuilder";

    private static final List<String> LEGACY_BLOCK_FACES =
            List.of("down", "up", "north", "south", "west", "east");

    private KubeJSBlockBuilderCompat() {
    }

    /**
     * KubeJS 6 BlockBuilder.model(String) selected the supplied block model.
     * KubeJS 7's normal block-model path adds a generated full-cube element
     * whenever tint callbacks are present, even when a custom parent model was
     * selected. That element overrides the parent model's own geometry and
     * refers to directional texture keys that legacy custom models need not
     * define. Use modelGenerator to emit only the requested parent and texture
     * overrides; the block tint callbacks remain registered on the builder.
     *
     * <p>The item builder keeps its own parentModel because it has a separate
     * asset-generation path.</p>
     */
    public static Object model(Object builder, String model) {
        Objects.requireNonNull(builder, "KubeJS block builder");

        try {
            Object parent = parseModelLocation(builder, model);
            publicField(builder, "parentModel").set(builder, parent);

            Field modelGeneratorField = publicField(builder, "modelGenerator");
            if (parent == null) {
                modelGeneratorField.set(builder, null);
            } else {
                Consumer<Object> customModel = generatedModel -> {
                    try {
                        Method parentSetter = generatedModel.getClass()
                                .getMethod("parent", parent.getClass());
                        parentSetter.invoke(generatedModel, parent);

                        Object rawTextures = publicField(builder, "textures").get(builder);
                        if (!(rawTextures instanceof Map<?, ?> textures)) {
                            throw new IllegalStateException("KubeJS BlockBuilder.textures is not a Map");
                        }
                        Method textureSetter = generatedModel.getClass().getMethod("textures", Map.class);
                        textureSetter.invoke(generatedModel, textures);
                    } catch (ReflectiveOperationException error) {
                        throw compatibilityFailure("model(String) model generator", error);
                    }
                };
                modelGeneratorField.set(builder, customModel);
            }

            Object itemBuilder = publicField(builder, "itemBuilder").get(builder);
            if (itemBuilder != null) {
                publicField(itemBuilder, "parentModel").set(itemBuilder, parent);
            }
            return builder;
        } catch (ReflectiveOperationException error) {
            throw compatibilityFailure("model(String)", error);
        }
    }

    /**
     * KubeJS 6 BlockBuilder.textureAll(String) populated all six face textures
     * and the particle texture. KubeJS 7's generic texture(String) only changes
     * baseTexture, so it is not an equivalent replacement.
     */
    public static Object textureAll(Object builder, String texture) {
        Objects.requireNonNull(builder, "KubeJS block builder");
        Objects.requireNonNull(texture, "KubeJS block texture");

        try {
            Object value = publicField(builder, "textures").get(builder);
            if (!(value instanceof Map<?, ?> rawMap)) {
                throw new IllegalStateException("KubeJS BlockBuilder.textures is not a Map");
            }

            @SuppressWarnings("unchecked")
            Map<String, String> textures = (Map<String, String>) rawMap;
            applyTextureAll(textures, texture, isCardinalBuilder(builder.getClass()));
            return builder;
        } catch (ReflectiveOperationException error) {
            throw compatibilityFailure("textureAll(String)", error);
        }
    }

    static void applyTextureAll(Map<String, String> textures, String texture, boolean cardinalBuilder) {
        Objects.requireNonNull(textures, "KubeJS block texture map");
        Objects.requireNonNull(texture, "KubeJS block texture");

        for (String face : LEGACY_BLOCK_FACES) {
            textures.put(face, texture);
        }
        textures.put("particle", texture);

        // KubeJS 6's horizontal/cardinal builder additionally wrote "side".
        // KubeJS 7's CardinalBlockBuilder reads side/top/front/particle instead
        // of the six directional keys when generating its orientable model.
        if (cardinalBuilder) {
            // CardinalBlockBuilder generates an orientable model from these keys,
            // not from the six directional face entries above.
            textures.put("side", texture);
            textures.put("front", texture);
            textures.put("top", texture);
            textures.put("bottom", texture);
        }
    }

    private static Object parseModelLocation(Object builder, String model) throws ReflectiveOperationException {
        if (model == null || model.isBlank()) {
            // The old builder used an empty string for the default model. In
            // KubeJS 7, a null parentModel selects the generated default.
            return null;
        }

        ClassLoader loader = builder.getClass().getClassLoader();
        Class<?> resourceLocationClass = Class.forName(RESOURCE_LOCATION, true, loader);
        Method parse = resourceLocationClass.getMethod("parse", String.class);
        try {
            return parse.invoke(null, model);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw error;
        }
    }

    private static boolean isCardinalBuilder(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (CARDINAL_BUILDER.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    private static Field publicField(Object owner, String name) throws NoSuchFieldException {
        return owner.getClass().getField(name);
    }

    private static IllegalStateException compatibilityFailure(String operation, Throwable cause) {
        Throwable root = cause instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : cause;
        return new IllegalStateException(
                "KubeJS 7 BlockBuilder " + operation + " compatibility expected the inspected public builder fields",
                root);
    }
}
