package mccompat.v1_7_10.net.minecraftforge.client.model.obj;

public class Vertex {
    public float x, y, z;

    public Vertex(float x, float y) {
        this(x, y, 0.0F);
    }

    public Vertex(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
