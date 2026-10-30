package legacyconnector.v1_12_2.net.minecraftforge.common.ticket;

import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** A ticket for the positions inside a box. */
public class AABBTicket extends SimpleTicket<Vec3d> {
    public final AxisAlignedBB axisAlignedBB;

    public AABBTicket(AxisAlignedBB axisAlignedBB) {
        this.axisAlignedBB = axisAlignedBB;
    }

    @Override
    public boolean matches(Vec3d toMatch) {
        AxisAlignedBB b = axisAlignedBB;
        return toMatch.x > b.minX && toMatch.x < b.maxX && toMatch.y > b.minY && toMatch.y < b.maxY && toMatch.z > b.minZ && toMatch.z < b.maxZ;
    }
}
