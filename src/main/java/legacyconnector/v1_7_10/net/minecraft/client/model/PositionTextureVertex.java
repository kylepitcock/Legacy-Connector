package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.util.Vec3;

/** A box vertex: position and texture coordinates. */
public class PositionTextureVertex {
    public Vec3 vector3D;
    public float texturePositionX;
    public float texturePositionY;

    public PositionTextureVertex(float x, float y, float z, float u, float v) {
        this(Vec3.createVectorHelper(x, y, z), u, v);
    }

    public PositionTextureVertex(PositionTextureVertex other, float u, float v) {
        this.vector3D = other.vector3D;
        this.texturePositionX = u;
        this.texturePositionY = v;
    }

    public PositionTextureVertex(Vec3 position, float u, float v) {
        this.vector3D = position;
        this.texturePositionX = u;
        this.texturePositionY = v;
    }

    public PositionTextureVertex setTexturePosition(float u, float v) {
        return new PositionTextureVertex(this, u, v);
    }
}
