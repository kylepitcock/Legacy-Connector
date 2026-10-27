package legacyconnector.v1_12_2.net.minecraftforge.common.model;

import javax.vecmath.Matrix4f;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** A transform of positions, faces and vertex order (block rotations). */
public interface ITransformation {
    Matrix4f getMatrix();

    EnumFacing rotate(EnumFacing facing);

    int rotate(EnumFacing facing, int vertexIndex);
}
