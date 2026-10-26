package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import java.util.Comparator;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/** Orders section renderers by squared distance from an entity, nearest first (1.7.10's chunk update order). */
public class EntitySorter implements Comparator {
    private final double entityPosX;
    private final double entityPosY;
    private final double entityPosZ;

    public EntitySorter(Entity entity) {
        this.entityPosX = entity.mcc$entity == null ? 0 : entity.mcc$entity.getX();
        this.entityPosY = entity.mcc$entity == null ? 0 : entity.mcc$entity.getY();
        this.entityPosZ = entity.mcc$entity == null ? 0 : entity.mcc$entity.getZ();
    }

    public int compare(WorldRenderer a, WorldRenderer b) {
        double ax = a.posXPlus - entityPosX, ay = a.posYPlus - entityPosY, az = a.posZPlus - entityPosZ;
        double bx = b.posXPlus - entityPosX, by = b.posYPlus - entityPosY, bz = b.posZPlus - entityPosZ;
        return (int) ((ax * ax + ay * ay + az * az - (bx * bx + by * by + bz * bz)) * 1024.0D);
    }

    @Override
    public int compare(Object a, Object b) {
        return compare((WorldRenderer) a, (WorldRenderer) b);
    }
}
