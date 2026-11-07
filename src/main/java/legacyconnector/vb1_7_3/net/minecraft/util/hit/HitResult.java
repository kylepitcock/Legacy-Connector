package legacyconnector.vb1_7_3.net.minecraft.util.hit;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;

public class HitResult {
    public HitResultType type;
    public int blockX, blockY, blockZ, side;
    public Vec3d pos;
    public Entity entity;
    public HitResult(int x, int y, int z, int side, Vec3d pos) {
        type = HitResultType.BLOCK;
        blockX = x; blockY = y; blockZ = z; this.side = side; this.pos = pos;
    }
    public HitResult(Entity entity) {
        type = HitResultType.ENTITY; this.entity = entity;
        pos = Vec3d.create(entity.x, entity.y, entity.z);
    }
}
