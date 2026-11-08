package legacyconnector.v1_7_10.codechicken.lib.vec;

import java.nio.DoubleBuffer;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** CodeChickenLib's geometric transformation of Vector3s; subclasses implement the vector and matrix forms. */
public abstract class Transformation extends ITransformation<Vector3, Transformation> {
    /** Transforms a normal (direction): the translation part is ignored. */
    public void applyN(Vector3 normal) {
        apply(normal);
    }

    /** Multiplies the matrix by this transformation. */
    public abstract void apply(Matrix4 mat);

    public Transformation at(Vector3 point) {
        return new TransformationList(new Translation(-point.x, -point.y, -point.z), this, point.translation());
    }

    public Transformation with(Transformation t) {
        return new TransformationList(this, t);
    }

    public Transformation merge(Transformation next) {
        return null;
    }

    /** Applies the transformation to the current OpenGL matrix. */
    public void glApply() {
        Matrix4 m = new Matrix4();
        apply(m);
        GL11.glMultMatrix(DoubleBuffer.wrap(m.toArrayColumnMajor()));
    }
}
