package legacyconnector.v1_7_10.codechicken.lib.vec;

/** CodeChickenLib's generic transformation of a vector type (Vector3 for the geometry ones, UV for texture coordinates). */
public abstract class ITransformation<Vector, Transform extends ITransformation<Vector, Transform>> {
    /** Transforms the vector in place. */
    public abstract void apply(Vector vec);

    /** This transformation applied around the point instead of the origin. */
    public abstract Transform at(Vector point);

    /** This transformation followed by the other one. */
    public abstract Transform with(Transform t);

    public abstract Transform inverse();

    /** A single transformation doing both, or null when none can express the pair. */
    public Transform merge(Transform next) {
        return null;
    }

    public abstract boolean isRedundant();
}
