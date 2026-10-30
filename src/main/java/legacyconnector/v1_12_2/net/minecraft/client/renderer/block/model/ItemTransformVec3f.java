package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f;

/** One display transform of a model (rotation in degrees, translation in blocks, scale). */
public class ItemTransformVec3f {
    public static final ItemTransformVec3f DEFAULT = new ItemTransformVec3f(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
    public final Vector3f rotation;
    public final Vector3f translation;
    public final Vector3f scale;

    public ItemTransformVec3f(Vector3f rotation, Vector3f translation, Vector3f scale) {
        this.rotation = new Vector3f(rotation);
        this.translation = new Vector3f(translation);
        this.scale = new Vector3f(scale);
    }

    /** Applies the transform to the GL matrix (render context), mirrored for the left hand as vanilla did. */
    public void apply(boolean leftHand) {
        if (this == DEFAULT) return;
        int i = leftHand ? -1 : 1;
        legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.translate(i * translation.x, translation.y, translation.z);
        float rotY = rotation.y * i, rotZ = rotation.z * i;
        // the XYZ euler rotation vanilla built its quaternion from: X, then Y, then Z in the model's frame
        legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.rotate(rotation.x, 1.0F, 0.0F, 0.0F);
        legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.rotate(rotY, 0.0F, 1.0F, 0.0F);
        legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.rotate(rotZ, 0.0F, 0.0F, 1.0F);
        legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.scale(scale.x, scale.y, scale.z);
    }

    /** The modern transform with the same values. */
    public net.minecraft.client.renderer.block.model.ItemTransform mcc$modern() {
        return new net.minecraft.client.renderer.block.model.ItemTransform(new org.joml.Vector3f(rotation.x, rotation.y, rotation.z),
                new org.joml.Vector3f(translation.x, translation.y, translation.z), new org.joml.Vector3f(scale.x, scale.y, scale.z));
    }

    public static ItemTransformVec3f mcc$of(net.minecraft.client.renderer.block.model.ItemTransform t) {
        if (t == null || t == net.minecraft.client.renderer.block.model.ItemTransform.NO_TRANSFORM) return DEFAULT;
        return new ItemTransformVec3f(new Vector3f(t.rotation.x(), t.rotation.y(), t.rotation.z()), new Vector3f(t.translation.x(), t.translation.y(), t.translation.z()),
                new Vector3f(t.scale.x(), t.scale.y(), t.scale.z()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemTransformVec3f other)) return false;
        return rotation.equals(other.rotation) && scale.equals(other.scale) && translation.equals(other.translation);
    }

    @Override
    public int hashCode() {
        int i = rotation.hashCode();
        i = 31 * i + translation.hashCode();
        return 31 * i + scale.hashCode();
    }

    /** Reads a transform from a model's {@code display} entry: translation in pixels (clamped to 5 blocks), scale clamped to 4. */
    public static class Deserializer implements com.google.gson.JsonDeserializer<ItemTransformVec3f> {
        public static final Vector3f ROTATION_DEFAULT = new Vector3f(0.0F, 0.0F, 0.0F);
        public static final Vector3f TRANSLATION_DEFAULT = new Vector3f(0.0F, 0.0F, 0.0F);
        public static final Vector3f SCALE_DEFAULT = new Vector3f(1.0F, 1.0F, 1.0F);

        @Override
        public ItemTransformVec3f deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            com.google.gson.JsonObject o = json.getAsJsonObject();
            Vector3f rotation = parseVector3f(o, "rotation", ROTATION_DEFAULT);
            Vector3f translation = parseVector3f(o, "translation", TRANSLATION_DEFAULT);
            translation.x = Math.max(-5.0F, Math.min(5.0F, translation.x * 0.0625F));
            translation.y = Math.max(-5.0F, Math.min(5.0F, translation.y * 0.0625F));
            translation.z = Math.max(-5.0F, Math.min(5.0F, translation.z * 0.0625F));
            Vector3f scale = parseVector3f(o, "scale", SCALE_DEFAULT);
            scale.x = Math.max(-4.0F, Math.min(4.0F, scale.x));
            scale.y = Math.max(-4.0F, Math.min(4.0F, scale.y));
            scale.z = Math.max(-4.0F, Math.min(4.0F, scale.z));
            return new ItemTransformVec3f(rotation, translation, scale);
        }

        private Vector3f parseVector3f(com.google.gson.JsonObject json, String key, Vector3f fallback) {
            if (!json.has(key)) return new Vector3f(fallback);
            com.google.gson.JsonArray array = json.getAsJsonArray(key);
            if (array.size() != 3) throw new com.google.gson.JsonParseException("Expected 3 " + key + " values, found: " + array.size());
            return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
        }
    }
}
