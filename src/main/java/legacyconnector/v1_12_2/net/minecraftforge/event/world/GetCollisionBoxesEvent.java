package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Forge's hook on {@code World.getCollisionBoxes}: handlers add or remove boxes. */
public class GetCollisionBoxesEvent extends WorldEvent {
    private final Entity entity;
    private final AxisAlignedBB aabb;
    private final List<AxisAlignedBB> collisionBoxesList;

    public GetCollisionBoxesEvent(World world, Entity entity, AxisAlignedBB aabb, List<AxisAlignedBB> collisionBoxesList) {
        super(world);
        this.entity = entity;
        this.aabb = aabb;
        this.collisionBoxesList = collisionBoxesList;
    }

    public Entity getEntity() {
        return entity;
    }

    public AxisAlignedBB getAabb() {
        return aabb;
    }

    public List<AxisAlignedBB> getCollisionBoxesList() {
        return collisionBoxesList;
    }
}
