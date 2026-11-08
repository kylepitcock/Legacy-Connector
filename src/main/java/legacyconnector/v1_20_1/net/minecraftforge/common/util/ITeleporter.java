package legacyconnector.v1_20_1.net.minecraftforge.common.util;

import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Forge 1.20.1's teleporter hook of {@code Entity.changeDimension(level, teleporter)}; NeoForge 1.21.1 moved to {@code DimensionTransition}. Kept so classes that
 * implement it load; {@code changeDimension(level, teleporter)} calls go to {@code ApiCompat#changeDimension}, which asks {@link #placeEntity} for the entity to
 * continue with when the mod overrides it, and otherwise transfers the entity to the same position in the other level.
 */
public interface ITeleporter {
    /** The entity after the move; the default asks {@code repositionEntity} (the vanilla placement, a no-op in the translated flow). */
    default Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
        return repositionEntity.apply(true);
    }

    /** The arrival position, speed and rotation; the default is what the vanilla placement would answer. */
    default org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyPortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
            Function<ServerLevel, org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyPortalInfo> defaultPortalInfo) {
        return defaultPortalInfo.apply(destWorld);
    }

    default boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
        return true;
    }

    default boolean isVanilla() {
        return getClass().equals(net.minecraft.world.level.portal.PortalForcer.class);
    }

    default boolean isCompatibleWith(Entity entity) {
        return true;
    }
}
