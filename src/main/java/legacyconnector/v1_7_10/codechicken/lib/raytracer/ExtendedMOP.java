package legacyconnector.v1_7_10.codechicken.lib.raytracer;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;

/** A ray trace result that also knows the data of the box it hit and its distance from the start. */
public class ExtendedMOP extends MovingObjectPosition implements Comparable<ExtendedMOP> {
    public Object data;
    public double dist;

    public ExtendedMOP(Entity entity, Object data) {
        super(entity);
        setData(data);
    }

    public ExtendedMOP(int x, int y, int z, int side, Vec3 hit, Object data) {
        super(x, y, z, side, hit);
        setData(data);
    }

    public ExtendedMOP(MovingObjectPosition mop, Object data, double dist) {
        super(mop.blockX, mop.blockY, mop.blockZ, mop.sideHit, mop.hitVec);
        this.typeOfHit = mop.typeOfHit;
        this.entityHit = mop.entityHit;
        this.dist = dist;
        setData(data);
    }

    public void setData(Object data) {
        this.data = data;
        if (data instanceof Integer i) subHit = i;
        hitInfo = data;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    public int compareTo(ExtendedMOP o) {
        return Double.compare(dist, o.dist);
    }

    /** The hit result as a plain vanilla one (some callers compare against the original type). */
    public static ExtendedMOP of(MovingObjectPosition mop) {
        return mop instanceof ExtendedMOP e ? e : null;
    }
}
