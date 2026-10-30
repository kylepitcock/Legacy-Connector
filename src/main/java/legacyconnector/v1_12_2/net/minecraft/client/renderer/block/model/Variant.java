package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** One model choice of a blockstate variant: the model, its rotation, uv lock and weight among the variant's choices. */
public class Variant {
    private final ResourceLocation modelLocation;
    private final ModelRotation rotation;
    private final boolean uvLock;
    private final int weight;

    public Variant(ResourceLocation modelLocationIn, ModelRotation rotationIn, boolean uvLockIn, int weightIn) {
        modelLocation = modelLocationIn;
        rotation = rotationIn;
        uvLock = uvLockIn;
        weight = weightIn;
    }

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public ModelRotation getRotation() {
        return rotation;
    }

    public boolean isUvLock() {
        return uvLock;
    }

    public int getWeight() {
        return weight;
    }

    /** Forge: the variant's transform as a model state. */
    public legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState getState() {
        return rotation;
    }

    @Override
    public String toString() {
        return "Variant{modelLocation=" + modelLocation + ", rotation=" + rotation + ", uvLock=" + uvLock + ", weight=" + weight + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variant v)) return false;
        return java.util.Objects.equals(modelLocation, v.modelLocation) && rotation == v.rotation && uvLock == v.uvLock && weight == v.weight;
    }

    @Override
    public int hashCode() {
        int i = modelLocation == null ? 0 : modelLocation.hashCode();
        i = 31 * i + (rotation != null ? rotation.hashCode() : 0);
        i = 31 * i + (uvLock ? 1 : 0);
        return 31 * i + weight;
    }
}
