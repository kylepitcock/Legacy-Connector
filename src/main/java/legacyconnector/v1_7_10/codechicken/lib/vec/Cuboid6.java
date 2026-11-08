package legacyconnector.v1_7_10.codechicken.lib.vec;

import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;

/** CodeChickenLib's axis-aligned box (min and max corners). */
public class Cuboid6 {
    public static final Cuboid6 full = new Cuboid6(0, 0, 0, 1, 1, 1);

    public Vector3 min;
    public Vector3 max;

    public Cuboid6(Vector3 min, Vector3 max) {
        this.min = min;
        this.max = max;
    }

    public Cuboid6(AxisAlignedBB aabb) {
        this(new Vector3(aabb.minX, aabb.minY, aabb.minZ), new Vector3(aabb.maxX, aabb.maxY, aabb.maxZ));
    }

    public Cuboid6(Cuboid6 c) {
        this(c.min.copy(), c.max.copy());
    }

    public Cuboid6(double minx, double miny, double minz, double maxx, double maxy, double maxz) {
        this(new Vector3(minx, miny, minz), new Vector3(maxx, maxy, maxz));
    }

    public AxisAlignedBB toAABB() {
        return AxisAlignedBB.getBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public Cuboid6 copy() {
        return new Cuboid6(this);
    }

    public Cuboid6 set(double minx, double miny, double minz, double maxx, double maxy, double maxz) {
        min.set(minx, miny, minz);
        max.set(maxx, maxy, maxz);
        return this;
    }

    public Cuboid6 set(Cuboid6 c) {
        min.set(c.min);
        max.set(c.max);
        return this;
    }

    public Cuboid6 add(Vector3 v) {
        min.add(v);
        max.add(v);
        return this;
    }

    public Cuboid6 expand(double d) {
        min.add(-d);
        max.add(d);
        return this;
    }

    public Cuboid6 enclose(Cuboid6 c) {
        min.set(Math.min(min.x, c.min.x), Math.min(min.y, c.min.y), Math.min(min.z, c.min.z));
        max.set(Math.max(max.x, c.max.x), Math.max(max.y, c.max.y), Math.max(max.z, c.max.z));
        return this;
    }

    public boolean intersects(Cuboid6 b) {
        return max.x - 1E-5 > b.min.x && max.y - 1E-5 > b.min.y && max.z - 1E-5 > b.min.z
                && b.max.x - 1E-5 > min.x && b.max.y - 1E-5 > min.y && b.max.z - 1E-5 > min.z;
    }

    public boolean contains(Vector3 v) {
        return v.x >= min.x && v.x <= max.x && v.y >= min.y && v.y <= max.y && v.z >= min.z && v.z <= max.z;
    }

    public Vector3 center() {
        return min.copy().add(max).multiply(0.5);
    }

    public String toString() {
        return "Cuboid: (" + min.x + ", " + min.y + ", " + min.z + ") -> (" + max.x + ", " + max.y + ", " + max.z + ")";
    }
}
