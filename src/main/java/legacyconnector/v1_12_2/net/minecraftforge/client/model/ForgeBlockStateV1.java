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
}
