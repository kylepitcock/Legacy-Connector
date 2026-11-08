package legacyconnector.v1_7_10.codechicken.lib.raytracer;

import java.util.List;
import legacyconnector.v1_7_10.codechicken.lib.vec.BlockCoord;
import legacyconnector.v1_7_10.codechicken.lib.vec.Cuboid6;
import legacyconnector.v1_7_10.codechicken.lib.vec.Vector3;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** CodeChickenLib's ray tracer: against a list of boxes (multipart blocks) and against the world from a player's eyes. */
public class RayTracer {
    private static final ThreadLocal<RayTracer> INSTANCE = ThreadLocal.withInitial(RayTracer::new);

    private Vector3 vec = new Vector3();
    private Vector3 vec2 = new Vector3();

    private double s_dist;
    private int s_side;
    private Vector3 s_vec = new Vector3();

    public static RayTracer instance() {
        return INSTANCE.get();
    }

    private void traceSide(int side, Vector3 start, Vector3 end, double a0, double a1, double b0, double b1, double c, int axis) {
        // axis: 0 = x plane, 1 = y plane, 2 = z plane; (a, b) are the other two coordinates
        double sc = axis == 0 ? start.x : axis == 1 ? start.y : start.z;
        double ec = axis == 0 ? end.x : axis == 1 ? end.y : end.z;
        double d = ec - sc;
        if (d == 0) return;
        double t = (c - sc) / d;
        if (t < 0 || t > 1) return;
        double x = start.x + (end.x - start.x) * t;
        double y = start.y + (end.y - start.y) * t;
        double z = start.z + (end.z - start.z) * t;
        double a = axis == 0 ? y : axis == 1 ? x : x;
        double b = axis == 0 ? z : axis == 1 ? z : y;
        if (a < a0 - 1E-9 || a > a1 + 1E-9 || b < b0 - 1E-9 || b > b1 + 1E-9) return;
        double dist = t * start.copy().sub(end).mag();
        if (dist < s_dist) {
            s_dist = dist;
            s_side = side;
            s_vec.set(x, y, z);
        }
    }

    /** Traces the segment against the box; null when it misses. The hit carries the box's side and the distance from start. */
    public ExtendedMOP rayTraceCuboid(Vector3 start, Vector3 end, Cuboid6 cuboid) {
        s_dist = Double.MAX_VALUE;
        s_side = -1;
        Vector3 min = cuboid.min;
        Vector3 max = cuboid.max;
        traceSide(0, start, end, min.x, max.x, min.z, max.z, min.y, 1);
        traceSide(1, start, end, min.x, max.x, min.z, max.z, max.y, 1);
        traceSide(2, start, end, min.x, max.x, min.y, max.y, min.z, 2);
        traceSide(3, start, end, min.x, max.x, min.y, max.y, max.z, 2);
        traceSide(4, start, end, min.y, max.y, min.z, max.z, min.x, 0);
        traceSide(5, start, end, min.y, max.y, min.z, max.z, max.x, 0);
        if (s_side < 0) return null;
        Vec3 hit = Vec3.createVectorHelper(s_vec.x, s_vec.y, s_vec.z);
        ExtendedMOP mop = new ExtendedMOP(0, 0, 0, s_side, hit, null);
        mop.dist = s_dist;
        return mop;
    }

    /** As {@link #rayTraceCuboid(Vector3, Vector3, Cuboid6)} with the block position and data filled in. */
    public ExtendedMOP rayTraceCuboid(Vector3 start, Vector3 end, Cuboid6 cuboid, BlockCoord pos, Object data) {
        ExtendedMOP mop = rayTraceCuboid(start, end, cuboid);
        if (mop != null) {
            mop.blockX = pos.x;
            mop.blockY = pos.y;
            mop.blockZ = pos.z;
            mop.setData(data);
        }
        return mop;
    }

    /** Traces all the boxes (positions relative to the block) and returns the nearest hit, whose data is the box's data. */
    public ExtendedMOP rayTraceCuboids(Vector3 start, Vector3 end, List<IndexedCuboid6> cuboids, BlockCoord pos, Block block) {
        ExtendedMOP closest = null;
        for (IndexedCuboid6 cuboid : cuboids) {
            Cuboid6 shifted = cuboid.copy().add(Vector3.fromBlockCoord(pos));
            ExtendedMOP mop = rayTraceCuboid(start, end, shifted, pos, cuboid.data);
            if (mop != null && (closest == null || mop.dist < closest.dist)) closest = mop;
        }
        return closest;
    }

    /** The point a player's eyes are at. */
    public static Vector3 getCorrectedHeadVec(EntityPlayer player) {
        Vector3 v = Vector3.fromEntity(player);
        v.y += player.getEyeHeight();
        return v;
    }

    public static Vector3 getStartVec(EntityPlayer player) {
        return getCorrectedHeadVec(player);
    }

    public static double getBlockReachDistance(EntityPlayer player) {
        return PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities) ? 5.0 : 4.5;
    }

    public static Vector3 getEndVec(EntityPlayer player) {
        Vector3 head = getCorrectedHeadVec(player);
        Vec3 look = player.getLookVec();
        double reach = getBlockReachDistance(player);
        return head.add(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
    }

    /** What the player is looking at, in the world (liquids included), as the block the cursor points to. */
    public static MovingObjectPosition reTrace(World world, EntityPlayer player) {
        return reTrace(world, player, getBlockReachDistance(player));
    }

    public static MovingObjectPosition reTrace(World world, EntityPlayer player, double reach) {
        Vector3 head = getCorrectedHeadVec(player);
        Vec3 look = player.getLookVec();
        Vec3 start = head.toVec3D();
        Vec3 end = Vec3.createVectorHelper(head.x + look.xCoord * reach, head.y + look.yCoord * reach, head.z + look.zCoord * reach);
        return world.rayTraceBlocks(start, end, true, false, true);
    }
}
