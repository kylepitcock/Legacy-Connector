package legacyconnector.v1_12_2.net.minecraftforge.common.model;

import java.util.EnumMap;
import java.util.Optional;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelRotation;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/**
 * Forge's affine transform as translation, left rotation, scale and right rotation, kept as its 4x4 matrix. The parts are
 * derived from the matrix when asked (polar decomposition: the rotation goes left, the right rotation is the identity, which is
 * exact for the rotation-and-scale transforms models use).
 */
public final class TRSRTransformation implements IModelState, ITransformation {
    private final Matrix4f matrix;
    private boolean full;
    private Vector3f translation;
    private Quat4f leftRot;
    private Vector3f scale;
    private Quat4f rightRot;
    private Matrix3f normalTransform;

    public TRSRTransformation(Matrix4f matrix) {
        this.matrix = matrix == null ? identityMatrix() : new Matrix4f(matrix);
    }

    public TRSRTransformation(Vector3f translation, Quat4f leftRot, Vector3f scale, Quat4f rightRot) {
        matrix = mul(translation, leftRot, scale, rightRot);
        this.translation = translation != null ? translation : new Vector3f();
        this.leftRot = leftRot != null ? leftRot : new Quat4f(0, 0, 0, 1);
        this.scale = scale != null ? scale : new Vector3f(1, 1, 1);
        this.rightRot = rightRot != null ? rightRot : new Quat4f(0, 0, 0, 1);
        full = true;
    }

    public TRSRTransformation(ItemTransformVec3f transform) {
        this(toVecmath(transform.translation), quatFromXYZDegrees(toVecmath(transform.rotation)), toVecmath(transform.scale), null);
    }

    public TRSRTransformation(ModelRotation rotation) {
        this(rotation.getMatrix());
    }

    public TRSRTransformation(EnumFacing facing) {
        this(getMatrix(facing));
    }

    private static Vector3f toVecmath(legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f v) {
        return new Vector3f(v.x, v.y, v.z);
    }

    private static Matrix4f identityMatrix() {
        Matrix4f m = new Matrix4f();
        m.setIdentity();
        return m;
    }

    private static final TRSRTransformation IDENTITY = new TRSRTransformation(identityMatrix());

    public static TRSRTransformation identity() {
        return IDENTITY;
    }

    public static Matrix4f mul(Vector3f translation, Quat4f leftRot, Vector3f scale, Quat4f rightRot) {
        Matrix4f res = new Matrix4f(), t = new Matrix4f();
        res.setIdentity();
        if (leftRot != null) {
            t.set(leftRot);
            res.mul(t);
        }
        if (scale != null) {
            t.setIdentity();
            t.m00 = scale.x;
            t.m11 = scale.y;
            t.m22 = scale.z;
            res.mul(t);
        }
        if (rightRot != null) {
            t.set(rightRot);
            res.mul(t);
        }
        if (translation != null) res.setTranslation(translation);
        return res;
    }

    public TRSRTransformation compose(TRSRTransformation b) {
        if (isIdentity()) return b;
        if (b.isIdentity()) return this;
        Matrix4f m = getMatrix();
        m.mul(b.getMatrix());
        return new TRSRTransformation(m);
    }

    public TRSRTransformation inverse() {
        if (isIdentity()) return this;
        Matrix4f m = getMatrix();
        m.invert();
        return new TRSRTransformation(m);
    }

    public boolean isIdentity() {
        return this == IDENTITY || matrix.equals(IDENTITY.matrix);
    }

    @Override
    public Matrix4f getMatrix() {
        return (Matrix4f) matrix.clone();
    }

    private void genCheck() {
        if (full) return;
        translation = new Vector3f(matrix.m03, matrix.m13, matrix.m23);
        Vector3f c0 = new Vector3f(matrix.m00, matrix.m10, matrix.m20), c1 = new Vector3f(matrix.m01, matrix.m11, matrix.m21),
                c2 = new Vector3f(matrix.m02, matrix.m12, matrix.m22);
        float sx = c0.length(), sy = c1.length(), sz = c2.length();
        Matrix3f r = new Matrix3f();
        matrix.getRotationScale(r);
        if (r.determinant() < 0) sx = -sx;
        scale = new Vector3f(sx, sy, sz);
        Matrix3f rot = new Matrix3f(sx == 0 ? 1 : matrix.m00 / sx, sy == 0 ? 0 : matrix.m01 / sy, sz == 0 ? 0 : matrix.m02 / sz,
                sx == 0 ? 0 : matrix.m10 / sx, sy == 0 ? 1 : matrix.m11 / sy, sz == 0 ? 0 : matrix.m12 / sz,
                sx == 0 ? 0 : matrix.m20 / sx, sy == 0 ? 0 : matrix.m21 / sy, sz == 0 ? 1 : matrix.m22 / sz);
        leftRot = new Quat4f();
        leftRot.set(rot);
        rightRot = new Quat4f(0, 0, 0, 1);
        full = true;
    }

    public Vector3f getTranslation() {
        genCheck();
        return (Vector3f) translation.clone();
    }

    public Quat4f getLeftRot() {
        genCheck();
        return (Quat4f) leftRot.clone();
    }

    public Vector3f getScale() {
        genCheck();
        return (Vector3f) scale.clone();
    }

    public Quat4f getRightRot() {
        genCheck();
        return (Quat4f) rightRot.clone();
    }

    @Override
    public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
        if (part.isPresent()) return Optional.empty();
        return Optional.of(this);
    }

    @Override
    public EnumFacing rotate(EnumFacing facing) {
        return rotate(matrix, facing);
    }

    public static EnumFacing rotate(Matrix4f matrix, EnumFacing facing) {
        var vec = facing.getDirectionVec();
        Vector4f v = new Vector4f(vec.getX(), vec.getY(), vec.getZ(), 0);
        matrix.transform(v);
        EnumFacing best = facing;
        float bestDot = -Float.MAX_VALUE;
        for (EnumFacing f : EnumFacing.values()) {
            var d = f.getDirectionVec();
            float dot = v.x * d.getX() + v.y * d.getY() + v.z * d.getZ();
            if (dot > bestDot) {
                bestDot = dot;
                best = f;
            }
        }
        return best;
    }

    public static boolean isInteger(Matrix4f matrix) {
        Matrix4f m = new Matrix4f();
        m.setIdentity();
        m.m30 = m.m31 = m.m32 = 1;
        m.m33 = 0;
        m.mul(matrix, m);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                float v = m.getElement(i, j) / m.getElement(3, j);
                if (Math.abs(v - Math.round(v)) > 1e-5) return false;
            }
        }
        return true;
    }

    @Override
    public int rotate(EnumFacing facing, int vertexIndex) {
        return vertexIndex;
    }

    public Matrix3f getNormalMatrix() {
        if (normalTransform == null) {
            normalTransform = new Matrix3f();
            matrix.getRotationScale(normalTransform);
            normalTransform.invert();
            normalTransform.transpose();
        }
        return (Matrix3f) normalTransform.clone();
    }

    public void transformPosition(Vector4f position) {
        matrix.transform(position);
    }

    public void transformNormal(Vector3f normal) {
        getNormalMatrix().transform(normal);
        normal.normalize();
    }

    public ItemTransformVec3f toItemTransform() {
        genCheck();
        Vector3f euler = toYXZDegrees(leftRot);
        return new ItemTransformVec3f(new legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f(euler.x, euler.y, euler.z),
                new legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f(translation.x, translation.y, translation.z),
                new legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f(scale.x, scale.y, scale.z));
    }

    private static Vector3f toYXZDegrees(Quat4f q) {
        Matrix3f m = new Matrix3f();
        m.set(q);
        float x = (float) Math.asin(Math.max(-1, Math.min(1, -m.m12)));
        float y = (float) Math.atan2(m.m02, m.m22);
        float z = (float) Math.atan2(m.m10, m.m11);
        return new Vector3f((float) Math.toDegrees(x), (float) Math.toDegrees(y), (float) Math.toDegrees(z));
    }

    public static Quat4f quatFromXYZDegrees(Vector3f xyz) {
        return quatFromXYZ((float) Math.toRadians(xyz.x), (float) Math.toRadians(xyz.y), (float) Math.toRadians(xyz.z));
    }

    public static Quat4f quatFromXYZ(Vector3f xyz) {
        return quatFromXYZ(xyz.x, xyz.y, xyz.z);
    }

    public static Quat4f quatFromXYZ(float x, float y, float z) {
        Quat4f ret = new Quat4f(0, 0, 0, 1), t = new Quat4f();
        t.set((float) Math.sin(x / 2), 0, 0, (float) Math.cos(x / 2));
        ret.mul(t);
        t.set(0, (float) Math.sin(y / 2), 0, (float) Math.cos(y / 2));
        ret.mul(t);
        t.set(0, 0, (float) Math.sin(z / 2), (float) Math.cos(z / 2));
        ret.mul(t);
        return ret;
    }

    public static Quat4f quatFromYXZDegrees(Vector3f yxz) {
        return quatFromYXZ((float) Math.toRadians(yxz.x), (float) Math.toRadians(yxz.y), (float) Math.toRadians(yxz.z));
    }

    public static Quat4f quatFromYXZ(Vector3f yxz) {
        return quatFromYXZ(yxz.x, yxz.y, yxz.z);
    }

    public static Quat4f quatFromYXZ(float y, float x, float z) {
        Quat4f ret = new Quat4f(0, 0, 0, 1), t = new Quat4f();
        t.set(0, (float) Math.sin(y / 2), 0, (float) Math.cos(y / 2));
        ret.mul(t);
        t.set((float) Math.sin(x / 2), 0, 0, (float) Math.cos(x / 2));
        ret.mul(t);
        t.set(0, 0, (float) Math.sin(z / 2), (float) Math.cos(z / 2));
        ret.mul(t);
        return ret;
    }

    public static TRSRTransformation from(ItemTransformVec3f transform) {
        return transform == null || transform.equals(ItemTransformVec3f.DEFAULT) ? IDENTITY : new TRSRTransformation(transform);
    }

    public static TRSRTransformation from(ModelRotation rotation) {
        return rotation == ModelRotation.X0_Y0 ? IDENTITY : new TRSRTransformation(rotation);
    }

    public static TRSRTransformation from(EnumFacing facing) {
        return facing == EnumFacing.NORTH ? IDENTITY : new TRSRTransformation(getMatrix(facing));
    }

    private static final EnumMap<EnumFacing, Matrix4f> FACE_MATRICES = new EnumMap<>(EnumFacing.class);

    public static Matrix4f getMatrix(EnumFacing facing) {
        synchronized (FACE_MATRICES) {
            return (Matrix4f) FACE_MATRICES.computeIfAbsent(facing, f -> switch (f) {
                case DOWN -> ModelRotation.X90_Y0.getMatrix();
                case UP -> ModelRotation.X270_Y0.getMatrix();
                case NORTH -> identityMatrix();
                case SOUTH -> ModelRotation.X0_Y180.getMatrix();
                case WEST -> ModelRotation.X0_Y270.getMatrix();
                case EAST -> ModelRotation.X0_Y90.getMatrix();
            }).clone();
        }
    }

    private static final TRSRTransformation FLIP_X;

    static {
        Matrix4f m = identityMatrix();
        m.m00 = -1;
        FLIP_X = new TRSRTransformation(m);
    }

    public static TRSRTransformation getFlipX() {
        return FLIP_X;
    }

    /** From a transform about the block's center to one about its corner (the model's origin). */
    public static TRSRTransformation blockCenterToCorner(TRSRTransformation transform) {
        if (transform.isIdentity()) return transform;
        Matrix4f ret = new Matrix4f(transform.getMatrix()), tmp = new Matrix4f();
        tmp.setIdentity();
        tmp.m03 = tmp.m13 = tmp.m23 = .5f;
        ret.mul(tmp, ret);
        tmp.m03 = tmp.m13 = tmp.m23 = -.5f;
        ret.mul(tmp);
        return new TRSRTransformation(ret);
    }

    public static TRSRTransformation blockCornerToCenter(TRSRTransformation transform) {
        if (transform.isIdentity()) return transform;
        Matrix4f ret = new Matrix4f(transform.getMatrix()), tmp = new Matrix4f();
        tmp.setIdentity();
        tmp.m03 = tmp.m13 = tmp.m23 = -.5f;
        ret.mul(tmp, ret);
        tmp.m03 = tmp.m13 = tmp.m23 = .5f;
        ret.mul(tmp);
        return new TRSRTransformation(ret);
    }

    public static Matrix4f toVecmath(legacyconnector.v1_12_2.org.lwjgl.util.vector.Matrix4f m) {
        return new Matrix4f(m.m00, m.m10, m.m20, m.m30, m.m01, m.m11, m.m21, m.m31, m.m02, m.m12, m.m22, m.m32, m.m03, m.m13, m.m23, m.m33);
    }

    public static legacyconnector.v1_12_2.org.lwjgl.util.vector.Matrix4f toLwjgl(Matrix4f m) {
        legacyconnector.v1_12_2.org.lwjgl.util.vector.Matrix4f r = new legacyconnector.v1_12_2.org.lwjgl.util.vector.Matrix4f();
        r.m00 = m.m00;
        r.m01 = m.m10;
        r.m02 = m.m20;
        r.m03 = m.m30;
        r.m10 = m.m01;
        r.m11 = m.m11;
        r.m12 = m.m21;
        r.m13 = m.m31;
        r.m20 = m.m02;
        r.m21 = m.m12;
        r.m22 = m.m22;
        r.m23 = m.m32;
        r.m30 = m.m03;
        r.m31 = m.m13;
        r.m32 = m.m23;
        r.m33 = m.m33;
        return r;
    }

    /** The modern pose matrix of this transform. */
    public org.joml.Matrix4f mcc$joml() {
        return new org.joml.Matrix4f(matrix.m00, matrix.m10, matrix.m20, matrix.m30, matrix.m01, matrix.m11, matrix.m21, matrix.m31, matrix.m02,
                matrix.m12, matrix.m22, matrix.m32, matrix.m03, matrix.m13, matrix.m23, matrix.m33);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TRSRTransformation t && matrix.equals(t.matrix);
    }

    @Override
    public int hashCode() {
        return matrix.hashCode();
    }

    @Override
    public String toString() {
        return matrix.toString();
    }
}
