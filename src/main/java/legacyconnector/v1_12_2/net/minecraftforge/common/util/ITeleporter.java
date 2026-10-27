package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Places an entity that arrived in another dimension (Forge's interface of vanilla's Teleporter and mods' own). */
public interface ITeleporter {
    void placeEntity(World world, Entity entity, float yaw);

    default boolean isVanilla() {
        return getClass().getName().equals("legacyconnector.v1_12_2.net.minecraft.world.Teleporter");
    }
}
