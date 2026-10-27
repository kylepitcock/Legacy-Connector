package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Forge 1.12.2 teleporter: places an entity that {@code changeDimension(dimension, teleporter)} moved into {@code world}. */
public interface ITeleporter {
    void placeEntity(World world, Entity entity, float yaw);

    default boolean isVanilla() {
        return getClass() == legacyconnector.v1_12_2.net.minecraft.world.Teleporter.class;
    }
}
