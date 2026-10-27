package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelPart;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.ITransformation;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Matrix4f;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f;

/** A blockstate variant's rotation in quarter turns about x, then y. */
public enum ModelRotation implements IModelState, ITransformation {
    X0_Y0(0, 0),
    X0_Y90(0, 90),
    X0_Y180(0, 180),
    X0_Y270(0, 270),
    X90_Y0(90, 0),
    X90_Y90(90, 90),
    X90_Y180(90, 180),
    X90_Y270(90, 270),
    X180_Y0(180, 0),
    X180_Y90(180, 90),
    X180_Y180(180, 180),
    X180_Y270(180, 270),
    X270_Y0(270, 0),
    X270_Y90(270, 90),
    X270_Y180(270, 180),
    X270_Y270(270, 270);

    private static final java.util.Map<Integer, ModelRotation> MAP_ROTATIONS = new java.util.HashMap<>();
    private final int combinedXY;
    private final Matrix4f matrix;
    private final int quartersX;
    private final int quartersY;

    static {
        for (ModelRotation r : values()) MAP_ROTATIONS.put(r.combinedXY, r);
    }

    private static int combineXY(int x, int y) {
        return x * 360 + y;
    }

    ModelRotation(int x, int y) {
        combinedXY = combineXY(x, y);
        matrix = new Matrix4f();
        Matrix4f mx = new Matrix4f();
        mx.setIdentity();
        Matrix4f.rotate(-x * 0.017453292F, new Vector3f(1.0F, 0.0F, 0.0F), mx, mx);
        quartersX = Math.abs(x / 90);
        Matrix4f my = new Matrix4f();
        my.setIdentity();
        Matrix4f.rotate(-y * 0.017453292F, new Vector3f(0.0F, 1.0F, 0.0F), my, my);
        quartersY = Math.abs(y / 90);
        Matrix4f.mul(my, mx, matrix);
    }

    public Matrix4f matrix() {
        return new Matrix4f(matrix);
    }

    public EnumFacing rotateFace(EnumFacing facing) {
        EnumFacing f = facing;
        for (int i = 0; i < quartersX; ++i) f = f.rotateAround(EnumFacing.Axis.X);
        if (f.getAxis() != EnumFacing.Axis.Y) {
            for (int j = 0; j < quartersY; ++j) f = f.rotateAround(EnumFacing.Axis.Y);
        }
        return f;
    }

    public int rotateVertex(EnumFacing facing, int vertexIndex) {
        int i = vertexIndex;
        if (facing.getAxis() == EnumFacing.Axis.X) i = (vertexIndex + quartersX) % 4;
        EnumFacing f = facing;
        for (int j = 0; j < quartersX; ++j) f = f.rotateAround(EnumFacing.Axis.X);
        if (f.getAxis() == EnumFacing.Axis.Y) i = (i + quartersY) % 4;
        return i;
    }

    public static ModelRotation getModelRotation(int x, int y) {
        return MAP_ROTATIONS.get(combineXY(Math.floorMod(x, 360), Math.floorMod(y, 360)));
    }

    @Override
    public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
        if (part.isPresent()) return Optional.empty();
        return Optional.of(new TRSRTransformation(getMatrix()));
    }

    @Override
    public javax.vecmath.Matrix4f getMatrix() {
        return TRSRTransformation.toVecmath(matrix);
    }

    @Override
    public EnumFacing rotate(EnumFacing facing) {
        return rotateFace(facing);
    }

    @Override
    public int rotate(EnumFacing facing, int vertexIndex) {
        return rotateVertex(facing, vertexIndex);
    }

    /** The modern block model rotation with the same quarter turns. */
    public net.minecraft.client.resources.model.BlockModelRotation mcc$modern() {
        return net.minecraft.client.resources.model.BlockModelRotation.by(quartersX * 90, quartersY * 90);
    }
}
