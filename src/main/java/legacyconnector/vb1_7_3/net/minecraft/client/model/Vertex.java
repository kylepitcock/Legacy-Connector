package legacyconnector.vb1_7_3.net.minecraft.client.model;

import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;

/** Beta 1.7.3 {@code PositionTextureVertex}: a model corner and its texture coordinates; mods' own model classes (Airships) build quads from them. */
public class Vertex {
    public Vec3d pos;
    public float u;
    public float v;

    public Vertex(float x, float y, float z, float u, float v) {
        this(Vec3d.create(x, y, z), u, v);
    }

    public Vertex(Vertex other, float u, float v) {
        this.pos = other.pos;
        this.u = u;
        this.v = v;
    }

    public Vertex(Vec3d pos, float u, float v) {
        this.pos = pos;
        this.u = u;
        this.v = v;
    }

    /** Beta {@code setTexturePosition}: the same corner with other texture coordinates. */
    public Vertex remap(float u, float v) {
        return new Vertex(this, u, v);
    }
}
