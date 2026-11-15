package legacyconnector.vb1_7_3.net.minecraft.util.hit;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;

public class HitResult {
    public HitResultType type;
    public int blockX, blockY, blockZ, side;
    public Vec3d pos;
    public Entity entity;
    /** Forge 1.0: the part of a block that was hit (a multipart block's raytrace sets it), -1 for the whole block. */
    public int subHit = -1;
    public HitResult(int x, int y, int z, int side, Vec3d pos) {
        type = HitResultType.BLOCK;
        blockX = x; blockY = y; blockZ = z; this.side = side; this.pos = pos;
    }
    public HitResult(Entity entity) {
        type = HitResultType.ENTITY; this.entity = entity;
        pos = Vec3d.create(entity.x, entity.y, entity.z);
    }
}
