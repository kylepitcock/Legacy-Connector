package legacyconnector.v1_7_10.codechicken.lib.raytracer;

import legacyconnector.v1_7_10.codechicken.lib.vec.Cuboid6;

/** A box that carries a tag (usually its index in the part), so a ray trace says which box it hit. */
public class IndexedCuboid6 extends Cuboid6 {
    public Object data;

    public IndexedCuboid6(Object data, Cuboid6 cuboid) {
        super(cuboid);
        this.data = data;
    }
}
