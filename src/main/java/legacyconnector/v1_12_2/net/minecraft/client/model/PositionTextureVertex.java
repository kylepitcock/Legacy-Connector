package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** A box vertex: position and texture coordinates. */
public class PositionTextureVertex {
    public Vec3d vector3D;
    public float texturePositionX;
    public float texturePositionY;

    public PositionTextureVertex(float x, float y, float z, float texU, float texV) {
        this(new Vec3d(x, y, z), texU, texV);
    }

    public PositionTextureVertex setTexturePosition(float texU, float texV) {
        return new PositionTextureVertex(this, texU, texV);
    }

    public PositionTextureVertex(PositionTextureVertex textureVertex, float texU, float texV) {
        vector3D = textureVertex.vector3D;
        texturePositionX = texU;
        texturePositionY = texV;
    }

    public PositionTextureVertex(Vec3d vector3DIn, float texU, float texV) {
        vector3D = vector3DIn;
        texturePositionX = texU;
        texturePositionY = texV;
    }
}
