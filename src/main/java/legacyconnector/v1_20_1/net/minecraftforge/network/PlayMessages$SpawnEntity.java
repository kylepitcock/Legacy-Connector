package legacyconnector.v1_20_1.net.minecraftforge.network;

import net.minecraft.world.entity.Entity;

/**
 * Forge 1.20.1's spawn message handed to {@code EntityType.Builder.setCustomClientFactory}. NeoForge 1.21.1 spawns entities with the vanilla packet and the
 * entity type's own factory, so the builder call is ignored ({@code ForgeApi1201}) and this class only has to exist for the mod's lambda to link.
 */
public class PlayMessages$SpawnEntity {
    private final Entity entity;

    public PlayMessages$SpawnEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public int getTypeId() {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getId(entity.getType());
    }

    public int getEntityId() {
        return entity.getId();
    }

    public java.util.UUID getUuid() {
        return entity.getUUID();
    }

    public double getPosX() {
        return entity.getX();
    }

    public double getPosY() {
        return entity.getY();
    }

    public double getPosZ() {
        return entity.getZ();
    }

    public float getPitch() {
        return entity.getXRot();
    }

    public float getYaw() {
        return entity.getYRot();
    }

    public float getHeadYaw() {
        return entity.getYHeadRot();
    }

    public int getVelX() {
        return (int) (net.minecraft.util.Mth.clamp(entity.getDeltaMovement().x, -3.9, 3.9) * 8000.0);
    }

    public int getVelY() {
        return (int) (net.minecraft.util.Mth.clamp(entity.getDeltaMovement().y, -3.9, 3.9) * 8000.0);
    }

    public int getVelZ() {
        return (int) (net.minecraft.util.Mth.clamp(entity.getDeltaMovement().z, -3.9, 3.9) * 8000.0);
    }

    public net.minecraft.network.FriendlyByteBuf getAdditionalData() {
        return null;
    }
}
