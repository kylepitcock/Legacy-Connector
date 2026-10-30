package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelRotation;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f;

/**
 * Forge 1.12.2's v1 blockstate format. Blockstate files are translated by the loader; what stays here is its table of named transforms
 * ({@link Transforms#get}), which mods use for their own models.
 */
public class ForgeBlockStateV1 {
    public static class Transforms {
        private static final ImmutableMap<TransformType, TRSRTransformation> BLOCK = ImmutableMap.<TransformType, TRSRTransformation>builder()
                .put(TransformType.GUI, t(30, 225, 0, 0, 0, 0, 0.625F))
                .put(TransformType.GROUND, t(0, 0, 0, 0, 3, 0, 0.25F))
                .put(TransformType.FIXED, t(0, 0, 0, 0, 0, 0, 0.5F))
                .put(TransformType.THIRD_PERSON_RIGHT_HAND, t(75, 45, 0, 0, 2.5F, 0, 0.375F))
                .put(TransformType.THIRD_PERSON_LEFT_HAND, t(75, 45, 0, 0, 2.5F, 0, 0.375F))
                .put(TransformType.FIRST_PERSON_RIGHT_HAND, t(0, 45, 0, 0, 0, 0, 0.4F))
                .put(TransformType.FIRST_PERSON_LEFT_HAND, t(0, 225, 0, 0, 0, 0, 0.4F))
                .build();
        private static final ImmutableMap<TransformType, TRSRTransformation> ITEM = ImmutableMap.<TransformType, TRSRTransformation>builder()
                .put(TransformType.GROUND, t(0, 0, 0, 0, 2, 0, 0.5F))
                .put(TransformType.HEAD, t(0, 180, 0, 0, 13, 7, 1.0F))
                .put(TransformType.THIRD_PERSON_RIGHT_HAND, t(0, 0, 0, 0, 3, 1, 0.55F))
                .put(TransformType.THIRD_PERSON_LEFT_HAND, t(0, 0, 0, 0, 3, 1, 0.55F))
                .put(TransformType.FIRST_PERSON_RIGHT_HAND, t(0, -90, 25, 1.13F, 3.2F, 1.13F, 0.68F))
                .put(TransformType.FIRST_PERSON_LEFT_HAND, t(0, 90, -25, 1.13F, 3.2F, 1.13F, 0.68F))
                .put(TransformType.FIXED, t(0, 180, 0, 0, 0, 0, 1.0F))
                .build();
        private static final ImmutableMap<TransformType, TRSRTransformation> TOOL = ImmutableMap.<TransformType, TRSRTransformation>builder()
                .put(TransformType.GROUND, t(0, 0, 0, 0, 2, 0, 0.5F))
                .put(TransformType.HEAD, t(0, 180, 0, 0, 13, 7, 1.0F))
                .put(TransformType.THIRD_PERSON_RIGHT_HAND, t(0, -90, 55, 0, 4.0F, 0.5F, 0.85F))
                .put(TransformType.THIRD_PERSON_LEFT_HAND, t(0, 90, -55, 0, 4.0F, 0.5F, 0.85F))
                .put(TransformType.FIRST_PERSON_RIGHT_HAND, t(0, -90, 25, 1.13F, 3.2F, 1.13F, 0.68F))
                .put(TransformType.FIRST_PERSON_LEFT_HAND, t(0, 90, -25, 1.13F, 3.2F, 1.13F, 0.68F))
                .put(TransformType.FIXED, t(0, 180, 0, 0, 0, 0, 1.0F))
                .build();
        public static final IModelState DEFAULT_BLOCK = new SimpleModelState(BLOCK);
        public static final IModelState DEFAULT_ITEM = new SimpleModelState(ITEM);
        public static final IModelState DEFAULT_TOOL = new SimpleModelState(TOOL);

        /** A display transform from a model json's numbers (rotation in degrees, translation in pixels). */
        private static TRSRTransformation t(float rx, float ry, float rz, float tx, float ty, float tz, float s) {
            return new TRSRTransformation(new ItemTransformVec3f(new Vector3f(rx, ry, rz), new Vector3f(tx / 16.0F, ty / 16.0F, tz / 16.0F), new Vector3f(s, s, s)));
        }

        /**
         * A named transform: {@code identity}/{@code none}, Forge's {@code forge:default-block}, {@code -item} and {@code -tool}, or a
         * block rotation such as {@code x90} or {@code x90_y180} (ModelRotation's own names).
         */
        public static Optional<IModelState> get(String name) {
            if (name == null) return Optional.empty();
            switch (name) {
                case "identity", "none" -> {
                    return Optional.of(TRSRTransformation.identity());
                }
                case "forge:default-block" -> {
                    return Optional.of(DEFAULT_BLOCK);
                }
                case "forge:default-item" -> {
                    return Optional.of(DEFAULT_ITEM);
                }
                case "forge:default-tool" -> {
                    return Optional.of(DEFAULT_TOOL);
                }
                default -> {
                }
            }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:x(\\d+))?_?(?:y(\\d+))?").matcher(name.toLowerCase(java.util.Locale.ROOT));
            if (!m.matches() || (m.group(1) == null && m.group(2) == null)) return Optional.empty();
            int x = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
            int y = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            ModelRotation rotation = ModelRotation.getModelRotation(x, y);
            return Optional.of(new TRSRTransformation(rotation));
        }
    }

    /**
     * Forge's transform json: a named transform ({@link Transforms#get}), a 3x4 matrix, or an object with {@code translation},
     * {@code rotation} (a quaternion, or single-axis degrees such as {@code {"y": 90}} or a list of them), {@code scale} and
     * {@code post-rotation}.
     */
    public static class TRSRDeserializer implements com.google.gson.JsonDeserializer<TRSRTransformation> {
        public static final TRSRDeserializer INSTANCE = new TRSRDeserializer();

        @Override
        public TRSRTransformation deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                String name = json.getAsString();
                Optional<IModelState> state = Transforms.get(name);
                if (!state.isPresent()) throw new com.google.gson.JsonParseException("transform: unknown default string: " + name);
                Optional<TRSRTransformation> t = state.get().apply(Optional.empty());
                return t.orElse(TRSRTransformation.identity());
            }
            if (json.isJsonArray()) return new TRSRTransformation(parseMatrix(json));
            if (!json.isJsonObject()) throw new com.google.gson.JsonParseException("transform: expected a string, object or array, got: " + json);
            com.google.gson.JsonObject obj = json.getAsJsonObject().deepCopy();
            if (obj.has("matrix")) {
                TRSRTransformation t = new TRSRTransformation(parseMatrix(obj.get("matrix")));
                obj.remove("matrix");
                if (obj.size() > 0) throw new com.google.gson.JsonParseException("transform: can't combine matrix and other keys");
                return t;
            }
            javax.vecmath.Vector3f translation = new javax.vecmath.Vector3f(0, 0, 0);
            javax.vecmath.Quat4f leftRot = new javax.vecmath.Quat4f(0, 0, 0, 1);
            javax.vecmath.Vector3f scale = new javax.vecmath.Vector3f(1, 1, 1);
            javax.vecmath.Quat4f rightRot = new javax.vecmath.Quat4f(0, 0, 0, 1);
            if (obj.has("translation")) {
                float[] f = parseFloats(obj.remove("translation"), 3, "translation");
                translation = new javax.vecmath.Vector3f(f[0], f[1], f[2]);
            }
            if (obj.has("rotation")) leftRot = parseRotation(obj.remove("rotation"));
            if (obj.has("scale")) {
                com.google.gson.JsonElement e = obj.remove("scale");
                if (e.isJsonPrimitive()) {
                    float f = e.getAsFloat();
                    scale = new javax.vecmath.Vector3f(f, f, f);
                } else {
                    float[] f = parseFloats(e, 3, "scale");
                    scale = new javax.vecmath.Vector3f(f[0], f[1], f[2]);
                }
            }
            if (obj.has("post-rotation")) rightRot = parseRotation(obj.remove("post-rotation"));
            if (obj.size() > 0) throw new com.google.gson.JsonParseException("transform: extra keys in " + obj);
            return new TRSRTransformation(translation, leftRot, scale, rightRot);
        }

        private static float[] parseFloats(com.google.gson.JsonElement e, int n, String what) {
            if (!e.isJsonArray() || e.getAsJsonArray().size() != n) throw new com.google.gson.JsonParseException("transform: " + what + ": expected " + n + " numbers");
            float[] out = new float[n];
            for (int i = 0; i < n; i++) out[i] = e.getAsJsonArray().get(i).getAsFloat();
            return out;
        }

        private static javax.vecmath.Matrix4f parseMatrix(com.google.gson.JsonElement e) {
            if (!e.isJsonArray() || e.getAsJsonArray().size() != 3) throw new com.google.gson.JsonParseException("transform: matrix: expected 3 rows of 4");
            javax.vecmath.Matrix4f m = new javax.vecmath.Matrix4f();
            m.setIdentity();
            for (int row = 0; row < 3; row++) {
                float[] r = parseFloats(e.getAsJsonArray().get(row), 4, "matrix row");
                for (int col = 0; col < 4; col++) m.setElement(row, col, r[col]);
            }
            return m;
        }

        private static javax.vecmath.Quat4f parseAxisRotation(com.google.gson.JsonElement e) {
            if (!e.isJsonObject() || e.getAsJsonObject().size() != 1) throw new com.google.gson.JsonParseException("transform: axis rotation: expected one axis");
            var entry = e.getAsJsonObject().entrySet().iterator().next();
            float angle = (float) Math.toRadians(entry.getValue().getAsFloat());
            javax.vecmath.Vector3f axis = switch (entry.getKey()) {
                case "x" -> new javax.vecmath.Vector3f(1, 0, 0);
                case "y" -> new javax.vecmath.Vector3f(0, 1, 0);
                case "z" -> new javax.vecmath.Vector3f(0, 0, 1);
                default -> throw new com.google.gson.JsonParseException("transform: axis rotation: unknown axis " + entry.getKey());
            };
            javax.vecmath.Quat4f q = new javax.vecmath.Quat4f();
            q.set(new javax.vecmath.AxisAngle4f(axis, angle));
            return q;
        }

        private static javax.vecmath.Quat4f parseRotation(com.google.gson.JsonElement e) {
            if (e.isJsonArray()) {
                var array = e.getAsJsonArray();
                if (array.size() == 4 && array.get(0).isJsonPrimitive()) {
                    float[] f = parseFloats(e, 4, "rotation");
                    return new javax.vecmath.Quat4f(f[0], f[1], f[2], f[3]);
                }
                javax.vecmath.Quat4f q = new javax.vecmath.Quat4f(0, 0, 0, 1);
                for (com.google.gson.JsonElement part : array) q.mul(parseAxisRotation(part));
                return q;
            }
            return parseAxisRotation(e);
        }
    }
}
