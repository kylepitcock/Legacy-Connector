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
}
